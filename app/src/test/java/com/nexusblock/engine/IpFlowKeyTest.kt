package com.nexusblock.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class IpFlowKeyTest {

    @Test
    fun tcpFlowKeyIncludesDestinationAddressAndPort() {
        val sourceIp = ipv4(10, 0, 0, 2)
        val firstDestination = ipv4(203, 0, 113, 10)
        val secondDestination = ipv4(203, 0, 113, 11)

        val firstKey = IpFlowKey.from(sourceIp, 49152, firstDestination, 443, IpFlowKey.PROTOCOL_TCP)
        val differentDestinationKey = IpFlowKey.from(sourceIp, 49152, secondDestination, 443, IpFlowKey.PROTOCOL_TCP)
        val differentPortKey = IpFlowKey.from(sourceIp, 49152, firstDestination, 8443, IpFlowKey.PROTOCOL_TCP)

        assertNotEquals(firstKey, differentDestinationKey)
        assertNotEquals(firstKey, differentPortKey)
    }

    @Test
    fun udpFlowKeyIncludesDestinationPort() {
        val sourceIp = ipv4(10, 0, 0, 2)
        val destinationIp = ipv4(198, 51, 100, 20)

        val dnsLikeKey = IpFlowKey.from(sourceIp, 50000, destinationIp, 53, IpFlowKey.PROTOCOL_UDP)
        val quicLikeKey = IpFlowKey.from(sourceIp, 50000, destinationIp, 443, IpFlowKey.PROTOCOL_UDP)

        assertNotEquals(dnsLikeKey, quicLikeKey)
    }

    @Test
    fun identicalFlowValuesProduceIdenticalKeys() {
        val sourceIp = ipv4(10, 0, 0, 2)
        val destinationIp = ipv4(192, 0, 2, 30)

        val firstKey = IpFlowKey.from(sourceIp, 49152, destinationIp, 443, IpFlowKey.PROTOCOL_TCP)
        val secondKey = IpFlowKey.from(sourceIp.copyOf(), 49152, destinationIp.copyOf(), 443, IpFlowKey.PROTOCOL_TCP)

        assertEquals(firstKey, secondKey)
    }

    private fun ipv4(first: Int, second: Int, third: Int, fourth: Int): ByteArray = byteArrayOf(
        first.toByte(),
        second.toByte(),
        third.toByte(),
        fourth.toByte()
    )
}