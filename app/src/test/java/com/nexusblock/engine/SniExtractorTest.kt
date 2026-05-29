package com.nexusblock.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SniExtractorTest {

    @Test
    fun extractsHostNameFromTlsClientHello() {
        val hello = tlsClientHello("pubads.g.doubleclick.net")

        assertEquals("pubads.g.doubleclick.net", SniExtractor.extractSni(hello, 0, hello.size))
    }

    @Test
    fun rejectsTruncatedClientHelloWithoutThrowing() {
        val hello = tlsClientHello("ads.hotstar.com").copyOf(24)

        assertNull(SniExtractor.extractSni(hello, 0, hello.size))
    }

    @Test
    fun rejectsInvalidOffsetAndLength() {
        val hello = tlsClientHello("aax-ott.amazon-adsystem.com")

        assertNull(SniExtractor.extractSni(hello, 2, hello.size))
    }

    private fun tlsClientHello(host: String): ByteArray {
        val hostBytes = host.toByteArray(Charsets.US_ASCII)
        val sniListLen = 1 + 2 + hostBytes.size
        val sniExtLen = 2 + sniListLen
        val extensionsLen = 4 + sniExtLen
        val handshakeLen = 2 + 32 + 1 + 2 + 2 + 1 + 1 + 2 + extensionsLen
        val recordLen = 4 + handshakeLen
        val out = ArrayList<Byte>(5 + recordLen)

        fun put(value: Int) { out.add((value and 0xFF).toByte()) }
        fun put16(value: Int) { put(value ushr 8); put(value) }
        fun put24(value: Int) { put(value ushr 16); put(value ushr 8); put(value) }

        put(0x16) // TLS handshake record
        put(0x03); put(0x03)
        put16(recordLen)
        put(0x01) // ClientHello
        put24(handshakeLen)
        put(0x03); put(0x03)
        repeat(32) { put(0) }
        put(0) // session id length
        put16(2); put16(0x1301) // TLS_AES_128_GCM_SHA256
        put(1); put(0) // null compression
        put16(extensionsLen)
        put16(0) // server_name extension
        put16(sniExtLen)
        put16(sniListLen)
        put(0) // host_name
        put16(hostBytes.size)
        hostBytes.forEach { out.add(it) }

        return out.toByteArray()
    }
}
