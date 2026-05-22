package com.nexusblock.engine

import java.nio.ByteBuffer

object SniExtractor {

    fun extractSni(tcpPayload: ByteArray, offset: Int, length: Int): String? {
        if (length < 6) return null
        val contentType = tcpPayload[offset].toInt() and 0xFF
        if (contentType != 0x16) return null // Not TLS Handshake

        val majorVersion = tcpPayload[offset + 1].toInt() and 0xFF
        if (majorVersion != 0x03) return null // Not TLS

        val handshakeType = tcpPayload[offset + 5].toInt() and 0xFF
        if (handshakeType != 0x01) return null // Not ClientHello

        return parseClientHello(tcpPayload, offset + 5, length - 5)
    }

    private fun parseClientHello(data: ByteArray, offset: Int, length: Int): String? {
        if (length < 39) return null
        var pos = offset + 1 // skip handshake type

        // Handshake length (3 bytes)
        pos += 3

        // Client version (2 bytes)
        pos += 2

        // Random (32 bytes)
        pos += 32

        if (pos - offset >= length) return null

        // Session ID
        val sessionIdLen = data[pos].toInt() and 0xFF
        pos += 1 + sessionIdLen

        if (pos - offset + 2 >= length) return null

        // Cipher suites
        val cipherSuitesLen = ((data[pos].toInt() and 0xFF) shl 8) or (data[pos + 1].toInt() and 0xFF)
        pos += 2 + cipherSuitesLen

        if (pos - offset + 1 >= length) return null

        // Compression methods
        val compressionLen = data[pos].toInt() and 0xFF
        pos += 1 + compressionLen

        if (pos - offset + 2 >= length) return null

        // Extensions length
        val extensionsLen = ((data[pos].toInt() and 0xFF) shl 8) or (data[pos + 1].toInt() and 0xFF)
        pos += 2

        val extensionsEnd = pos + extensionsLen
        if (extensionsEnd - offset > length) return null

        while (pos + 4 <= extensionsEnd) {
            val extType = ((data[pos].toInt() and 0xFF) shl 8) or (data[pos + 1].toInt() and 0xFF)
            val extLen = ((data[pos + 2].toInt() and 0xFF) shl 8) or (data[pos + 3].toInt() and 0xFF)
            pos += 4

            if (extType == 0x0000) { // SNI extension
                return parseSniExtension(data, pos, extLen)
            }

            pos += extLen
        }
        return null
    }

    private fun parseSniExtension(data: ByteArray, offset: Int, length: Int): String? {
        if (length < 5) return null
        var pos = offset

        // SNI list length (2 bytes)
        pos += 2

        // Server name type (1 byte) — must be 0 (host_name)
        val nameType = data[pos].toInt() and 0xFF
        if (nameType != 0) return null
        pos += 1

        // Host name length (2 bytes)
        val nameLen = ((data[pos].toInt() and 0xFF) shl 8) or (data[pos + 1].toInt() and 0xFF)
        pos += 2

        if (pos + nameLen - offset > length) return null
        if (nameLen == 0 || nameLen > 255) return null

        return String(data, pos, nameLen, Charsets.US_ASCII).lowercase()
    }
}
