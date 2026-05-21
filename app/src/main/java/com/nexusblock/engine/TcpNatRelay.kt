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

/**
 * TCP NAT relay for full-tunnel VPN mode.
 *
 * Intercepts TCP connections entering the TUN, completes the 3-way handshake
 * with the local client, opens a protected [SocketChannel] to the real
 * destination, and relays payload data both ways.
 *
 * The first client payload is scanned for TLS SNI; if it matches a blocked
 * domain the connection is RST-killed.
 *
 * State machine:
 *   SYN_RECEIVED: sent SYN+ACK, upstream connection in progress
 *   ESTABLISHED:  upstream connected, data flows both ways
 *   CLOSED:       session torn down
 */
class TcpNatRelay(
    private val vpnService: VpnService,
    private val tunOutput: FileChannel,
    private val shouldInspectSni: () -> Boolean,
    private val isDomainBlocked: (String) -> Boolean
) {
    companion object {
        private const val TAG = "NexusBlock/TcpRelay"
        private const val TLS_HANDSHAKE = 22

        private const val TCP_SYN = 0x02
        private const val TCP_ACK = 0x10
        private const val TCP_PSH = 0x08
        private const val TCP_FIN = 0x01
        private const val TCP_RST = 0x04
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sessions = ConcurrentHashMap<String, TcpSession>()
    private val writeLock = Any()

    fun process(
        buffer: ByteBuffer,
        ipPos: Int,
        ipHeaderLen: Int,
        srcIp: ByteArray,
        dstIp: ByteArray,
        srcPort: Int,
        dstPort: Int,
        tcpPayloadLen: Int
    ) {
        val key = sessionKey(srcIp, srcPort, dstIp, dstPort)
        val tcpOffset = ipPos + ipHeaderLen
        val flags = buffer.get(tcpOffset + 13).toInt() and 0xFF
        val isSyn = (flags and TCP_SYN) != 0
        val isRst = (flags and TCP_RST) != 0
        val isFin = (flags and TCP_FIN) != 0

        val existing = sessions[key]

        if (isRst || isFin) {
            existing?.close()
            sessions.remove(key)
            return
        }

        if (existing == null) {
            if (isSyn) {
                val sess = TcpSession(
                    srcIp, dstIp, srcPort, dstPort,
                    vpnService, tunOutput, writeLock, scope,
                    shouldInspectSni, isDomainBlocked
                )
                sessions[key] = sess
                sess.onSyn(buffer, ipPos, ipHeaderLen)
            }
            return
        }

        existing.onPacket(buffer, ipPos, ipHeaderLen, tcpPayloadLen)
    }

    fun stop() {
        scope.cancel()
        sessions.values.forEach { it.close() }
        sessions.clear()
    }

    private fun sessionKey(
        srcIp: ByteArray, srcPort: Int, dstIp: ByteArray, dstPort: Int
    ): String = "${bytesToIp(srcIp)}:$srcPort-${bytesToIp(dstIp)}:$dstPort"

    private class TcpSession(
        private val clientSrcIp: ByteArray,
        private val clientDstIp: ByteArray,
        private val clientSrcPort: Int,
        private val clientDstPort: Int,
        private val vpnService: VpnService,
        private val tunOutput: FileChannel,
        private val writeLock: Any,
        private val scope: CoroutineScope,
        private val shouldInspectSni: () -> Boolean,
        private val isDomainBlocked: (String) -> Boolean
    ) {
        private var state = State.SYN_RECEIVED
        private var serverChannel: SocketChannel? = null
        private var relayJob: Job? = null

        private var localSeq: Long = 0
        private var localAck: Long = 0
        private var remoteSeq: Long = 0
        private var ipId = (System.currentTimeMillis() and 0xFFFF).toInt()
        private var sniChecked = false

        private val pendingPayloads = mutableListOf<ByteArray>()

        enum class State { SYN_RECEIVED, ESTABLISHED, CLOSED }

        fun onSyn(buffer: ByteBuffer, ipPos: Int, ipHeaderLen: Int) {
            val tcpOffset = ipPos + ipHeaderLen
            val remoteSeqRaw = buffer.getInt(tcpOffset + 4).toLong() and 0xFFFFFFFFL
            remoteSeq = remoteSeqRaw
            localSeq = System.nanoTime() and 0xFFFFFFFFL
            localAck = (remoteSeq + 1) and 0xFFFFFFFFL

            // Complete handshake with client immediately
            sendTcpPacket(flags = TCP_SYN or TCP_ACK, data = ByteBuffer.allocate(0))

            // Start upstream connection in background
            scope.launch(Dispatchers.IO) {
                try {
                    val ch = SocketChannel.open()
                    ch.configureBlocking(true)
                    vpnService.protect(ch.socket())
                    ch.connect(
                        InetSocketAddress(
                            InetAddress.getByAddress(clientDstIp),
                            clientDstPort
                        )
                    )
                    if (state != State.CLOSED) {
                        serverChannel = ch
                        state = State.ESTABLISHED
                        // Flush any payloads that arrived while connecting
                        for (p in pendingPayloads) {
                            serverChannel?.write(ByteBuffer.wrap(p))
                        }
                        pendingPayloads.clear()
                        startServerRelay()
                    } else {
                        try { ch.close() } catch (_: Exception) {}
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Upstream connect failed to ${bytesToIp(clientDstIp)}:$clientDstPort", e)
                    if (state != State.CLOSED) {
                        sendRstToClient()
                    }
                    close()
                }
            }
        }

        fun onPacket(buffer: ByteBuffer, ipPos: Int, ipHeaderLen: Int, payloadLen: Int) {
            if (state == State.CLOSED) return

            val tcpOffset = ipPos + ipHeaderLen
            val seq = buffer.getInt(tcpOffset + 4).toLong() and 0xFFFFFFFFL
            remoteSeq = seq

            if (payloadLen > 0) {
                val payload = ByteArray(payloadLen)
                val mark = buffer.position()
                buffer.position(tcpOffset + 20)
                buffer.get(payload)
                buffer.position(mark)

                // SNI inspection on first client payload
                if (!sniChecked && shouldInspectSni()) {
                    sniChecked = true
                    val sni = extractSni(payload)
                    if (sni != null && isDomainBlocked(sni)) {
                        Log.v(TAG, "SNI block: $sni")
                        sendRstToClient()
                        close()
                        return
                    }
                }

                localAck = (localAck + payloadLen) and 0xFFFFFFFFL
                sendTcpPacket(flags = TCP_ACK, data = ByteBuffer.allocate(0))

                if (state == State.ESTABLISHED) {
                    try {
                        serverChannel?.write(ByteBuffer.wrap(payload))
                    } catch (e: Exception) {
                        Log.w(TAG, "Server write failed", e)
                        close()
                    }
                } else {
                    pendingPayloads.add(payload)
                }
            }
        }

        private fun startServerRelay() {
            relayJob = scope.launch(Dispatchers.IO) {
                val ch = serverChannel ?: return@launch
                val readBuf = ByteBuffer.allocate(8192)
                try {
                    while (isActive && state == State.ESTABLISHED) {
                        readBuf.clear()
                        val read = ch.read(readBuf)
                        if (read < 0) {
                            close()
                            break
                        }
                        if (read == 0) continue
                        readBuf.flip()
                        sendTcpPacket(flags = TCP_PSH or TCP_ACK, data = readBuf)
                        localSeq = (localSeq + read) and 0xFFFFFFFFL
                    }
                } catch (e: Exception) {
                    if (isActive) Log.w(TAG, "Server relay error", e)
                    close()
                }
            }
        }

        private fun sendTcpPacket(flags: Int, data: ByteBuffer) {
            val ipLen = 20
            val tcpLen = 20 + data.remaining()
            val totalLen = ipLen + tcpLen
            val pkt = ByteBuffer.allocate(totalLen)

            pkt.put(0x45)           // Version 4, IHL 5
            pkt.put(0x00)           // DSCP/ECN
            pkt.putShort(totalLen.toShort())
            pkt.putShort((ipId++ and 0xFFFF).toShort())
            pkt.putShort(0x4000.toShort())  // Flags + offset
            pkt.put(64.toByte())    // TTL
            pkt.put(6.toByte())     // Protocol = TCP
            pkt.putShort(0)         // Checksum placeholder
            pkt.put(clientDstIp)    // Source = original destination
            pkt.put(clientSrcIp)    // Dest = original source

            pkt.putShort(clientDstPort.toShort())
            pkt.putShort(clientSrcPort.toShort())
            pkt.putInt(localSeq.toInt())
            pkt.putInt(localAck.toInt())
            pkt.put((5 shl 4).toByte())
            pkt.put(flags.toByte())
            pkt.putShort(65535.toShort())  // Window size
            pkt.putShort(0)
            pkt.putShort(0)

            pkt.put(data)

            val ipChecksum = calculateChecksum(pkt, 0, ipLen)
            pkt.putShort(10, ipChecksum.toShort())
            val tcpChecksum = calculatePseudoChecksum(pkt, clientDstIp, clientSrcIp, 6, tcpLen)
            pkt.putShort(ipLen + 16, tcpChecksum.toShort())

            pkt.flip()
            synchronized(writeLock) {
                try { tunOutput.write(pkt) } catch (_: Exception) {}
            }
        }

        private fun sendRstToClient() {
            val rst = ByteBuffer.allocate(40)
            rst.put(0x45); rst.put(0x00); rst.putShort(40)
            rst.putShort(0); rst.putShort(0x4000.toShort())
            rst.put(64.toByte()); rst.put(6.toByte()); rst.putShort(0)
            rst.put(clientDstIp); rst.put(clientSrcIp)
            rst.putShort(clientDstPort.toShort()); rst.putShort(clientSrcPort.toShort())
            rst.putInt(0); rst.putInt(0)
            rst.put((5 shl 4).toByte()); rst.put(TCP_RST.toByte())
            rst.putShort(0); rst.putShort(0); rst.putShort(0)
            val ipCsum = calculateChecksum(rst, 0, 20)
            rst.putShort(10, ipCsum.toShort())
            val tcpCsum = calculatePseudoChecksum(rst, clientDstIp, clientSrcIp, 6, 20)
            rst.putShort(36, tcpCsum.toShort())
            rst.flip()
            synchronized(writeLock) { try { tunOutput.write(rst) } catch (_: Exception) {} }
        }

        fun close() {
            if (state == State.CLOSED) return
            state = State.CLOSED
            relayJob?.cancel()
            relayJob = null
            try { serverChannel?.close() } catch (_: Exception) {}
            serverChannel = null
        }

        private fun extractSni(payload: ByteArray): String? {
            if (payload.size < 6) return null
            val contentType = payload[0].toInt() and 0xFF
            if (contentType != TLS_HANDSHAKE) return null
            val handshakeType = payload[5].toInt() and 0xFF
            if (handshakeType != 1) return null // TLS_CLIENT_HELLO
            try {
                var pos = 9
                pos += 2 // version
                pos += 32 // random
                val sessionIdLen = payload[pos].toInt() and 0xFF
                pos += 1 + sessionIdLen
                if (pos + 2 > payload.size) return null
                val cipherSuitesLen = ((payload[pos].toInt() and 0xFF) shl 8) or (payload[pos + 1].toInt() and 0xFF)
                pos += 2 + cipherSuitesLen
                if (pos + 1 > payload.size) return null
                val compressionLen = payload[pos].toInt() and 0xFF
                pos += 1 + compressionLen
                if (pos + 2 > payload.size) return null
                val extensionsLen = ((payload[pos].toInt() and 0xFF) shl 8) or (payload[pos + 1].toInt() and 0xFF)
                pos += 2
                val extensionsEnd = pos + extensionsLen
                while (pos + 4 <= extensionsEnd && pos + 4 <= payload.size) {
                    val extType = ((payload[pos].toInt() and 0xFF) shl 8) or (payload[pos + 1].toInt() and 0xFF)
                    val extLen = ((payload[pos + 2].toInt() and 0xFF) shl 8) or (payload[pos + 3].toInt() and 0xFF)
                    pos += 4
                    if (extType == 0x0000) { // SNI
                        if (pos + 2 <= payload.size) {
                            val sniListLen = ((payload[pos].toInt() and 0xFF) shl 8) or (payload[pos + 1].toInt() and 0xFF)
                            var sniPos = pos + 2
                            val sniListEnd = sniPos + sniListLen
                            while (sniPos + 3 <= sniListEnd && sniPos + 3 <= payload.size) {
                                val nameType = payload[sniPos].toInt() and 0xFF
                                val nameLen = ((payload[sniPos + 1].toInt() and 0xFF) shl 8) or (payload[sniPos + 2].toInt() and 0xFF)
                                sniPos += 3
                                if (nameType == 0 && sniPos + nameLen <= payload.size) {
                                    return String(payload, sniPos, nameLen, Charsets.UTF_8)
                                }
                                sniPos += nameLen
                            }
                        }
                        return null
                    }
                    pos += extLen
                }
            } catch (_: Exception) {}
            return null
        }
    }
}
