package com.nexusblock.engine.network

import java.nio.ByteBuffer

/**
 * Zero-allocation IPv4 packet parser.
 *
 * Reads directly from the provided ByteBuffer without creating new objects.
 * All offsets are relative to the buffer's current position (which must be at
 * the start of the IP packet).
 */
object PacketParser {

    const val PROTO_ICMP = 1
    const val PROTO_TCP = 6
    const val PROTO_UDP = 17

    private const val OFFSET_VERSION_IHL = 0
    private const val OFFSET_TOTAL_LENGTH = 2
    private const val OFFSET_PROTOCOL = 9
    private const val OFFSET_SRC_IP = 12
    private const val OFFSET_DST_IP = 16

    /** IP header length in bytes (IHL * 4). */
    fun ipHeaderLength(buffer: ByteBuffer, pos: Int): Int {
        val ihl = buffer.get(pos + OFFSET_VERSION_IHL).toInt() and 0x0F
        return ihl * 4
    }

    /** Total packet length from IP header. */
    fun totalLength(buffer: ByteBuffer, pos: Int): Int {
        return buffer.getShort(pos + OFFSET_TOTAL_LENGTH).toInt() and 0xFFFF
    }

    fun protocol(buffer: ByteBuffer, pos: Int): Int {
        return buffer.get(pos + OFFSET_PROTOCOL).toInt() and 0xFF
    }

    /** Source IPv4 address as 4 packed bytes. Position-safe absolute read. */
    fun srcIpBytes(buffer: ByteBuffer, pos: Int): ByteArray {
        val ip = ByteArray(4)
        ip[0] = buffer.get(pos + OFFSET_SRC_IP)
        ip[1] = buffer.get(pos + OFFSET_SRC_IP + 1)
        ip[2] = buffer.get(pos + OFFSET_SRC_IP + 2)
        ip[3] = buffer.get(pos + OFFSET_SRC_IP + 3)
        return ip
    }

    /** Destination IPv4 address as 4 packed bytes. Position-safe absolute read. */
    fun dstIpBytes(buffer: ByteBuffer, pos: Int): ByteArray {
        val ip = ByteArray(4)
        ip[0] = buffer.get(pos + OFFSET_DST_IP)
        ip[1] = buffer.get(pos + OFFSET_DST_IP + 1)
        ip[2] = buffer.get(pos + OFFSET_DST_IP + 2)
        ip[3] = buffer.get(pos + OFFSET_DST_IP + 3)
        return ip
    }

    /** Source IPv4 address as raw Int (big-endian). */
    fun srcIpInt(buffer: ByteBuffer, pos: Int): Int {
        return buffer.getInt(pos + OFFSET_SRC_IP)
    }

    /** Destination IPv4 address as raw Int (big-endian). */
    fun dstIpInt(buffer: ByteBuffer, pos: Int): Int {
        return buffer.getInt(pos + OFFSET_DST_IP)
    }

    /** Source port from TCP/UDP header. */
    fun srcPort(buffer: ByteBuffer, transportOffset: Int): Int {
        return buffer.getShort(transportOffset).toInt() and 0xFFFF
    }

    /** Destination port from TCP/UDP header. */
    fun dstPort(buffer: ByteBuffer, transportOffset: Int): Int {
        return buffer.getShort(transportOffset + 2).toInt() and 0xFFFF
    }

    /** UDP length field. */
    fun udpLength(buffer: ByteBuffer, transportOffset: Int): Int {
        return buffer.getShort(transportOffset + 4).toInt() and 0xFFFF
    }

    /** TCP data offset in bytes (TCP header length). */
    fun tcpDataOffset(buffer: ByteBuffer, transportOffset: Int): Int {
        val dataOffsetByte = buffer.get(transportOffset + 12).toInt() and 0xFF
        return (dataOffsetByte shr 4) * 4
    }

    /** TCP flags byte. */
    fun tcpFlags(buffer: ByteBuffer, transportOffset: Int): Int {
        return buffer.get(transportOffset + 13).toInt() and 0xFF
    }

    /**
     * Extracts UDP payload into the provided [dest] array.
     * Returns number of payload bytes copied, or -1 if packet is too short.
     */
    fun copyUdpPayload(
        buffer: ByteBuffer,
        transportOffset: Int,
        dest: ByteArray,
        destOffset: Int = 0
    ): Int {
        val len = udpLength(buffer, transportOffset)
        val payloadLen = len - 8
        if (payloadLen <= 0) return 0
        if (buffer.remaining() < transportOffset + 8 + payloadLen) return -1
        val mark = buffer.position()
        buffer.position(transportOffset + 8)
        val toCopy = minOf(payloadLen, dest.size - destOffset)
        buffer.get(dest, destOffset, toCopy)
        buffer.position(mark)
        return toCopy
    }

    /** Formats raw IPv4 bytes as dotted-decimal string. */
    fun formatIpv4(bytes: ByteArray): String {
        return "${bytes[0].toInt() and 0xFF}.${bytes[1].toInt() and 0xFF}.${bytes[2].toInt() and 0xFF}.${bytes[3].toInt() and 0xFF}"
    }
}
