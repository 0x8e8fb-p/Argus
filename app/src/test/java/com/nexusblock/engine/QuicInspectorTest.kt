package com.nexusblock.engine

import org.junit.Assert.*
import org.junit.Test

class QuicInspectorTest {

    @Test
    fun `returns null for empty payload`() {
        assertNull(QuicInspector.extractSni(byteArrayOf()))
    }

    @Test
    fun `returns null for non-QUIC data`() {
        val random = ByteArray(100) { it.toByte() }
        assertNull(QuicInspector.extractSni(random))
    }

    @Test
    fun `returns null for short header packet`() {
        // Short header form: first byte MSB = 0
        val packet = byteArrayOf(
            0x40, // Short header
            0x00, 0x00, 0x00, 0x00
        )
        assertNull(QuicInspector.extractSni(packet))
    }

    @Test
    fun `returns null for retry packet without client hello`() {
        // Long header with RETRY packet type, version 1
        val packet = ByteArray(50)
        packet[0] = 0xF3.toByte() // Long header | Retry type
        packet[1] = 0x00
        packet[2] = 0x00
        packet[3] = 0x00
        packet[4] = 0x01 // Version 1
        assertNull(QuicInspector.extractSni(packet))
    }

    @Test
    fun `extracts SNI from synthetic QUIC initial`() {
        // Build a minimal QUIC Initial packet containing a synthetic
        // TLS ClientHello with SNI "youtubei.googleapis.com"
        val sni = "youtubei.googleapis.com"
        val packet = buildSyntheticQuicInitialWithSni(sni)
        val result = QuicInspector.extractSni(packet)
        assertEquals(sni, result)
    }

    @Test
    fun `extracts SNI from packet with different hostname`() {
        val sni = "spclient.wg.spotify.com"
        val packet = buildSyntheticQuicInitialWithSni(sni)
        val result = QuicInspector.extractSni(packet)
        assertEquals(sni, result)
    }

    /**
     * Build a synthetic QUIC Initial packet with an embedded TLS 1.3 ClientHello
     * containing the given SNI hostname. This is NOT a valid QUIC packet, but
     * contains enough structure for the heuristic parser to find the SNI.
     */
    private fun buildSyntheticQuicInitialWithSni(sni: String): ByteArray {
        val sniBytes = sni.toByteArray(Charsets.UTF_8)

        // Build a fake TLS ClientHello record
        val clientHello = buildClientHelloWithSni(sniBytes)

        // Build a fake TLS record wrapper (type=0x16, version=0x0303, length)
        val recordLength = clientHello.size
        val tlsRecord = ByteArray(5 + recordLength)
        tlsRecord[0] = 0x16 // Handshake
        tlsRecord[1] = 0x03 // TLS 1.2 record version
        tlsRecord[2] = 0x03
        tlsRecord[3] = (recordLength shr 8).toByte()
        tlsRecord[4] = (recordLength and 0xFF).toByte()
        System.arraycopy(clientHello, 0, tlsRecord, 5, recordLength)

        // Build QUIC-like long header packet
        // Byte 0: Long header (0x80) | Initial type (0x00)
        val packet = mutableListOf<Byte>()
        packet.add(0xC0.toByte()) // Long header | Initial | version 1 bits

        // Version (4 bytes)
        packet.add(0x00)
        packet.add(0x00)
        packet.add(0x00)
        packet.add(0x01)

        // DCID len + DCID
        packet.add(0x08)
        repeat(8) { packet.add(0x00) }

        // SCID len + SCID
        packet.add(0x00)

        // Token length (varint = 0)
        packet.add(0x00)

        // Length (varint = payload size + auth tag)
        val payloadSize = tlsRecord.size + 16 // + fake auth tag
        writeVarInt(packet, payloadSize.toLong())

        // Payload: embed TLS record
        tlsRecord.forEach { packet.add(it) }

        // Fake auth tag
        repeat(16) { packet.add(0x00) }

        return packet.toByteArray()
    }

    private fun writeVarInt(list: MutableList<Byte>, value: Long) {
        when {
            value < 64 -> {
                list.add(value.toInt().toByte())
            }
            value < 16384 -> {
                list.add((0x40 or (value ushr 8).toInt()).toByte())
                list.add((value and 0xFF).toInt().toByte())
            }
            value < 1073741824 -> {
                list.add((0x80 or (value ushr 16).toInt()).toByte())
                list.add(((value ushr 8) and 0xFF).toInt().toByte())
                list.add((value and 0xFF).toInt().toByte())
            }
            else -> {
                list.add((0xC0 or (value ushr 24).toInt()).toByte())
                list.add(((value ushr 16) and 0xFF).toInt().toByte())
                list.add(((value ushr 8) and 0xFF).toInt().toByte())
                list.add((value and 0xFF).toInt().toByte())
            }
        }
    }

    private fun buildClientHelloWithSni(sniBytes: ByteArray): ByteArray {
        val ch = mutableListOf<Byte>()

        // Handshake type = 1 (ClientHello), length (3 bytes)
        ch.add(0x01)
        ch.add(0x00)
        ch.add(0x00)
        ch.add(0x00) // placeholder length

        val lengthStart = ch.size

        // Version TLS 1.3 (0x0303 in record, but actual version here is 0x0303 for compat)
        ch.add(0x03)
        ch.add(0x03)

        // Random (32 bytes)
        repeat(32) { ch.add(0x00) }

        // Session ID length
        ch.add(0x00)

        // Cipher suites length = 2, cipher = 0x1301 (TLS_AES_128_GCM_SHA256)
        ch.add(0x00)
        ch.add(0x02)
        ch.add(0x13)
        ch.add(0x01)

        // Compression length = 1, method = null
        ch.add(0x01)
        ch.add(0x00)

        // Extensions length (2 bytes) - placeholder
        val extLenPos = ch.size
        ch.add(0x00)
        ch.add(0x00)

        val extensionsStart = ch.size

        // SNI extension
        val sniListLen = 2 + 1 + 2 + sniBytes.size // sniListLen(2) + nameType(1) + nameLen(2) + name
        val sniExtLen = 2 + sniListLen // sniListLen(2) + list

        ch.add(0x00) // Extension type = SNI (0x0000)
        ch.add(0x00)
        ch.add((sniExtLen shr 8).toByte())
        ch.add((sniExtLen and 0xFF).toByte())
        ch.add((sniListLen shr 8).toByte())
        ch.add((sniListLen and 0xFF).toByte())
        ch.add(0x00) // nameType = hostname
        ch.add((sniBytes.size shr 8).toByte())
        ch.add((sniBytes.size and 0xFF).toByte())
        sniBytes.forEach { ch.add(it) }

        val extensionsEnd = ch.size
        val extensionsLen = extensionsEnd - extensionsStart
        ch[extLenPos] = (extensionsLen shr 8).toByte()
        ch[extLenPos + 1] = (extensionsLen and 0xFF).toByte()

        val chLength = ch.size - lengthStart
        val totalLen = chLength - 3 // length field itself is 3 bytes
        ch[1] = (totalLen shr 16).toByte()
        ch[2] = (totalLen shr 8).toByte()
        ch[3] = (totalLen and 0xFF).toByte()

        return ch.toByteArray()
    }
}
