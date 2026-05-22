package com.nexusblock.engine

internal data class IpFlowKey(
    val sourceAddress: Int,
    val sourcePort: Int,
    val destinationAddress: Int,
    val destinationPort: Int,
    val protocol: Int
) {
    companion object {
        const val PROTOCOL_TCP = 6
        const val PROTOCOL_UDP = 17

        fun from(
            sourceIp: ByteArray,
            sourcePort: Int,
            destinationIp: ByteArray,
            destinationPort: Int,
            protocol: Int
        ): IpFlowKey = IpFlowKey(
            sourceAddress = ipv4ToInt(sourceIp),
            sourcePort = sourcePort,
            destinationAddress = ipv4ToInt(destinationIp),
            destinationPort = destinationPort,
            protocol = protocol
        )

        private fun ipv4ToInt(address: ByteArray): Int {
            require(address.size == 4) { "IPv4 flow keys require 4-byte addresses" }
            return ((address[0].toInt() and 0xFF) shl 24) or
                ((address[1].toInt() and 0xFF) shl 16) or
                ((address[2].toInt() and 0xFF) shl 8) or
                (address[3].toInt() and 0xFF)
        }
    }
}