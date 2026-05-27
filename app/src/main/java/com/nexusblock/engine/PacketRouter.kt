package com.nexusblock.engine

import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import com.nexusblock.Constants
import com.nexusblock.data.repository.VpnRoutingMode
import com.nexusblock.data.repository.StatsRepository
import com.nexusblock.engine.network.FlowCache
import com.nexusblock.engine.network.PacketParser
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PacketRouter @Inject constructor(
    private val dnsEngine: DnsFilterEngine,
    private val statsRepo: StatsRepository,
    private val settingsRepo: com.nexusblock.data.repository.SettingsRepository,
    private val tcpRelay: TcpRelayEngine,
    private val udpRelay: UdpRelayEngine,
    private val flowCache: FlowCache
) {
    companion object {
        private const val TAG = "Argus/Router"
        private const val BUFFER_SIZE = 32768
        private const val PROTO_ICMP = 1
        private const val PROTO_TCP = 6
        private const val PROTO_UDP = 17

        private const val TCP_RST = 0x04
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var routerJob: Job? = null
    private var techniquesJob: Job? = null
    private var routingModeJob: Job? = null
    private var blockedLogJob: Job? = null
    // Bounded channel for blocked-event logging. Dropping the oldest entry
    // under burst is fine — diagnostics should never throttle the data path.
    private val blockedLogChannel = Channel<Pair<String, String>>(
        capacity = 256,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    private var techniques = com.nexusblock.data.repository.BlockingTechniques()
    private var routingMode = VpnRoutingMode.DNS_ONLY
    private val outputLock = Any()

    @Volatile
    var isRunning = false
        private set

    // Diagnostics counters
    var packetsProcessed = 0L
    var packetsBlocked = 0L
    var bytesTotal = 0L

    fun start(
        vpnService: VpnService,
        tunFd: ParcelFileDescriptor,
        dnsAddress: InetAddress
    ) {
        if (isRunning) return
        isRunning = true

        dnsEngine.start(dnsAddress)

        val outputChannel = FileOutputStream(tunFd.fileDescriptor).channel
        tcpRelay.blockedCallback = { target, type ->
            packetsBlocked++
            blockedLogChannel.trySend(target to type)
        }
        tcpRelay.blockUnknownCloudFrontDistributions = settingsRepo.vpnRoutingMode == VpnRoutingMode.FULL_ROUTE
        if (settingsRepo.vpnRoutingMode == VpnRoutingMode.FULL_ROUTE) {
            tcpRelay.start(vpnService, outputChannel)
            udpRelay.start(vpnService, outputChannel)
        }

        techniquesJob?.cancel()
        techniquesJob = scope.launch {
            settingsRepo.observeTechniques().collect {
                techniques = it
                Log.d(TAG, "Techniques updated: $techniques")
            }
        }

        routingModeJob?.cancel()
        routingModeJob = scope.launch {
            settingsRepo.observeVpnRoutingMode().collect {
                routingMode = it
                tcpRelay.blockUnknownCloudFrontDistributions = it == VpnRoutingMode.FULL_ROUTE
                Log.d(TAG, "Routing mode updated: $routingMode")
            }
        }

        // Single consumer drains the blocked-log channel and forwards to
        // StatsRepository. This replaces a `scope.launch { ... }` per blocked
        // packet which was hammering the IO dispatcher under load.
        blockedLogJob?.cancel()
        blockedLogJob = scope.launch {
            val batch = ArrayList<com.nexusblock.data.model.BlockedEvent>(100)
            while (isActive) {
                val first = withTimeoutOrNull(5000) { blockedLogChannel.receive() }
                if (first == null) {
                    if (batch.isNotEmpty()) {
                        try { statsRepo.logBlockedBatch(batch.toList()) } catch (_: Exception) {}
                        batch.clear()
                    }
                    continue
                }
                batch.add(com.nexusblock.data.model.BlockedEvent(host = first.first, type = first.second))
                while (batch.size < 100) {
                    val next = blockedLogChannel.tryReceive().getOrNull() ?: break
                    batch.add(com.nexusblock.data.model.BlockedEvent(host = next.first, type = next.second))
                }
                try { statsRepo.logBlockedBatch(batch.toList()) } catch (_: Exception) {}
                batch.clear()
            }
        }

        routerJob = scope.launch {
            val inputStream = FileInputStream(tunFd.fileDescriptor)
            val output = outputChannel
            val rawBuffer = ByteArray(BUFFER_SIZE)

            // Use blocking FileInputStream.read() on this dedicated coroutine.
            // Unlike FileChannel.read(ByteBuffer) which returns 0 immediately on
            // an empty TUN (forcing a delay(5) polling loop that burns CPU at
            // ~200 wakeups/sec), FileInputStream.read() blocks in the kernel until
            // a packet arrives — zero CPU cost while idle. This is the single
            // biggest performance improvement for low-end Android TV hardware.
            while (isActive && isRunning) {
                try {
                    val read = inputStream.read(rawBuffer)
                    if (read <= 0) continue
                    packetsProcessed++
                    bytesTotal += read
                    val buffer = ByteBuffer.wrap(rawBuffer, 0, read)
                    processPacket(buffer, output, vpnService)
                } catch (e: Exception) {
                    if (isActive && isRunning) Log.w(TAG, "Packet routing error", e)
                }
            }
        }
        Log.i(TAG, "Packet router started")
    }

    fun stop() {
        isRunning = false
        routerJob?.cancel()
        routerJob = null
        techniquesJob?.cancel()
        techniquesJob = null
        routingModeJob?.cancel()
        routingModeJob = null
        blockedLogJob?.cancel()
        blockedLogJob = null
        tcpRelay.blockedCallback = null
        tcpRelay.stop()
        udpRelay.stop()
        dnsEngine.stop()
        flowCache.clear()
        Log.i(TAG, "Packet router stopped")
    }

    /** Called by [NexusVpnService.onTrimMemory] when Android TV signals low RAM. */
    fun trimMemory() {
        flowCache.trimMemory()
        Log.d(TAG, "Flow cache trimmed, size=${flowCache.size()}")
    }

    fun getRouterStats(): RouterStats = RouterStats(
        packetsProcessed = packetsProcessed,
        packetsBlocked = packetsBlocked,
        bytesTotal = bytesTotal
    )

    private fun processPacket(buffer: ByteBuffer, output: FileChannel, vpnService: VpnService) {
        if (buffer.remaining() < 20) return
        val version = buffer.get(buffer.position()).toInt() shr 4 and 0x0F
        if (version == 4) {
            processIPv4(buffer, output)
        } else if (version == 6) {
            processIPv6(buffer, output)
        }
    }

    private fun processIPv4(buffer: ByteBuffer, output: FileChannel) {
        val pos = buffer.position()
        val ipHeaderLen = PacketParser.ipHeaderLength(buffer, pos)
        val totalLen = PacketParser.totalLength(buffer, pos)
        val protocol = PacketParser.protocol(buffer, pos)

        if (buffer.remaining() < ipHeaderLen) return

        val srcIp = PacketParser.srcIpBytes(buffer, pos)
        val dstIp = PacketParser.dstIpBytes(buffer, pos)
        val dstIpStr = PacketParser.formatIpv4(dstIp)
        val srcIpInt = PacketParser.srcIpInt(buffer, pos)
        val dstIpInt = PacketParser.dstIpInt(buffer, pos)

        // 1. Stealth Mode (Block ICMP)
        if (techniques.stealthMode && protocol == PacketParser.PROTO_ICMP) {
            packetsBlocked++
            return
        }

        // 2. IP Filtering (Fast check) — check flow cache first, then rule engine
        if (techniques.ipBlocking) {
            val tcpUdpOffset = pos + ipHeaderLen
            val sp = if (buffer.remaining() >= tcpUdpOffset + 2)
                PacketParser.srcPort(buffer, tcpUdpOffset) else 0
            val dp = if (buffer.remaining() >= tcpUdpOffset + 2)
                PacketParser.dstPort(buffer, tcpUdpOffset) else 0
            val flowKey = flowCache.key(srcIpInt, dstIpInt, sp, dp)

            val cached = flowCache.get(flowKey)
            if (cached == FlowCache.Verdict.DROP) {
                packetsBlocked++
                if (protocol == PacketParser.PROTO_TCP && buffer.remaining() >= tcpUdpOffset + 4) {
                    sendRst(output, pos, ipHeaderLen, srcIp, dstIp, sp, dp)
                }
                return
            }

            if (dnsEngine.isIpBlocked(dstIpStr)) {
                packetsBlocked++
                flowCache.put(flowKey, FlowCache.Verdict.DROP)
                if (protocol == PacketParser.PROTO_TCP && buffer.remaining() >= tcpUdpOffset + 4) {
                    sendRst(output, pos, ipHeaderLen, srcIp, dstIp, sp, dp)
                }
                return
            }
        }

        when (protocol) {
            PacketParser.PROTO_UDP -> handleUdp(buffer, pos, ipHeaderLen, srcIp, dstIp, srcIpInt, dstIpInt, dstIpStr, output)
            PacketParser.PROTO_TCP -> handleTcp(buffer, pos, ipHeaderLen, srcIp, dstIp, dstIpStr, totalLen, output)
            else -> writePacket(output, buffer.duplicate())
        }
    }

    private fun handleUdp(
        buffer: ByteBuffer, pos: Int, ipHeaderLen: Int,
        srcIp: ByteArray, dstIp: ByteArray,
        srcIpInt: Int, dstIpInt: Int, dstIpStr: String,
        output: FileChannel
    ) {
        val udpOffset = pos + ipHeaderLen
        if (buffer.remaining() < udpOffset + 8) return

        val srcPort = PacketParser.srcPort(buffer, udpOffset)
        val dstPort = PacketParser.dstPort(buffer, udpOffset)
        val flowKey = flowCache.key(srcIpInt, dstIpInt, srcPort, dstPort)

        // Check flow cache for QUIC downgrade verdict
        flowCache.get(flowKey)?.let {
            if (it == FlowCache.Verdict.DROP) {
                packetsBlocked++
                return
            }
        }

        if (isDnsBypassIp(dstIpStr) && dstPort != 53 && isDnsBypassPort(dstPort)) {
            packetsBlocked++
            blockedLogChannel.trySend("$dstIpStr:$dstPort" to "dns-bypass")
            flowCache.put(flowKey, FlowCache.Verdict.DROP)
            return
        }

        // QUIC downgrade: block UDP/443 to CloudFront IPs
        if (dstPort == 443 && techniques.dnsFiltering &&
            routingMode == VpnRoutingMode.FULL_ROUTE &&
            (dnsEngine.shouldForceDowngradeQuic(dstIpStr) || CloudFrontCidr.isCloudFrontIp(dstIpStr))) {
            packetsBlocked++
            blockedLogChannel.trySend("$dstIpStr:443" to "quic-downgrade")
            flowCache.put(flowKey, FlowCache.Verdict.DROP)
            return
        }

        // DNS interception
        if (dstPort == 53 && techniques.dnsFiltering) {
            val payloadLen = PacketParser.udpLength(buffer, udpOffset) - 8
            if (payloadLen > 0 && buffer.remaining() >= udpOffset + 8 + payloadLen) {
                val queryPayload = ByteArray(payloadLen)
                val mark = buffer.position()
                buffer.position(udpOffset + 8)
                buffer.get(queryPayload)
                buffer.position(mark)

                scope.launch {
                    val responsePayload = try {
                        dnsEngine.resolveDns(queryPayload)
                    } catch (e: Exception) {
                        if (isRunning) Log.w(TAG, "DNS resolution error", e)
                        null
                    }
                    if (responsePayload != null && isRunning) {
                        try {
                            sendDnsResponse(output, srcIp, dstIp, srcPort, dstPort, responsePayload)
                        } catch (e: Exception) {
                            if (isRunning) Log.w(TAG, "DNS response injection error", e)
                        }
                    }
                }
                return
            }
        }

        if (routingMode == VpnRoutingMode.DNS_ONLY) return

        // Non-DNS UDP: relay through protected socket
        val payloadLen = PacketParser.udpLength(buffer, udpOffset) - 8
        if (payloadLen > 0 && buffer.remaining() >= udpOffset + 8 + payloadLen) {
            val payload = ByteArray(payloadLen)
            val mark = buffer.position()
            buffer.position(udpOffset + 8)
            buffer.get(payload)
            buffer.position(mark)
            udpRelay.handlePacket(srcIp, dstIp, srcPort, dstPort, payload)
        }
    }

    private fun handleTcp(
        buffer: ByteBuffer, pos: Int, ipHeaderLen: Int,
        srcIp: ByteArray, dstIp: ByteArray, dstIpStr: String,
        totalLen: Int, output: FileChannel
    ) {
        val tcpOffset = pos + ipHeaderLen
        if (buffer.remaining() < tcpOffset + 20) return

        val srcPort = PacketParser.srcPort(buffer, tcpOffset)
        val dstPort = PacketParser.dstPort(buffer, tcpOffset)

        if (isDnsBypassIp(dstIpStr) && isDnsBypassPort(dstPort)) {
            sendRst(output, pos, ipHeaderLen, srcIp, dstIp, srcPort, dstPort)
            packetsBlocked++
            blockedLogChannel.trySend("$dstIpStr:$dstPort" to "dns-bypass")
            return
        }

        if (routingMode == VpnRoutingMode.DNS_ONLY) {
            writePacket(output, buffer.duplicate())
            return
        }

        tcpRelay.handlePacket(buffer, pos, ipHeaderLen, srcIp, dstIp, srcPort, dstPort, tcpOffset, totalLen)
    }

    private fun isDnsBypassIp(ip: String): Boolean = ip in Constants.DNS_BYPASS_IPV4_ROUTES

    private fun isDnsBypassPort(port: Int): Boolean = port in Constants.DNS_BYPASS_PORTS

    private fun processIPv6(buffer: ByteBuffer, output: FileChannel) {
        if (buffer.remaining() < 40) {
            // Malformed/short IPv6 packet — drop silently without counting as blocked
            return
        }
        val pos = buffer.position()
        val nextHeader = buffer.get(pos + 6).toInt() and 0xFF

        if (nextHeader == PROTO_UDP && buffer.remaining() >= 48) {
            val udpOffset = pos + 40
            val dstPort = buffer.getShort(udpOffset + 2).toInt() and 0xFFFF
            if (dstPort == 53 && techniques.dnsFiltering) {
                val udpLen = (buffer.getShort(udpOffset + 4).toInt() and 0xFFFF)
                val payloadLenUdp = udpLen - 8
                if (payloadLenUdp > 0 && buffer.remaining() >= udpOffset + 8 + payloadLenUdp) {
                    val queryPayload = ByteArray(payloadLenUdp)
                    buffer.position(udpOffset + 8)
                    buffer.get(queryPayload)

                    // Extract IP/port for response construction
                    val srcIp = ByteArray(16)
                    val dstIp = ByteArray(16)
                    buffer.position(pos + 8)
                    buffer.get(srcIp)
                    buffer.get(dstIp)
                    val srcPort = buffer.getShort(udpOffset).toInt() and 0xFFFF

                    scope.launch {
                        val responsePayload = try {
                            dnsEngine.resolveDns(queryPayload)
                        } catch (e: Exception) {
                            if (isRunning) Log.w(TAG, "IPv6 DNS resolution error", e)
                            null
                        }
                        if (responsePayload != null && isRunning) {
                            try {
                                sendDnsResponseV6(output, srcIp, dstIp, srcPort, dstPort, responsePayload)
                            } catch (e: Exception) {
                                if (isRunning) Log.w(TAG, "IPv6 DNS response injection error", e)
                            }
                        }
                    }
                    return // DNS handled; don't count as blocked
                }
            }
        }
        // In DNS-only mode IPv6 shouldn't be routed into TUN at all.
        // If it appears here (edge case), pass through to avoid stalls.
        writePacket(output, buffer.duplicate())
    }

    private fun readIpv4(buffer: ByteBuffer, offset: Int): ByteArray {
        return byteArrayOf(buffer.get(offset), buffer.get(offset + 1), buffer.get(offset + 2), buffer.get(offset + 3))
    }

    private fun sendRst(
        output: FileChannel,
        ipPos: Int,
        ipHeaderLen: Int,
        srcIp: ByteArray,
        dstIp: ByteArray,
        srcPort: Int,
        dstPort: Int
    ) {
        val rst = ByteBuffer.allocate(40)

        // IP header
        rst.put(0x45)
        rst.put(0x00)
        rst.putShort(40)
        rst.putShort(0)
        rst.putShort(0x4000.toShort())
        rst.put(64.toByte())
        rst.put(PROTO_TCP.toByte())
        rst.putShort(0)
        rst.put(dstIp)
        rst.put(srcIp)

        // TCP header
        rst.putShort(dstPort.toShort())
        rst.putShort(srcPort.toShort())
        rst.putInt(0)
        rst.putInt(0)
        rst.put((5 shl 4).toByte())
        rst.put(TCP_RST.toByte())
        rst.putShort(0)
        rst.putShort(0)
        rst.putShort(0)

        // Fix checksums
        val ipChecksum = calculateChecksum(rst, 0, 20)
        rst.putShort(10, ipChecksum.toShort())
        val tcpChecksum = calculatePseudoChecksum(rst, dstIp, srcIp, PROTO_TCP, 20)
        rst.putShort(36, tcpChecksum.toShort())

        rst.flip()
        writePacket(output, rst)
    }

    private fun sendDnsResponse(
        output: FileChannel,
        srcIp: ByteArray,
        dstIp: ByteArray,
        srcPort: Int,
        dstPort: Int,
        responsePayload: ByteArray
    ) {
        val ipHeaderLen = 20
        val udpLen = 8 + responsePayload.size
        val totalLen = ipHeaderLen + udpLen
        val pkt = ByteBuffer.allocate(totalLen)

        // IP header
        pkt.put(0x45) // Version 4, IHL 5
        pkt.put(0x00) // DSCP/ECN
        pkt.putShort(totalLen.toShort()) // Total length
        pkt.putShort(0) // Identification
        pkt.putShort(0x4000.toShort()) // Flags + Fragment offset
        pkt.put(64.toByte()) // TTL
        pkt.put(PROTO_UDP.toByte()) // Protocol
        pkt.putShort(0) // Header checksum placeholder
        pkt.put(dstIp) // Swap: destination = original source
        pkt.put(srcIp) // Swap: source = original destination

        // UDP header: swap ports
        pkt.putShort(dstPort.toShort()) // Source port = original destination port (53)
        pkt.putShort(srcPort.toShort()) // Destination port = original source port
        pkt.putShort(udpLen.toShort()) // UDP length
        pkt.putShort(0) // UDP checksum = 0 (optional in IPv4)

        // DNS payload
        pkt.put(responsePayload)

        // Fix IP checksum
        val ipChecksum = calculateChecksum(pkt, 0, ipHeaderLen)
        pkt.putShort(10, ipChecksum.toShort())

        pkt.flip()
        writePacket(output, pkt)
    }

    private fun sendDnsResponseV6(
        output: FileChannel,
        srcIp: ByteArray,
        dstIp: ByteArray,
        srcPort: Int,
        dstPort: Int,
        responsePayload: ByteArray
    ) {
        val udpLen = 8 + responsePayload.size
        val ip6HeaderLen = 40
        val totalLen = ip6HeaderLen + udpLen
        val pkt = ByteBuffer.allocate(totalLen)

        // IPv6 header
        pkt.putInt(0x60000000) // Version 6, TC 0, Flow Label 0
        pkt.putShort(udpLen.toShort()) // Payload Length
        pkt.put(PROTO_UDP.toByte()) // Next Header = UDP
        pkt.put(64.toByte()) // Hop Limit
        pkt.put(dstIp) // Source = original destination (our TUN DNS)
        pkt.put(srcIp) // Destination = original source (app)

        // UDP header
        pkt.putShort(dstPort.toShort()) // Source port = 53
        pkt.putShort(srcPort.toShort()) // Destination port = original source
        pkt.putShort(udpLen.toShort()) // UDP length
        pkt.putShort(0) // Checksum placeholder

        // DNS payload
        pkt.put(responsePayload)

        // Compute IPv6 UDP checksum
        val pseudoHeader = ByteBuffer.allocate(40)
        pseudoHeader.put(dstIp) // Source = our address
        pseudoHeader.put(srcIp) // Destination = app address
        pseudoHeader.putInt(udpLen)
        pseudoHeader.put(0)
        pseudoHeader.put(0)
        pseudoHeader.put(0)
        pseudoHeader.put(PROTO_UDP.toByte())
        pseudoHeader.flip()

        val udpOffsetInPkt = ip6HeaderLen
        val checksumBuf = ByteBuffer.allocate(40 + udpLen)
        checksumBuf.put(pseudoHeader)
        pkt.position(udpOffsetInPkt)
        val udpBytes = ByteArray(udpLen)
        pkt.get(udpBytes)
        checksumBuf.put(udpBytes)
        checksumBuf.flip()

        val udpChecksum = calculateChecksum(checksumBuf, 0, checksumBuf.remaining())
        pkt.putShort(udpOffsetInPkt + 6, udpChecksum.toShort())

        pkt.flip()
        writePacket(output, pkt)
    }

    private fun writePacket(output: FileChannel, packet: ByteBuffer) {
        try {
            synchronized(outputLock) {
                output.write(packet)
            }
        } catch (e: java.nio.channels.ClosedChannelException) {
            Log.w(TAG, "TUN channel closed, triggering router stop")
            isRunning = false
        } catch (_: Exception) {}
    }

    data class RouterStats(
        val packetsProcessed: Long,
        val packetsBlocked: Long,
        val bytesTotal: Long
    )
}
