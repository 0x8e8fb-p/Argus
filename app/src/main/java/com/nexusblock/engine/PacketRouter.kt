package com.nexusblock.engine

import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import com.nexusblock.Constants
import com.nexusblock.data.repository.StatsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
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
    private val connectionTracker: ConnectionTracker,
    private val settingsRepo: com.nexusblock.data.repository.SettingsRepository
) {
    companion object {
        private const val TAG = "NexusBlock/Router"
        private const val BUFFER_SIZE = 32768

        private const val PROTO_ICMP = 1
        private const val PROTO_TCP = 6
        private const val PROTO_UDP = 17

        private const val TCP_RST = 0x04

        private const val TLS_HANDSHAKE = 22
        private const val TLS_CLIENT_HELLO = 1
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var routerJob: Job? = null
    private var techniquesJob: Job? = null
    private var techniques = com.nexusblock.data.repository.BlockingTechniques()
    private val outputLock = Any()

    @Volatile
    var isRunning = false
        private set

    // Diagnostics counters
    var packetsProcessed = 0L
    var packetsBlocked = 0L
    var sniBlocks = 0L
    var dnsBlocks = 0L
    var bytesTotal = 0L
    private var lastStatsUpdate = 0L

    fun start(
        vpnService: VpnService,
        tunFd: ParcelFileDescriptor,
        dnsAddress: InetAddress
    ) {
        if (isRunning) return
        isRunning = true

        dnsEngine.start(dnsAddress)
        connectionTracker.start()

        techniquesJob?.cancel()
        techniquesJob = scope.launch {
            settingsRepo.observeTechniques().collect {
                techniques = it
                Log.d(TAG, "Techniques updated: $techniques")
            }
        }

        routerJob = scope.launch {
            val input = FileInputStream(tunFd.fileDescriptor).channel
            val output = FileOutputStream(tunFd.fileDescriptor).channel
            val buffer = ByteBuffer.allocateDirect(BUFFER_SIZE)

            while (isActive && isRunning) {
                try {
                    buffer.clear()
                    val read = input.read(buffer)
                    if (read <= 0) {
                        delay(5)
                        continue
                    }
                    packetsProcessed++
                    bytesTotal += read
                    buffer.flip()
                    processPacket(buffer, output, vpnService)
                } catch (e: Exception) {
                    if (isActive) Log.w(TAG, "Packet routing error", e)
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
        dnsEngine.stop()
        connectionTracker.stop()
        Log.i(TAG, "Packet router stopped")
    }

    fun getRouterStats(): RouterStats = RouterStats(
        packetsProcessed = packetsProcessed,
        packetsBlocked = packetsBlocked,
        sniBlocks = sniBlocks,
        dnsBlocks = dnsBlocks,
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
                if (buffer.remaining() >= udpOffset + 8) {
                    val srcPort = buffer.getShort(udpOffset).toInt() and 0xFFFF
                    val dstPort = buffer.getShort(udpOffset + 2).toInt() and 0xFFFF

                    if (isDnsBypassIp(dstIpStr) && dstPort != 53 && isDnsBypassPort(dstPort)) {
                        packetsBlocked++
                        scope.launch {
                            statsRepo.logBlocked("$dstIpStr:$dstPort", type = "dns-bypass")
                        }
                        return
                    }

                    // 2a. Per-app firewall BLOCK check (before DNS)
                    if (techniques.appFirewall && isSourceAppBlocked(srcIp, srcPort, dstIp, dstPort, "udp")) {
                        // App is blocked — drop DNS silently, drop everything else silently too
                        if (dstPort == 53) {
                            val udpLen = (buffer.getShort(udpOffset + 4).toInt() and 0xFFFF)
                            val payloadLen = udpLen - 8
                            if (payloadLen > 0 && buffer.remaining() >= udpOffset + 8 + payloadLen) {
                                val mark = buffer.position()
                                buffer.position(udpOffset + 8)
                                val queryPayload = ByteArray(payloadLen)
                                buffer.get(queryPayload)
                                buffer.position(mark)
                                val nxdomain = dnsEngine.buildNxDomainBytes(queryPayload)
                                if (nxdomain != null) {
                                    sendDnsResponse(output, srcIp, dstIp, srcPort, dstPort, nxdomain)
                                }
                            }
                        }
                        packetsBlocked++
                        return
                    }

                    // 2b. DNS interception
                    if (dstPort == 53 && techniques.dnsFiltering) {
                        val udpLen = (buffer.getShort(udpOffset + 4).toInt() and 0xFFFF)
                        val payloadLen = udpLen - 8
                        if (payloadLen > 0 && buffer.remaining() >= udpOffset + 8 + payloadLen) {
                            val queryPayload = ByteArray(payloadLen)
                            val mark = buffer.position()
                            buffer.position(udpOffset + 8)
                            buffer.get(queryPayload)
                            buffer.position(mark)

                            scope.launch {
                                try {
                                    val responsePayload = dnsEngine.resolveDns(queryPayload)
                                    if (responsePayload != null) {
                                        sendDnsResponse(output, srcIp, dstIp, srcPort, dstPort, responsePayload)
                                    }
                                } catch (e: Exception) {
                                    if (isRunning) Log.w(TAG, "DNS resolution error", e)
                                }
                            }
                            return // drop original, async response will be sent
                        }
                    }
                }
                writePacket(output, buffer.duplicate())
            }
            PROTO_TCP -> {
                val tcpOffset = pos + ipHeaderLen
                if (buffer.remaining() < tcpOffset + 20) return

                val dataOffset = ((buffer.get(tcpOffset + 12).toInt() shr 4) and 0x0F) * 4
                val tcpPayloadOffset = tcpOffset + dataOffset
                val tcpPayloadLen = totalLen - ipHeaderLen - dataOffset
                val srcPort = buffer.getShort(tcpOffset).toInt() and 0xFFFF
                val dstPort = buffer.getShort(tcpOffset + 2).toInt() and 0xFFFF

                if (isDnsBypassIp(dstIpStr) && isDnsBypassPort(dstPort)) {
                    sendRst(output, pos, ipHeaderLen, srcIp, dstIp, srcPort, dstPort)
                    packetsBlocked++
                    scope.launch {
                        statsRepo.logBlocked("$dstIpStr:$dstPort", type = "dns-bypass")
                    }
                    return
                }

                // Per-app firewall BLOCK check
                if (techniques.appFirewall && isSourceAppBlocked(srcIp, srcPort, dstIp, dstPort, "tcp")) {
                    sendRst(output, pos, ipHeaderLen, srcIp, dstIp, srcPort, dstPort)
                    packetsBlocked++
                    return
                }

                // SNI inspection on HTTPS (port 443)
                if (techniques.sniInspection && dstPort == 443 && tcpPayloadLen > 0) {
                    val sni = extractSni(buffer, tcpPayloadOffset, tcpPayloadLen)
                    if (sni != null && dnsEngine.isDomainBlocked(sni)) {
                        Log.v(TAG, "SNI block: $sni")
                        sniBlocks++
                        packetsBlocked++

                        // App Attribution
                        val pkgName = connectionTracker.getPackageForConnection(
                            InetAddress.getByAddress(srcIp),
                            srcPort,
                            InetAddress.getByAddress(dstIp),
                            dstPort,
                            "tcp"
                        ) ?: "Unknown"

                        sendRst(output, pos, ipHeaderLen, srcIp, dstIp, srcPort, dstPort)
                        scope.launch {
                            statsRepo.logBlocked(sni, appPackage = pkgName, type = "sni")
                        }
                        return
                    }
                }
                writePacket(output, buffer.duplicate())
            }
            else -> writePacket(output, buffer.duplicate())
        }
    }

    private fun isDnsBypassIp(ip: String): Boolean = ip in Constants.DNS_BYPASS_IPV4_ROUTES

    private fun isDnsBypassPort(port: Int): Boolean = port in Constants.DNS_BYPASS_PORTS

    private fun processIPv6(buffer: ByteBuffer, output: FileChannel) {
        // Drop all IPv6 packets entering the TUN.
        // IPv6 is null-routed at the VPN level (addRoute("2000::", 3))
        // to prevent IPv6 DNS leaks that would bypass our ad-blocking.
        // Any IPv6 packet reaching here should be silently dropped.
        packetsBlocked++
        return
    }

    private fun readIpv4(buffer: ByteBuffer, offset: Int): ByteArray {
        return byteArrayOf(buffer.get(offset), buffer.get(offset + 1), buffer.get(offset + 2), buffer.get(offset + 3))
    }

    /** Check if the source app of a connection is in BLOCK firewall mode. */
    private fun isSourceAppBlocked(srcIp: ByteArray, srcPort: Int, dstIp: ByteArray, dstPort: Int, proto: String): Boolean {
        return try {
            val pkg = connectionTracker.getPackageForConnection(
                InetAddress.getByAddress(srcIp), srcPort,
                InetAddress.getByAddress(dstIp), dstPort, proto
            )
            pkg != null && settingsRepo.getBlockedPackages().contains(pkg)
        } catch (_: Exception) {
            false
        }
    }

    private fun extractSni(buffer: ByteBuffer, offset: Int, length: Int): String? {
        if (length < 6) return null
        val contentType = buffer.get(offset).toInt() and 0xFF
        if (contentType != TLS_HANDSHAKE) return null
        val handshakeType = buffer.get(offset + 5).toInt() and 0xFF
        if (handshakeType != TLS_CLIENT_HELLO) return null

        try {
            if (offset + 9 > offset + length) return null
            var pos = offset + 9
            pos += 2 // version
            pos += 32 // random

            val sessionIdLen = buffer.get(pos).toInt() and 0xFF
            pos += 1 + sessionIdLen

            if (pos + 2 > offset + length) return null
            val cipherSuitesLen = buffer.getShort(pos).toInt() and 0xFFFF
            pos += 2 + cipherSuitesLen

            if (pos + 1 > offset + length) return null
            val compressionLen = buffer.get(pos).toInt() and 0xFF
            pos += 1 + compressionLen

            if (pos + 2 > offset + length) return null
            val extensionsLen = buffer.getShort(pos).toInt() and 0xFFFF
            pos += 2
            val extensionsEnd = pos + extensionsLen

            while (pos + 4 <= extensionsEnd && pos + 4 <= offset + length) {
                val extType = buffer.getShort(pos).toInt() and 0xFFFF
                val extLen = buffer.getShort(pos + 2).toInt() and 0xFFFF
                pos += 4
                if (extType == 0x0000) { // SNI
                    if (pos + 2 <= offset + length) {
                        val sniListLen = buffer.getShort(pos).toInt() and 0xFFFF
                        var sniPos = pos + 2
                        val sniListEnd = sniPos + sniListLen
                        while (sniPos + 3 <= sniListEnd && sniPos + 3 <= offset + length) {
                            val nameType = buffer.get(sniPos).toInt() and 0xFF
                            val nameLen = buffer.getShort(sniPos + 1).toInt() and 0xFFFF
                            sniPos += 3
                            if (nameType == 0 && sniPos + nameLen <= offset + length) {
                                val sniBytes = ByteArray(nameLen)
                                for (i in 0 until nameLen) sniBytes[i] = buffer.get(sniPos + i)
                                return String(sniBytes, Charsets.UTF_8)
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

    private fun writePacket(output: FileChannel, packet: ByteBuffer) {
        try {
            synchronized(outputLock) {
                output.write(packet)
            }
        } catch (_: Exception) {}
    }

    private fun calculateChecksum(buffer: ByteBuffer, offset: Int, length: Int): Int {
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

    private fun calculatePseudoChecksum(buffer: ByteBuffer, srcIp: ByteArray, dstIp: ByteArray, protocol: Int, tcpLen: Int): Int {
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

    data class RouterStats(
        val packetsProcessed: Long,
        val packetsBlocked: Long,
        val sniBlocks: Long,
        val dnsBlocks: Long,
        val bytesTotal: Long
    )
}
