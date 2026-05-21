package com.nexusblock.engine

import java.nio.ByteBuffer

internal fun bytesToIp(b: ByteArray): String =
    "${b[0].toInt() and 0xFF}.${b[1].toInt() and 0xFF}.${b[2].toInt() and 0xFF}.${b[3].toInt() and 0xFF}"

internal fun calculateChecksum(buffer: ByteBuffer, offset: Int, length: Int): Int {
    var sum = 0
    var i = offset
    while (i < offset + length - 1) {
        sum += ((buffer.get(i).toInt() and 0xFF) shl 8) or (buffer.get(i + 1).toInt() and 0xFF)
        i += 2
    }
    if (i < offset + length) sum += (buffer.get(i).toInt() and 0xFF) shl 8
    while (sum shr 16 != 0) sum = (sum and 0xFFFF) + (sum shr 16)
    return sum.inv() and 0xFFFF
}

internal fun calculatePseudoChecksum(
    buffer: ByteBuffer,
    srcIp: ByteArray,
    dstIp: ByteArray,
    protocol: Int,
    tcpLen: Int
): Int {
    val pseudo = ByteBuffer.allocate(12 + tcpLen)
    pseudo.put(srcIp)
    pseudo.put(dstIp)
    pseudo.put(0)
    pseudo.put(protocol.toByte())
    pseudo.putShort(tcpLen.toShort())
    for (i in 0 until tcpLen) pseudo.put(buffer.get(20 + i))
    pseudo.flip()
    return calculateChecksum(pseudo, 0, pseudo.remaining())
}
