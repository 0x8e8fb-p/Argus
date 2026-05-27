package com.nexusblock.engine

import android.net.VpnService
import android.util.Log
import kotlinx.coroutines.*
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.SocketChannel
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Userspace TCP relay engine for full-route VPN.
 *
 * Implements "split TCP": terminates TCP from apps at the TUN interface
 * (raw packet manipulation) and relays data to/from real servers via
 * protected [SocketChannel]s that bypass the VPN.
 *
 * Flow for each connection:
 * 1. App sends SYN → we send SYN-ACK back to TUN (connection accepted)
 * 2. App sends data (e.g., TLS ClientHello) → we forward to real server
 * 3. Real server sends response → we inject TCP data back into TUN
 * 4. App sends FIN → we FIN real server, send FIN-ACK to app
 */
@Singleton
class TcpRelayEngine @Inject constructor(
    private val dnsEngine: DnsFilterEngine
) {
    companion object {
        private const val TAG = "Argus/TcpRelay"
        private const val MAX_SESSIONS = 512
        private const val MAX_PENDING_BYTES_PER_SESSION = 64 * 1024
        private const val SESSION_TIMEOUT_MS = 60_000L
        private const val CONNECT_TIMEOUT_MS = 10_000L
        private const val WRITE_TIMEOUT_MS = 5_000L
        private const val MAX_ZERO_WRITES = 64
        private const val BUFFER_SIZE = 16384
        private const val IDLE_DELAY_NO_SESSIONS_MS = 100L
        private const val IDLE_DELAY_ACTIVE_SESSIONS_MS = 25L

        private const val TCP_FIN = 0x01
        private const val TCP_SYN = 0x02
        private const val TCP_RST = 0x04
        private const val TCP_PSH = 0x08
        private const val TCP_ACK = 0x10
        private const val TCP_SYN_ACK = 0x12
        private const val TCP_FIN_ACK = 0x11
    }

    private val sessions = ConcurrentHashMap<IpFlowKey, TcpSession>(256)
    private val secureRandom = java.security.SecureRandom()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var readerJob: Job? = null
    private var cleanupJob: Job? = null

    @Volatile
    private var vpnService: VpnService? = null
    @Volatile
    private var tunOutput: FileChannel? = null
    private val outputLock = Any()

    var blockedCallback: ((String, String) -> Unit)? = null

    @Volatile
    var blockUnknownCloudFrontDistributions: Boolean = true

    fun start(service: VpnService, output: FileChannel) {
        vpnService = service
        tunOutput = output

        // Periodic reader: polls all active sessions for server responses
        readerJob?.cancel()
        readerJob = scope.launch {
            val readBuf = ByteBuffer.allocate(BUFFER_SIZE)
            while (isActive) {
                var hadData = false
                for ((_, session) in sessions) {
                    if (session.state == SessionState.ESTABLISHED ||
                        session.state == SessionState.FIN_WAIT) {
                        try {
                            val channel = session.serverChannel ?: continue
                            if (!channel.isConnected) continue
                            readBuf.clear()
                            val read = channel.read(readBuf)
                            if (read > 0) {
                                hadData = true
                                readBuf.flip()
                                val data = ByteArray(read)
                                readBuf.get(data)
                                sendDataToApp(session, data)
                                session.lastActivity = System.currentTimeMillis()
                            } else if (read == -1) {
                                // Server closed connection
                                sendFinToApp(session)
                                session.state = SessionState.CLOSED
                            }
                        } catch (e: Exception) {
                            session.state = SessionState.CLOSED
                        }
                    }
                }
                if (!hadData) {
                    delay(if (sessions.isEmpty()) IDLE_DELAY_NO_SESSIONS_MS else IDLE_DELAY_ACTIVE_SESSIONS_MS)
                }
            }
        }

        // Session cleanup
        cleanupJob?.cancel()
        cleanupJob = scope.launch {
            while (isActive) {
                delay(10_000)
                val now = System.currentTimeMillis()
                val stale = sessions.entries.filter { (_, s) ->
                    s.state == SessionState.CLOSED ||
                        now - s.lastActivity > SESSION_TIMEOUT_MS
                }
                for ((key, session) in stale) {
                    closeSession(session)
                    sessions.remove(key)
                }
                if (stale.isNotEmpty()) {
                    Log.d(TAG, "Cleaned ${stale.size} sessions, ${sessions.size} active")
                }
            }
        }

        Log.i(TAG, "TCP relay engine started")
    }

    fun stop() {
        readerJob?.cancel()
        cleanupJob?.cancel()
        for ((_, session) in sessions) {
            closeSession(session)
        }
        sessions.clear()
        vpnService = null
        tunOutput = null
        Log.i(TAG, "TCP relay engine stopped")
    }

    /**
     * Handle an incoming TCP packet from the TUN device.
     * Called by PacketRouter for every TCP packet.
     *
     * @return true if handled (relayed or blocked), false if packet should be dropped
     */
    fun handlePacket(
        buffer: ByteBuffer,
        ipPos: Int,
        ipHeaderLen: Int,
        srcIp: ByteArray,
        dstIp: ByteArray,
        srcPort: Int,
        dstPort: Int,
        tcpOffset: Int,
        totalLen: Int
    ): Boolean {
        val flags = buffer.get(tcpOffset + 13).toInt() and 0xFF
        val dataOffset = ((buffer.get(tcpOffset + 12).toInt() shr 4) and 0x0F) * 4
        val payloadOffset = tcpOffset + dataOffset
        val payloadLen = totalLen - ipHeaderLen - dataOffset
        val seqNum = buffer.getInt(tcpOffset + 4).toLong() and 0xFFFFFFFFL
        val sessionKey = IpFlowKey.from(srcIp, srcPort, dstIp, dstPort, IpFlowKey.PROTOCOL_TCP)

        when {
            // SYN — new connection
            (flags and TCP_SYN) != 0 && (flags and TCP_ACK) == 0 -> {
                return handleSyn(sessionKey, srcIp, dstIp, srcPort, dstPort, seqNum)
            }
            // ACK with data — relay payload
            payloadLen > 0 && (flags and TCP_ACK) != 0 -> {
                val session = sessions[sessionKey] ?: return false
                val payload = ByteArray(payloadLen)
                val mark = buffer.position()
                buffer.position(payloadOffset)
                buffer.get(payload)
                buffer.position(mark)
                return handleData(session, payload, seqNum)
            }
            // FIN
            (flags and TCP_FIN) != 0 -> {
                val session = sessions[sessionKey] ?: return false
                return handleFin(session, seqNum)
            }
            // RST
            (flags and TCP_RST) != 0 -> {
                val session = sessions[sessionKey]
                if (session != null) {
                    closeSession(session)
                    sessions.remove(sessionKey)
                }
                return true
            }
            // Pure ACK (no data) — just acknowledge, don't relay
            (flags and TCP_ACK) != 0 && payloadLen == 0 -> {
                val session = sessions[sessionKey] ?: return false
                session.lastActivity = System.currentTimeMillis()
                return true
            }
        }
        return false
    }

    private fun handleSyn(
        sessionKey: IpFlowKey,
        srcIp: ByteArray,
        dstIp: ByteArray,
        srcPort: Int,
        dstPort: Int,
        seqNum: Long
    ): Boolean {
        sessions.remove(sessionKey)?.let { closeSession(it) }
        if (sessions.size >= MAX_SESSIONS && !evictOldestSession()) {
            Log.w(TAG, "Session limit reached, dropping SYN")
            return false
        }

        // Check if destination should be blocked (SNI not available yet, but IP might be blocked)
        val dstIpStr = InetAddress.getByAddress(dstIp).hostAddress ?: return false
        if (dnsEngine.isIpBlocked(dstIpStr)) {
            blockedCallback?.invoke(dstIpStr, "ip-block")
            return true // Drop SYN silently (no RST to prevent retries)
        }

        val session = TcpSession(
            srcIp = srcIp.copyOf(),
            dstIp = dstIp.copyOf(),
            srcPort = srcPort,
            dstPort = dstPort,
            appInitSeq = seqNum,
            appSeqNum = seqNum + 1, // After SYN, next expected from app
            ourSeqNum = secureRandom.nextInt().toLong() and 0xFFFFFFFFL, // Secure random ISN
            state = SessionState.SYN_RECEIVED,
            lastActivity = System.currentTimeMillis(),
            pendingData = PendingByteQueue(MAX_PENDING_BYTES_PER_SESSION)
        )

        sessions[sessionKey] = session

        // Send SYN-ACK to app
        sendSynAck(session)

        // Open connection to real server (async)
        scope.launch {
            try {
                val channel = SocketChannel.open()
                channel.configureBlocking(false)
                vpnService?.protect(channel.socket())

                val addr = InetSocketAddress(InetAddress.getByAddress(dstIp), dstPort)
                channel.connect(addr)

                // Wait for connection (with timeout)
                val deadline = System.currentTimeMillis() + CONNECT_TIMEOUT_MS
                while (!channel.finishConnect()) {
                    if (System.currentTimeMillis() > deadline) {
                        throw Exception("Connect timeout")
                    }
                    delay(5)
                }

                session.serverChannel = channel
                session.state = SessionState.ESTABLISHED

                // Flush any buffered data that arrived before connect completed
                val pending = session.drainPendingData()
                if (pending != null) {
                    if (!writeToServer(channel, pending)) {
                        throw Exception("Server write timeout")
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Connect failed to $dstIpStr:${session.dstPort}: ${e.message}")
                sendRstToApp(session)
                session.state = SessionState.CLOSED
            }
        }

        return true
    }

    private fun handleData(session: TcpSession, payload: ByteArray, seqNum: Long): Boolean {
        session.appSeqNum = seqNum + payload.size.toLong()
        session.lastActivity = System.currentTimeMillis()

        // Send ACK to app
        sendAckToApp(session)

        // SNI inspection on first data packet (TLS ClientHello on port 443)
        if (session.dstPort == 443 && !session.sniChecked) {
            session.sniChecked = true
            val sni = SniExtractor.extractSni(payload, 0, payload.size)
            if (sni != null) {
                session.sni = sni
                // Check if domain should be blocked
                if (dnsEngine.isDomainBlocked(sni)) {
                    Log.d(TAG, "SNI blocked: $sni")
                    sendRstToApp(session)
                    session.state = SessionState.CLOSED
                    blockedCallback?.invoke(sni, "sni")
                    return true
                }
                // CloudFront default-deny
                if (blockUnknownCloudFrontDistributions && dnsEngine.isUnknownCloudFrontDistribution(sni)) {
                    Log.d(TAG, "CloudFront default-deny: $sni")
                    sendRstToApp(session)
                    session.state = SessionState.CLOSED
                    blockedCallback?.invoke(sni, "sni-cf-deny")
                    return true
                }
            }
        }

        // Forward data to real server
        val channel = session.serverChannel
        if (channel != null && channel.isConnected) {
            try {
                if (!writeToServer(channel, payload)) {
                    throw Exception("Server write timeout")
                }
            } catch (e: Exception) {
                sendRstToApp(session)
                session.state = SessionState.CLOSED
            }
        } else {
            // Connection not ready yet, buffer the data
            if (!session.bufferPendingData(payload)) {
                Log.d(TAG, "Pending data cap exceeded for ${session.dstPort}, closing session")
                sendRstToApp(session)
                closeSession(session)
                session.state = SessionState.CLOSED
                blockedCallback?.invoke("${session.dstPort}", "tcp-buffer-overflow")
            }
        }

        return true
    }

    private fun writeToServer(channel: SocketChannel, payload: ByteArray): Boolean {
        val buffer = ByteBuffer.wrap(payload)
        val deadline = System.currentTimeMillis() + WRITE_TIMEOUT_MS
        var zeroWrites = 0

        while (buffer.hasRemaining()) {
            val written = channel.write(buffer)
            if (written > 0) {
                zeroWrites = 0
                continue
            }

            zeroWrites++
            if (written < 0 || zeroWrites > MAX_ZERO_WRITES || System.currentTimeMillis() > deadline) {
                return false
            }
            Thread.sleep(1)
        }
        return true
    }

    private fun evictOldestSession(): Boolean {
        val oldestEntry = sessions.entries.minByOrNull { it.value.lastActivity } ?: return false
        closeSession(oldestEntry.value)
        return sessions.remove(oldestEntry.key, oldestEntry.value)
    }

    private fun handleFin(session: TcpSession, seqNum: Long): Boolean {
        session.appSeqNum = seqNum + 1
        session.lastActivity = System.currentTimeMillis()

        // Send FIN-ACK to app
        sendFinAckToApp(session)

        // Close server connection
        try { session.serverChannel?.close() } catch (_: Exception) {}
        session.state = SessionState.CLOSED

        return true
    }

    // --- Packet Construction (TUN injection) ---

    private fun sendSynAck(session: TcpSession) {
        val pkt = buildTcpPacket(
            srcIp = session.dstIp,
            dstIp = session.srcIp,
            srcPort = session.dstPort,
            dstPort = session.srcPort,
            seqNum = session.ourSeqNum,
            ackNum = session.appSeqNum,
            flags = TCP_SYN_ACK,
            payload = null,
            // Set MSS option in SYN-ACK
            options = buildMssOption(1400)
        )
        session.ourSeqNum = (session.ourSeqNum + 1) and 0xFFFFFFFFL
        writeTun(pkt)
    }

    private fun sendAckToApp(session: TcpSession) {
        val pkt = buildTcpPacket(
            srcIp = session.dstIp,
            dstIp = session.srcIp,
            srcPort = session.dstPort,
            dstPort = session.srcPort,
            seqNum = session.ourSeqNum,
            ackNum = session.appSeqNum,
            flags = TCP_ACK,
            payload = null
        )
        writeTun(pkt)
    }

    private fun sendDataToApp(session: TcpSession, data: ByteArray) {
        // Split into MSS-sized chunks
        val mss = 1400
        var offset = 0
        while (offset < data.size) {
            val chunkLen = minOf(mss, data.size - offset)
            val chunk = data.copyOfRange(offset, offset + chunkLen)
            val pkt = buildTcpPacket(
                srcIp = session.dstIp,
                dstIp = session.srcIp,
                srcPort = session.dstPort,
                dstPort = session.srcPort,
                seqNum = session.ourSeqNum,
                ackNum = session.appSeqNum,
                flags = TCP_ACK or TCP_PSH,
                payload = chunk
            )
            session.ourSeqNum = (session.ourSeqNum + chunkLen) and 0xFFFFFFFFL
            writeTun(pkt)
            offset += chunkLen
        }
    }

    private fun sendFinToApp(session: TcpSession) {
        val pkt = buildTcpPacket(
            srcIp = session.dstIp,
            dstIp = session.srcIp,
            srcPort = session.dstPort,
            dstPort = session.srcPort,
            seqNum = session.ourSeqNum,
            ackNum = session.appSeqNum,
            flags = TCP_FIN_ACK,
            payload = null
        )
        session.ourSeqNum = (session.ourSeqNum + 1) and 0xFFFFFFFFL
        writeTun(pkt)
    }

    private fun sendFinAckToApp(session: TcpSession) {
        val pkt = buildTcpPacket(
            srcIp = session.dstIp,
            dstIp = session.srcIp,
            srcPort = session.dstPort,
            dstPort = session.srcPort,
            seqNum = session.ourSeqNum,
            ackNum = session.appSeqNum,
            flags = TCP_FIN_ACK,
            payload = null
        )
        session.ourSeqNum = (session.ourSeqNum + 1) and 0xFFFFFFFFL
        writeTun(pkt)
    }

    private fun sendRstToApp(session: TcpSession) {
        val pkt = buildTcpPacket(
            srcIp = session.dstIp,
            dstIp = session.srcIp,
            srcPort = session.dstPort,
            dstPort = session.srcPort,
            seqNum = session.ourSeqNum,
            ackNum = session.appSeqNum,
            flags = TCP_RST or TCP_ACK,
            payload = null
        )
        writeTun(pkt)
    }

    private fun buildTcpPacket(
        srcIp: ByteArray,
        dstIp: ByteArray,
        srcPort: Int,
        dstPort: Int,
        seqNum: Long,
        ackNum: Long,
        flags: Int,
        payload: ByteArray?,
        options: ByteArray? = null
    ): ByteBuffer {
        val tcpHeaderLen = 20 + (options?.size ?: 0)
        val dataOffsetWords = tcpHeaderLen / 4
        val totalLen = 20 + tcpHeaderLen + (payload?.size ?: 0)
        val pkt = ByteBuffer.allocate(totalLen)

        // IP header (20 bytes)
        pkt.put(0x45.toByte())           // Version 4, IHL 5
        pkt.put(0x00.toByte())           // DSCP/ECN
        pkt.putShort(totalLen.toShort()) // Total length
        pkt.putShort(0)                  // Identification
        pkt.putShort(0x4000.toShort())   // Don't Fragment
        pkt.put(64.toByte())             // TTL
        pkt.put(6.toByte())              // Protocol = TCP
        pkt.putShort(0)                  // Checksum placeholder
        pkt.put(srcIp)                   // Source IP
        pkt.put(dstIp)                   // Destination IP

        // TCP header
        pkt.putShort(srcPort.toShort())  // Source port
        pkt.putShort(dstPort.toShort())  // Destination port
        pkt.putInt(seqNum.toInt())       // Sequence number
        pkt.putInt(ackNum.toInt())       // Acknowledgment number
        pkt.put((dataOffsetWords shl 4).toByte()) // Data offset
        pkt.put(flags.toByte())          // Flags
        pkt.putShort(65535.toShort())    // Window size (max)
        pkt.putShort(0)                  // Checksum placeholder
        pkt.putShort(0)                  // Urgent pointer

        // TCP options (if any)
        if (options != null) {
            pkt.put(options)
        }

        // Payload
        if (payload != null) {
            pkt.put(payload)
        }

        // IP checksum
        val ipChecksum = calculateChecksum(pkt, 0, 20)
        pkt.putShort(10, ipChecksum.toShort())

        // TCP checksum (pseudo-header based)
        val tcpLen = tcpHeaderLen + (payload?.size ?: 0)
        val tcpChecksum = calculateTcpChecksum(pkt, srcIp, dstIp, tcpLen)
        pkt.putShort(20 + 16, tcpChecksum.toShort()) // Offset 36 from start

        pkt.flip()
        return pkt
    }

    private fun buildMssOption(mss: Int): ByteArray {
        // MSS option: Kind=2, Length=4, MSS value (2 bytes)
        return byteArrayOf(
            0x02, 0x04,
            ((mss shr 8) and 0xFF).toByte(),
            (mss and 0xFF).toByte()
        )
    }

    private fun calculateChecksum(buf: ByteBuffer, offset: Int, length: Int): Int {
        var sum = 0L
        var i = offset
        while (i < offset + length - 1) {
            sum += (buf.getShort(i).toLong() and 0xFFFFL)
            i += 2
        }
        if (length % 2 != 0) {
            sum += ((buf.get(offset + length - 1).toLong() and 0xFFL) shl 8)
        }
        while (sum shr 16 != 0L) {
            sum = (sum and 0xFFFFL) + (sum shr 16)
        }
        return (sum.toInt().inv()) and 0xFFFF
    }

    private fun calculateTcpChecksum(pkt: ByteBuffer, srcIp: ByteArray, dstIp: ByteArray, tcpLen: Int): Int {
        var sum = 0L

        // Pseudo-header
        for (i in 0 until 4 step 2) {
            sum += ((srcIp[i].toInt() and 0xFF) shl 8) or (srcIp[i + 1].toInt() and 0xFF)
        }
        for (i in 0 until 4 step 2) {
            sum += ((dstIp[i].toInt() and 0xFF) shl 8) or (dstIp[i + 1].toInt() and 0xFF)
        }
        sum += 6L // Protocol TCP
        sum += tcpLen.toLong()

        // TCP segment (header + data)
        val tcpStart = 20 // IP header is always 20 bytes in our packets
        var i = tcpStart
        while (i < tcpStart + tcpLen - 1) {
            sum += (pkt.getShort(i).toLong() and 0xFFFFL)
            i += 2
        }
        if (tcpLen % 2 != 0) {
            sum += ((pkt.get(tcpStart + tcpLen - 1).toLong() and 0xFFL) shl 8)
        }

        while (sum shr 16 != 0L) {
            sum = (sum and 0xFFFFL) + (sum shr 16)
        }
        return (sum.toInt().inv()) and 0xFFFF
    }

    private fun writeTun(pkt: ByteBuffer) {
        val output = tunOutput ?: return
        try {
            synchronized(outputLock) {
                output.write(pkt)
            }
        } catch (_: Exception) {}
    }

    private fun closeSession(session: TcpSession) {
        try { session.serverChannel?.close() } catch (_: Exception) {}
    }

    // --- Session Data Class ---

    private enum class SessionState {
        SYN_RECEIVED, ESTABLISHED, FIN_WAIT, CLOSED
    }

    private class TcpSession(
        val srcIp: ByteArray,
        val dstIp: ByteArray,
        val srcPort: Int,
        val dstPort: Int,
        val appInitSeq: Long,
        var appSeqNum: Long,     // Next expected seq from app
        var ourSeqNum: Long,     // Our next seq to send to app
        var state: SessionState,
        var lastActivity: Long,
        var serverChannel: SocketChannel? = null,
        var sniChecked: Boolean = false,
        var sni: String? = null,
        private val pendingData: PendingByteQueue
    ) {
        @Synchronized
        fun bufferPendingData(data: ByteArray): Boolean = pendingData.offer(data)

        @Synchronized
        fun drainPendingData(): ByteArray? = pendingData.drain()
    }
}
