package com.nexusblock.engine

import android.net.VpnService
import android.util.Log
import kotlinx.coroutines.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.ConcurrentHashMap

/**
 * UDP relay for full-tunnel VPN mode.
 *
 * UDP is connectionless, so we maintain a socket-per-flow mapping keyed by
 * (srcIp, srcPort, dstIp, dstPort).  Replies from the real server are
 * wrapped back into IP/UDP packets and written to the TUN output.
 */
class UdpRelay(
    private val vpnService: VpnService,
    private val tunOutput: FileChannel
) {
    companion object {
        private const val TAG = "NexusBlock/UdpRelay"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val flows = ConcurrentHashMap<String, UdpFlow>()
    private val writeLock = Any()

    data class FlowKey(
        val srcIp: ByteArray, val srcPort: Int,
        val dstIp: ByteArray, val dstPort: Int
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is FlowKey) return false
            return srcPort == other.srcPort && dstPort == other.dstPort &&
                    srcIp.contentEquals(other.srcIp) && dstIp.contentEquals(other.dstIp)
        }
        override fun hashCode(): Int {
            var result = srcIp.contentHashCode()
            result = 31 * result + srcPort
            result = 31 * result + dstIp.contentHashCode()
            result = 31 * result + dstPort
            return result
        }
    }

    fun process(
        buffer: ByteBuffer,
        ipPos: Int,
        ipHeaderLen: Int,
        srcIp: ByteArray,
        dstIp: ByteArray,
        srcPort: Int,
        dstPort: Int
    ) {
        val key = flowKey(srcIp, srcPort, dstIp, dstPort)
        val udpOffset = ipPos + ipHeaderLen
        val udpLen = (buffer.getShort(udpOffset + 4).toInt() and 0xFFFF)
        val payloadLen = udpLen - 8
        if (payloadLen <= 0) return

        val payload = ByteArray(payloadLen)
        val mark = buffer.position()
        buffer.position(udpOffset + 8)
        buffer.get(payload)
        buffer.position(mark)

        var flow = flows[key]
        if (flow == null) {
            flow = UdpFlow(
                srcIp, dstIp, srcPort, dstPort,
                vpnService, tunOutput, writeLock, scope
            )
            val existing = flows.putIfAbsent(key, flow)
            if (existing != null) {
                flow.close()
                flow = existing
            } else {
                flow.startReceive()
            }
        }
        flow.send(payload)
    }

    fun stop() {
        scope.cancel()
        flows.values.forEach { it.close() }
        flows.clear()
    }

    private fun flowKey(
        srcIp: ByteArray, srcPort: Int, dstIp: ByteArray, dstPort: Int
    ): String = "${bytesToIp(srcIp)}:$srcPort-${bytesToIp(dstIp)}:$dstPort"

    private class UdpFlow(
        private val clientSrcIp: ByteArray,
        private val clientDstIp: ByteArray,
        private val clientSrcPort: Int,
        private val clientDstPort: Int,
        private val vpnService: VpnService,
        private val tunOutput: FileChannel,
        private val writeLock: Any,
        private val scope: CoroutineScope
    ) {
        private var socket: DatagramSocket? = null
        private var receiveJob: Job? = null
        private var ipId = (System.currentTimeMillis() and 0xFFFF).toInt()

        fun startReceive() {
            try {
                val s = DatagramSocket()
                vpnService.protect(s)
                socket = s
                receiveJob = scope.launch(Dispatchers.IO) {
                    val buf = ByteArray(65535)
                    while (isActive) {
                        try {
                            val pkt = DatagramPacket(buf, buf.size)
                            s.receive(pkt)
                            val replyData = pkt.data.copyOf(pkt.length)
                            sendReply(replyData)
                        } catch (e: Exception) {
                            if (isActive) Log.w(TAG, "UDP receive error", e)
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to create UDP flow socket", e)
            }
        }

        fun send(data: ByteArray) {
            try {
                val s = socket ?: return
                val pkt = DatagramPacket(
                    data, data.size,
                    InetAddress.getByAddress(clientDstIp), clientDstPort
                )
                s.send(pkt)
            } catch (e: Exception) {
                Log.w(TAG, "UDP send error", e)
            }
        }

        private fun sendReply(data: ByteArray) {
            val ipLen = 20
            val udpLen = 8 + data.size
            val totalLen = ipLen + udpLen
            val pkt = ByteBuffer.allocate(totalLen)

            // IP header: src = original dst, dst = original src
            pkt.put(0x45)
            pkt.put(0x00)
            pkt.putShort(totalLen.toShort())
            pkt.putShort((ipId++ and 0xFFFF).toShort())
            pkt.putShort(0x4000.toShort())
            pkt.put(64.toByte())
            pkt.put(17.toByte()) // UDP
            pkt.putShort(0) // checksum placeholder
            pkt.put(clientDstIp) // src = original destination
            pkt.put(clientSrcIp) // dst = original source

            // UDP header: reversed ports
            pkt.putShort(clientDstPort.toShort()) // src port
            pkt.putShort(clientSrcPort.toShort()) // dst port
            pkt.putShort(udpLen.toShort())
            pkt.putShort(0) // UDP checksum = 0 (optional in IPv4)

            // Data
            pkt.put(data)

            // Fix IP checksum
            val ipChecksum = calculateChecksum(pkt, 0, ipLen)
            pkt.putShort(10, ipChecksum.toShort())

            pkt.flip()
            synchronized(writeLock) {
                try { tunOutput.write(pkt) } catch (_: Exception) {}
            }
        }

        fun close() {
            receiveJob?.cancel()
            receiveJob = null
            try { socket?.close() } catch (_: Exception) {}
            socket = null
        }
    }

}
