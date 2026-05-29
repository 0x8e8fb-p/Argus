package com.nexusblock.engine

object SniExtractor {

    fun extractSni(tcpPayload: ByteArray, offset: Int, length: Int): String? {
        if (offset < 0 || length < 6 || offset + length > tcpPayload.size) return null
        val contentType = tcpPayload[offset].toInt() and 0xFF
        if (contentType != 0x16) return null // Not TLS Handshake

        val majorVersion = tcpPayload[offset + 1].toInt() and 0xFF
        if (majorVersion != 0x03) return null // Not TLS

        val recordLen = u16(tcpPayload, offset + 3)
        if (recordLen <= 0 || recordLen + 5 > length) return null

        val handshakeType = tcpPayload[offset + 5].toInt() and 0xFF
        if (handshakeType != 0x01) return null // Not ClientHello

        return parseClientHello(tcpPayload, offset + 5, recordLen)
    }

    private fun parseClientHello(data: ByteArray, offset: Int, length: Int): String? {
        if (length < 39) return null
        val end = offset + length
        if (offset < 0 || end > data.size) return null
        var pos = offset + 1 // skip handshake type

        // Handshake length (3 bytes)
        pos += 3

        // Client version (2 bytes)
        pos += 2

        // Random (32 bytes)
        pos += 32

        if (pos >= end) return null

        // Session ID
        val sessionIdLen = data[pos].toInt() and 0xFF
        pos += 1 + sessionIdLen

        if (pos + 2 > end) return null

        // Cipher suites
        val cipherSuitesLen = u16(data, pos)
        pos += 2 + cipherSuitesLen

        if (pos + 1 > end) return null

        // Compression methods
        val compressionLen = data[pos].toInt() and 0xFF
        pos += 1 + compressionLen

        if (pos + 2 > end) return null

        // Extensions length
        val extensionsLen = u16(data, pos)
        pos += 2

        val extensionsEnd = pos + extensionsLen
        if (extensionsEnd > end) return null

        while (pos + 4 <= extensionsEnd) {
            val extType = u16(data, pos)
            val extLen = u16(data, pos + 2)
            pos += 4
            if (pos + extLen > extensionsEnd) return null

            if (extType == 0x0000) { // SNI extension
                return parseSniExtension(data, pos, extLen)
            }

            pos += extLen
        }
        return null
    }

    private fun parseSniExtension(data: ByteArray, offset: Int, length: Int): String? {
        if (length < 5) return null
        val end = offset + length
        if (offset < 0 || end > data.size) return null
        var pos = offset

        // SNI list length (2 bytes)
        val listLen = u16(data, pos)
        pos += 2
        if (listLen <= 0 || pos + listLen > end) return null

        // Server name type (1 byte) — must be 0 (host_name)
        val nameType = data[pos].toInt() and 0xFF
        if (nameType != 0) return null
        pos += 1

        // Host name length (2 bytes)
        if (pos + 2 > end) return null
        val nameLen = u16(data, pos)
        pos += 2

        if (pos + nameLen > end) return null
        if (nameLen == 0 || nameLen > 255) return null

        return String(data, pos, nameLen, Charsets.US_ASCII).lowercase()
    }

    private fun u16(data: ByteArray, pos: Int): Int {
        return ((data[pos].toInt() and 0xFF) shl 8) or (data[pos + 1].toInt() and 0xFF)
    }
}
