package com.nexusblock.engine

import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import com.nexusblock.Constants
import com.nexusblock.data.repository.VpnRoutingMode
import com.nexusblock.data.repository.StatsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Semaphore
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
    private val udpRelay: UdpRelayEngine
) {
    companion object {
        private const val TAG = "NexusBlock/Router"
        private const val BUFFER_SIZE = 32768
        private const val PROTO_ICMP = 1
        private const val PROTO_TCP = 6
        private const val PROTO_UDP = 17
        private const val MAX_CONCURRENT_DNS_QUERIES = 64

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
    private var routingMode = VpnRoutingMode.FULL_ROUTE_AGGRESSIVE
    private val dnsQueryLimiter = Semaphore(MAX_CONCURRENT_DNS_QUERIES)
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
        tcpRelay.blockUnknownCloudFrontDistributions = settingsRepo.vpnRoutingMode == VpnRoutingMode.FULL_ROUTE_AGGRESSIVE
        tcpRelay.start(vpnService, outputChannel)
        udpRelay.start(vpnService, outputChannel)

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
                tcpRelay.blockUnknownCloudFrontDistributions = it == VpnRoutingMode.FULL_ROUTE_AGGRESSIVE
                Log.d(TAG, "Routing mode updated: $routingMode")
            }
        }

        // Single consumer drains the blocked-log channel and forwards to
        // StatsRepository. This replaces a `scope.launch { ... }` per blocked
        // packet which was hammering the IO dispatcher under load.
        blockedLogJob?.cancel()
        blockedLogJob = scope.launch {
            for ((target, type) in blockedLogChannel) {
                try { statsRepo.logBlocked(target, type = type) }
                catch (e: Exception) { Log.w(TAG, "logBlocked failed", e) }
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
        Log.i(TAG, "Packet router stopped")
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
        val ipHeaderLen = (buffer.get(pos).toInt() and 0x0F) * 4
        val totalLen = buffer.getShort(pos + 2).toInt() and 0xFFFF
        val protocol = buffer.get(pos + 9).toInt() and 0xFF

        if (buffer.remaining() < ipHeaderLen) return

        // Read common IPs early for blocked-app checks
        val srcIp = readIpv4(buffer, pos + 12)
        val dstIp = readIpv4(buffer, pos + 16)
        val dstIpStr = "${dstIp[0].toInt() and 0xFF}.${dstIp[1].toInt() and 0xFF}.${dstIp[2].toInt() and 0xFF}.${dstIp[3].toInt() and 0xFF}"

        // 1. Stealth Mode (Block ICMP)
        if (techniques.stealthMode && protocol == PROTO_ICMP) {
            packetsBlocked++
            return
        }

        // 2. IP Filtering (Fast check) — applies to all protocols
        if (techniques.ipBlocking) {
            if (dnsEngine.isIpBlocked(dstIpStr)) {
                packetsBlocked++
                if (protocol == PROTO_TCP) {
                    val tcpOffset = pos + ipHeaderLen
                    if (buffer.remaining() >= tcpOffset + 4) {
                        val srcPort = buffer.getShort(tcpOffset).toInt() and 0xFFFF
                        val dstPort = buffer.getShort(tcpOffset + 2).toInt() and 0xFFFF
                        sendRst(output, pos, ipHeaderLen, srcIp, dstIp, srcPort, dstPort)
                    }
                }
                // For UDP (including QUIC/443) just drop silently
                return
            }
        }

        when (protocol) {
            PROTO_UDP -> {
                val udpOffset = pos + ipHeaderLen
                val srcPort = if (buffer.remaining() >= udpOffset + 8) buffer.getShort(udpOffset).toInt() and 0xFFFF else 0
                val dstPort = if (buffer.remaining() >= udpOffset + 8) buffer.getShort(udpOffset + 2).toInt() and 0xFFFF else 0
                if (buffer.remaining() >= udpOffset + 8) {
                    if (isDnsBypassIp(dstIpStr) && dstPort != 53 && isDnsBypassPort(dstPort)) {
                        packetsBlocked++
                        blockedLogChannel.trySend("$dstIpStr:$dstPort" to "dns-bypass")
                        return
                    }

                    // QUIC downgrade: block UDP/443 to ALL CloudFront IPs (both
                    // CIDR-matched and DNS-observed) so apps fall back to TCP/443
                    // where TLS SNI inspection gives us visibility to block
                    // ad-serving CloudFront distributions.
                    if (dstPort == 443 && techniques.dnsFiltering &&
                        routingMode == VpnRoutingMode.FULL_ROUTE_AGGRESSIVE &&
                        (dnsEngine.shouldForceDowngradeQuic(dstIpStr) || CloudFrontCidr.isCloudFrontIp(dstIpStr))) {
                        packetsBlocked++
                        blockedLogChannel.trySend("$dstIpStr:443" to "quic-downgrade")
                        return
                    }

                    // DNS interception
                    if (dstPort == 53 && techniques.dnsFiltering) {
                        val udpLen = (buffer.getShort(udpOffset + 4).toInt() and 0xFFFF)
                        val payloadLen = udpLen - 8
                        if (payloadLen > 0 && buffer.remaining() >= udpOffset + 8 + payloadLen) {
                            val queryPayload = ByteArray(payloadLen)
                            val mark = buffer.position()
                            buffer.position(udpOffset + 8)
                            buffer.get(queryPayload)
                            buffer.position(mark)

                            if (!dnsQueryLimiter.tryAcquire()) {
                                packetsBlocked++
                                blockedLogChannel.trySend("$dstIpStr:53" to "dns-overload")
                                return
                            }

                            scope.launch {
                                try {
                                    val responsePayload = dnsEngine.resolveDns(queryPayload)
                                    if (responsePayload != null) {
                                        sendDnsResponse(output, srcIp, dstIp, srcPort, dstPort, responsePayload)
                                    }
                                } catch (e: Exception) {
                                    if (isRunning) Log.w(TAG, "DNS resolution error", e)
                                } finally {
                                    dnsQueryLimiter.release()
                                }
                            }
                            return // drop original, async response will be sent
                        }
                    }

                    // Non-DNS UDP: relay through protected socket
                    val udpLen = (buffer.getShort(udpOffset + 4).toInt() and 0xFFFF)
                    val payloadLen = udpLen - 8
                    if (payloadLen > 0 && buffer.remaining() >= udpOffset + 8 + payloadLen) {
                        val payload = ByteArray(payloadLen)
                        val mark = buffer.position()
                        buffer.position(udpOffset + 8)
                        buffer.get(payload)
                        buffer.position(mark)
                        udpRelay.handlePacket(srcIp, dstIp, srcPort, dstPort, payload)
                    }
                }
            }
            PROTO_TCP -> {
                val tcpOffset = pos + ipHeaderLen
                if (buffer.remaining() < tcpOffset + 20) return

                val srcPort = buffer.getShort(tcpOffset).toInt() and 0xFFFF
                val dstPort = buffer.getShort(tcpOffset + 2).toInt() and 0xFFFF

                if (isDnsBypassIp(dstIpStr) && isDnsBypassPort(dstPort)) {
                    sendRst(output, pos, ipHeaderLen, srcIp, dstIp, srcPort, dstPort)
                    packetsBlocked++
                    blockedLogChannel.trySend("$dstIpStr:$dstPort" to "dns-bypass")
                    return
                }

                // Delegate ALL TCP to the relay engine (handles SNI blocking internally)
                tcpRelay.handlePacket(
                    buffer, pos, ipHeaderLen, srcIp, dstIp,
                    srcPort, dstPort, tcpOffset, totalLen
                )
            }
            else -> writePacket(output, buffer.duplicate())
        }
    }

    private fun isDnsBypassIp(ip: String): Boolean = ip in Constants.DNS_BYPASS_IPV4_ROUTES

    private fun isDnsBypassPort(port: Int): Boolean = port in Constants.DNS_BYPASS_PORTS

    private fun processIPv6(buffer: ByteBuffer, output: FileChannel) {
        if (buffer.remaining() < 40) {
            packetsBlocked++
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

                    // Extract IP/port before launching coroutine to avoid race on shared ByteBuffer
                    val srcIp = ByteArray(16)
                    val dstIp = ByteArray(16)
                    buffer.position(pos + 8)
                    buffer.get(srcIp)
                    buffer.get(dstIp)
                    val srcPort = buffer.getShort(udpOffset).toInt() and 0xFFFF

                    if (!dnsQueryLimiter.tryAcquire()) {
                        packetsBlocked++
                        blockedLogChannel.trySend("ipv6-dns" to "dns-overload")
                        return
                    }

                    scope.launch {
                        try {
                            val responsePayload = dnsEngine.resolveDns(queryPayload)
                            if (responsePayload != null) {
                                sendDnsResponseV6(output, srcIp, dstIp, srcPort, dstPort, responsePayload)
                            }
                        } catch (e: Exception) {
                            if (isRunning) Log.w(TAG, "IPv6 DNS resolution error", e)
                        } finally {
                            dnsQueryLimiter.release()
                        }
                    }
                    return // DNS handled asynchronously; don't count as blocked
                }
            }
        }
        // Silently drop all non-DNS IPv6 traffic to prevent leaks outside the tunnel
        packetsBlocked++
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
        } catch (_: Exception) {}
    }

    data class RouterStats(
        val packetsProcessed: Long,
        val packetsBlocked: Long,
        val bytesTotal: Long
    )
}
