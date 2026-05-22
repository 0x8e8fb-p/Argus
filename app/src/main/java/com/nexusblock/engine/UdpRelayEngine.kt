package com.nexusblock.engine

import android.net.VpnService
import android.util.Log
import kotlinx.coroutines.*
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.nio.channels.FileChannel
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UDP relay for non-DNS traffic in full-route VPN mode.
 * DNS (port 53) is still handled by [DnsFilterEngine] in [PacketRouter].
 * All other UDP is relayed to the real destination via protected [DatagramChannel]s.
 */
@Singleton
class UdpRelayEngine @Inject constructor() {

    companion object {
        private const val TAG = "NexusBlock/UdpRelay"
        private const val MAX_SESSIONS = 500
        private const val SESSION_TIMEOUT_MS = 30_000L
    }

    private val sessions = ConcurrentHashMap<Long, UdpSession>(128)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var readerJob: Job? = null
    private var cleanupJob: Job? = null

    @Volatile
    private var vpnService: VpnService? = null
    @Volatile
    private var tunOutput: FileChannel? = null
    private val outputLock = Any()

    fun start(service: VpnService, output: FileChannel) {
        vpnService = service
        tunOutput = output

        // Reader: check all UDP sessions for incoming responses
        readerJob?.cancel()
        readerJob = scope.launch {
            val readBuf = ByteBuffer.allocate(65536)
            while (isActive) {
                var hadData = false
                for ((_, session) in sessions) {
                    try {
                        val channel = session.channel ?: continue
                        readBuf.clear()
                        val addr = channel.receive(readBuf)
                        if (addr != null) {
                            hadData = true
                            readBuf.flip()
                            val data = ByteArray(readBuf.remaining())
                            readBuf.get(data)
                            sendUdpToApp(session, data)
                            session.lastActivity = System.currentTimeMillis()
                        }
                    } catch (_: Exception) {}
                }
                if (!hadData) delay(2)
            }
        }

        // Cleanup stale sessions
        cleanupJob?.cancel()
        cleanupJob = scope.launch {
            while (isActive) {
                delay(10_000)
                val now = System.currentTimeMillis()
                val stale = sessions.entries.filter { (_, s) ->
                    now - s.lastActivity > SESSION_TIMEOUT_MS
                }
                for ((key, session) in stale) {
                    try { session.channel?.close() } catch (_: Exception) {}
                    sessions.remove(key)
                }
            }
        }

        Log.i(TAG, "UDP relay engine started")
    }

    fun stop() {
        readerJob?.cancel()
        cleanupJob?.cancel()
        for ((_, session) in sessions) {
            try { session.channel?.close() } catch (_: Exception) {}
        }
        sessions.clear()
        vpnService = null
        tunOutput = null
        Log.i(TAG, "UDP relay engine stopped")
    }

    /**
     * Relay a non-DNS UDP packet to its real destination.
     * Returns true if handled.
     */
    fun handlePacket(
        srcIp: ByteArray,
        dstIp: ByteArray,
        srcPort: Int,
        dstPort: Int,
        payload: ByteArray
    ): Boolean {
        val sessionKey = (srcPort.toLong() shl 32) or
            ((dstIp[0].toLong() and 0xFF) shl 24) or
            ((dstIp[1].toLong() and 0xFF) shl 16) or
            ((dstIp[2].toLong() and 0xFF) shl 8) or
            (dstIp[3].toLong() and 0xFF)

        val session = sessions.getOrPut(sessionKey) {
            if (sessions.size >= MAX_SESSIONS) return false
            val channel = DatagramChannel.open()
            channel.configureBlocking(false)
            vpnService?.protect(channel.socket())
            channel.connect(InetSocketAddress(InetAddress.getByAddress(dstIp), dstPort))
            UdpSession(
                srcIp = srcIp.copyOf(),
                dstIp = dstIp.copyOf(),
                srcPort = srcPort,
                dstPort = dstPort,
                channel = channel,
                lastActivity = System.currentTimeMillis()
            )
        }

        // Send to real destination
        try {
            session.channel?.write(ByteBuffer.wrap(payload))
            session.lastActivity = System.currentTimeMillis()
        } catch (e: Exception) {
            sessions.remove(sessionKey)
            return false
        }

        return true
    }

    private fun sendUdpToApp(session: UdpSession, data: ByteArray) {
        val output = tunOutput ?: return
        val ipHeaderLen = 20
        val udpLen = 8 + data.size
        val totalLen = ipHeaderLen + udpLen
        val pkt = ByteBuffer.allocate(totalLen)

        // IP header (source = real server, dest = app)
        pkt.put(0x45.toByte())
        pkt.put(0x00.toByte())
        pkt.putShort(totalLen.toShort())
        pkt.putShort(0)
        pkt.putShort(0x4000.toShort())
        pkt.put(64.toByte())
        pkt.put(17.toByte()) // UDP
        pkt.putShort(0)      // Checksum placeholder
        pkt.put(session.dstIp) // Source = real server
        pkt.put(session.srcIp) // Dest = app

        // UDP header
        pkt.putShort(session.dstPort.toShort()) // Source port = server port
        pkt.putShort(session.srcPort.toShort()) // Dest port = app port
        pkt.putShort(udpLen.toShort())
        pkt.putShort(0) // Checksum optional in IPv4

        // Payload
        pkt.put(data)

        // IP checksum
        var sum = 0L
        for (i in 0 until 20 step 2) {
            sum += (pkt.getShort(i).toLong() and 0xFFFFL)
        }
        while (sum shr 16 != 0L) sum = (sum and 0xFFFFL) + (sum shr 16)
        pkt.putShort(10, (sum.toInt().inv() and 0xFFFF).toShort())

        pkt.flip()
        try {
            synchronized(outputLock) {
                output.write(pkt)
            }
        } catch (_: Exception) {}
    }

    private class UdpSession(
        val srcIp: ByteArray,
        val dstIp: ByteArray,
        val srcPort: Int,
        val dstPort: Int,
        val channel: DatagramChannel?,
        var lastActivity: Long
    )
}
