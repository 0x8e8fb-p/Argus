package com.nexusblock.engine

import android.util.Log
import com.nexusblock.engine.dns.DnsProviderProfile
import com.nexusblock.engine.dns.DnsProfileManager
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.xbill.DNS.Message
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import javax.net.ssl.SSLSocketFactory

/**
 * Rewritten upstream DNS resolver that delegates to the currently selected
 * [DnsProviderProfile] from [DnsProfileManager].
 *
 * Strategy:
 * 1. Try DoH (most private, ads already blocked by the resolver).
 * 2. If DoH fails, try DoT (encrypted, fast).
 * 3. If DoT fails, fall back to plain UDP (fastest, least private).
 * 4. If plain UDP fails, try the built-in fallback servers (8.8.8.8, 1.1.1.1).
 *
 * Resolution order is chosen because:
 * - DoH = maximum ad-blocking effectiveness (the resolver sees the full hostname).
 * - DoT = good balance of speed + privacy.
 * - Plain UDP = fastest, works on all networks (some mobile/carrier Wi-Fi
 *   blocks DoH/DoT entirely).
 *
 * All DNS queries are sent through VpnProtector so they exit via the
 * underlying (non-VPN) network, avoiding recursive loops.
 */
class DnsUpstreamManager(
    private val okHttpClient: OkHttpClient,
    private val profileManager: DnsProfileManager
) {
    companion object {
        private const val TAG = "NexusBlock/DnsUpstream"
        private const val PLAIN_DNS_TIMEOUT_MS = 5000
        private const val DOH_TIMEOUT_MS = 10000

        /** Absolute fallback — these are queried only when the active profile fails. */
        val FALLBACK_SERVERS = listOf("8.8.8.8", "1.1.1.1", "9.9.9.9")
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun resolve(queryBytes: ByteArray): ByteArray? = runBlocking {
        val profile = profileManager.activeProfile()
        val protocols = profile.resolutionOrder()

        for (proto in protocols) {
            val result = when (proto) {
                Protocol.DOH -> resolveDoh(profile, queryBytes)
                Protocol.DOT -> resolveDot(profile, queryBytes)
                Protocol.PLAIN -> resolvePlain(profile, queryBytes)
            }
            if (result != null) {
                return@runBlocking result
            }
        }

        // All profile protocols failed — try generic fallbacks so the user
        // doesn't lose internet entirely.
        Log.w(TAG, "All profile protocols failed for ${profile.name}, using generic fallbacks")
        resolvePlainFallback(queryBytes)
    }

    // ─────────────────────────────────────────────────────────────
    // DoH
    // ─────────────────────────────────────────────────────────────

    private suspend fun resolveDoh(
        profile: DnsProviderProfile,
        queryBytes: ByteArray
    ): ByteArray? = withContext(Dispatchers.IO) {
        val url = profile.dohUrl ?: return@withContext null
        val request = Request.Builder()
            .url(url)
            .header("Content-Type", "application/dns-message")
            .header("Accept", "application/dns-message")
            .post(queryBytes.toRequestBody("application/dns-message".toMediaType()))
            .build()

        try {
            okHttpClient.newBuilder()
                .callTimeout(DOH_TIMEOUT_MS.toLong(), java.util.concurrent.TimeUnit.MILLISECONDS)
                .build()
                .newCall(request)
                .execute()
                .use { response ->
                    if (response.isSuccessful) {
                        response.body?.bytes()
                    } else null
                }
        } catch (e: Exception) {
            Log.w(TAG, "DoH failed for ${profile.name}: ${e.message}")
            null
        }
    }

    // ─────────────────────────────────────────────────────────────
    // DoT (DNS-over-TLS)
    // We implement DoT by wrapping a plain UDP query inside an TLS
    // connection to port 853. For simplicity we fallback to plain
    // DNS tunnelled over TLS via an SSLSocket to the resolver.
    // ─────────────────────────────────────────────────────────────

    private suspend fun resolveDot(
        profile: DnsProviderProfile,
        queryBytes: ByteArray
    ): ByteArray? = withContext(Dispatchers.IO) {
        val hostname = profile.dotHostname ?: return@withContext null
        val servers = profile.plainIpv4.takeIf { it.isNotEmpty() } ?: return@withContext null

        for (server in servers) {
            try {
                val sslSocket = SSLSocketFactory.getDefault().createSocket(server, 853) as javax.net.ssl.SSLSocket
                VpnProtector.protect(sslSocket)
                sslSocket.use { socket ->
                    socket.startHandshake()

                    // DNS-over-TLS framing: 2-byte length prefix + raw DNS message
                    val out = socket.outputStream
                    out.write(queryBytes.size shr 8 and 0xFF)
                    out.write(queryBytes.size and 0xFF)
                    out.write(queryBytes)
                    out.flush()

                    val input = socket.inputStream
                    val lenHi = input.read()
                    val lenLo = input.read()
                    if (lenHi < 0 || lenLo < 0) return@use null
                    val len = (lenHi shl 8) or lenLo
                    val response = ByteArray(len)
                    var read = 0
                    while (read < len) {
                        val r = input.read(response, read, len - read)
                        if (r < 0) return@use null
                        read += r
                    }
                    return@withContext response
                }
            } catch (e: Exception) {
                Log.w(TAG, "DoT failed for $server:$hostname: ${e.message}")
            }
        }
        null
    }

    // ─────────────────────────────────────────────────────────────
    // Plain UDP (profile servers)
    // ─────────────────────────────────────────────────────────────

    private suspend fun resolvePlain(
        profile: DnsProviderProfile,
        queryBytes: ByteArray
    ): ByteArray? = withContext(Dispatchers.IO) {
        val servers = profile.plainIpv4.takeIf { it.isNotEmpty() } ?: return@withContext null
        resolveWithServers(servers, queryBytes)
    }

    // ─────────────────────────────────────────────────────────────
    // Plain UDP (generic fallbacks)
    // ─────────────────────────────────────────────────────────────

    private suspend fun resolvePlainFallback(queryBytes: ByteArray): ByteArray? =
        withContext(Dispatchers.IO) {
            resolveWithServers(FALLBACK_SERVERS, queryBytes)
        }

    private fun resolveWithServers(servers: List<String>, queryBytes: ByteArray): ByteArray? {
        for (server in servers) {
            try {
                DatagramSocket().use { socket ->
                    VpnProtector.protect(socket)
                    socket.soTimeout = PLAIN_DNS_TIMEOUT_MS

                    val packet = DatagramPacket(
                        queryBytes, queryBytes.size,
                        InetAddress.getByName(server), 53
                    )
                    socket.send(packet)

                    val buf = ByteArray(4096)
                    val resp = DatagramPacket(buf, buf.size)
                    socket.receive(resp)
                    return resp.data.copyOf(resp.length)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Plain DNS server $server failed: ${e.message}")
            }
        }
        return null
    }

    /** Determine the preferred resolution order for a profile. */
    private fun DnsProviderProfile.resolutionOrder(): List<Protocol> {
        // Prefer DoH when available because the resolver blocks ads at the
        // resolution layer with the clearest view of the full domain name.
        val list = mutableListOf<Protocol>()
        if (!dohUrl.isNullOrBlank()) list.add(Protocol.DOH)
        if (!dotHostname.isNullOrBlank()) list.add(Protocol.DOT)
        list.add(Protocol.PLAIN)
        return list
    }

    private enum class Protocol { DOH, DOT, PLAIN }
}
