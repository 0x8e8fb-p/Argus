package com.nexusblock.engine

import android.util.Log
import java.nio.ByteBuffer

/**
 * QUIC Initial Packet inspector for extracting SNI from QUIC long headers.
 *
 * QUIC version 1 (RFC 9000) sends the first flight in "long header" packets.
 * The Client Initial contains the crypto frame with a TLS 1.3 ClientHello,
 * which includes the SNI in cleartext (before encryption keys are derived).
 *
 * This class parses just enough of the QUIC long header and CRYPTO frame
 * to extract the SNI hostname, enabling DNS-blocklist-based blocking of
 * QUIC connections that would otherwise bypass our TCP-layer SNI inspector.
 *
 * Reference: draft-ietf-quic-transport-34, RFC 9000
 */
object QuicInspector {

    private const val TAG = "Argus/Quic"

    // QUIC long header packet types
    private const val PACKET_TYPE_INITIAL: Byte = 0x00
    private const val PACKET_TYPE_0RTT: Byte = 0x01
    private const val PACKET_TYPE_HANDSHAKE: Byte = 0x02
    private const val PACKET_TYPE_RETRY: Byte = 0x03

    // Long header form mask: 1ST byte MSB = 1
    private const val LONG_HEADER_MASK: Byte = 0x80.toByte()

    // Frame types
    private const val FRAME_TYPE_CRYPTO: Long = 0x06

    /**
     * Inspect a UDP payload believed to be QUIC and return the SNI hostname
     * if this is an Initial packet containing a TLS ClientHello.
     *
     * @param payload UDP payload bytes
     * @return SNI hostname, or null if not found or not QUIC Initial
     */
    fun extractSni(payload: ByteArray): String? {
        if (payload.isEmpty()) return null

        try {
            val buf = ByteBuffer.wrap(payload)
            val firstByte = buf.get()

            // Check long header form (MSB must be 1)
            if (firstByte.toInt() and LONG_HEADER_MASK.toInt() == 0) {
                // Short header — no SNI in cleartext
                return null
            }

            val packetType = ((firstByte.toInt() ushr 4) and 0x03).toByte()
            if (packetType != PACKET_TYPE_INITIAL && packetType != PACKET_TYPE_0RTT) {
                // Only Initial and 0-RTT carry ClientHello
                return null
            }

            val version = buf.int // 4 bytes version
            if (version == 0x00000000) {
                // Version Negotiation — no crypto
                return null
            }

            val dcidLen = buf.get().toInt() and 0xFF
            if (dcidLen > 255) return null
            if (buf.remaining() < dcidLen) return null
            buf.position(buf.position() + dcidLen)

            val scidLen = buf.get().toInt() and 0xFF
            if (scidLen > 255) return null
            if (buf.remaining() < scidLen) return null
            buf.position(buf.position() + scidLen)

            // Token length (varint) — only present in Initial
            val tokenLen = readVarInt(buf)
            if (tokenLen < 0) return null
            if (buf.remaining() < tokenLen) return null
            buf.position(buf.position() + tokenLen.toInt())

            // Length (varint) — remaining protected payload + auth tag
            val length = readVarInt(buf)
            if (length < 0) return null
            if (buf.remaining() < length) return null

            // From here, payload is "protected" with the initial keys,
            // but for QUIC v1 the Initial keys are derived from the DCID
            // and the crypto frame containing ClientHello is in cleartext
            // enough for us to extract the TLS record.
            // We use a heuristic: scan for TLS Handshake record (0x16)
            // followed by TLS 1.3 version (0x0303) and ClientHello (0x01).

            val payloadStart = buf.position()
            val payloadEnd = payloadStart + length.toInt()

            // Heuristic TLS ClientHello search within the payload
            return findTlsClientHelloSni(buf, payloadStart, payloadEnd)

        } catch (e: Exception) {
            Log.v(TAG, "QUIC parse error: ${e.message}")
            return null
        }
    }

    /**
     * Read a QUIC variable-length integer (max 62 bits, encoded in 1-8 bytes).
     */
    private fun readVarInt(buf: ByteBuffer): Long {
        if (!buf.hasRemaining()) return -1
        val first = buf.get(buf.position()).toInt() and 0xFF
        val len = 1 shl (first ushr 6)
        if (buf.remaining() < len) return -1
        var value = 0L
        val b0 = buf.get().toLong() and 0xFF
        value = b0 and 0x3F
        for (i in 1 until len) {
            value = (value shl 8) or (buf.get().toLong() and 0xFF)
        }
        return value
    }

    /**
     * Heuristic scan for TLS ClientHello record within the QUIC CRYPTO payload.
     * Returns the SNI hostname if a ClientHello is found.
     */
    private fun findTlsClientHelloSni(buf: ByteBuffer, start: Int, end: Int): String? {
        // Minimum TLS record header (5) + Handshake header (4) + ClientHello min data
        val minRecordSize = 5 + 4 + 39

        for (offset in start..(end - minRecordSize)) {
            // TLS Handshake record type = 0x16
            if (buf.get(offset) != 0x16.toByte()) continue
            // TLS version = 0x0303 (TLS 1.2 record version for TLS 1.3)
            if (buf.get(offset + 1) != 0x03.toByte() || buf.get(offset + 2) != 0x03.toByte()) continue

            val recordLen = ((buf.get(offset + 3).toInt() and 0xFF) shl 8) or
                    (buf.get(offset + 4).toInt() and 0xFF)
            if (recordLen < 4) continue

            val handshakeType = buf.get(offset + 5).toInt() and 0xFF
            if (handshakeType != 0x01) continue // Not ClientHello

            // Parse SNI from this ClientHello
            val sni = try {
                extractSniFromClientHello(buf, offset + 5, recordLen + 5 - 4)
            } catch (e: Exception) {
                null
            }
            if (sni != null) {
                Log.v(TAG, "QUIC SNI found: $sni")
                return sni
            }
        }
        return null
    }

    /**
     * Extract SNI from a TLS ClientHello starting at [offset] within the buffer.
     * Parses the handshake message (without the record header).
     */
    private fun extractSniFromClientHello(buf: ByteBuffer, offset: Int, maxLen: Int): String? {
        var pos = offset

        // Handshake type (1 byte) + length (3 bytes)
        pos += 4

        // Version (2 bytes)
        pos += 2

        // Random (32 bytes)
        pos += 32

        if (pos >= offset + maxLen) return null
        val sessionIdLen = buf.get(pos).toInt() and 0xFF
        pos += 1 + sessionIdLen

        if (pos + 2 > offset + maxLen) return null
        val cipherSuitesLen = ((buf.get(pos).toInt() and 0xFF) shl 8) or (buf.get(pos + 1).toInt() and 0xFF)
        pos += 2 + cipherSuitesLen

        if (pos + 1 > offset + maxLen) return null
        val compressionLen = buf.get(pos).toInt() and 0xFF
        pos += 1 + compressionLen

        if (pos + 2 > offset + maxLen) return null
        val extensionsLen = ((buf.get(pos).toInt() and 0xFF) shl 8) or (buf.get(pos + 1).toInt() and 0xFF)
        pos += 2
        val extensionsEnd = pos + extensionsLen

        while (pos + 4 <= extensionsEnd && pos + 4 <= offset + maxLen) {
            val extType = ((buf.get(pos).toInt() and 0xFF) shl 8) or (buf.get(pos + 1).toInt() and 0xFF)
            val extLen = ((buf.get(pos + 2).toInt() and 0xFF) shl 8) or (buf.get(pos + 3).toInt() and 0xFF)
            pos += 4
            if (extType == 0x0000) { // SNI extension
                if (pos + 2 <= offset + maxLen) {
                    val sniListLen = ((buf.get(pos).toInt() and 0xFF) shl 8) or (buf.get(pos + 1).toInt() and 0xFF)
                    var sniPos = pos + 2
                    val sniListEnd = sniPos + sniListLen
                    while (sniPos + 3 <= sniListEnd && sniPos + 3 <= offset + maxLen) {
                        val nameType = buf.get(sniPos).toInt() and 0xFF
                        val nameLen = ((buf.get(sniPos + 1).toInt() and 0xFF) shl 8) or (buf.get(sniPos + 2).toInt() and 0xFF)
                        sniPos += 3
                        if (nameType == 0 && sniPos + nameLen <= offset + maxLen) {
                            val sniBytes = ByteArray(nameLen)
                            for (i in 0 until nameLen) {
                                sniBytes[i] = buf.get(sniPos + i)
                            }
                            return String(sniBytes, Charsets.UTF_8)
                        }
                        sniPos += nameLen
                    }
                }
                return null
            }
            pos += extLen
        }
        return null
    }
}
