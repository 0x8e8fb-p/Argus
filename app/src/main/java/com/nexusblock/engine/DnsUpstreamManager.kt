package com.nexusblock.engine

import android.util.Log
import com.nexusblock.engine.dns.DnsProviderProfile
import com.nexusblock.engine.dns.DnsProfileManager
import kotlinx.coroutines.*
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.TimeUnit

/**
 * Rewritten upstream DNS resolver that delegates to the currently selected
 * [DnsProviderProfile] from [DnsProfileManager].
 *
 * Strategy:
 * 1. Try plain UDP (fastest, single round-trip ~5-15ms).
 * 2. If plain fails, try DoH (encrypted, ads blocked by resolver).
 * 3. If DoH fails, fall back to generic plain UDP servers (8.8.8.8, 1.1.1.1).
 *
 * Resolution order is chosen because:
 * - Plain UDP resolves in ~5-15ms while DoH needs TCP+TLS+HTTP (200-500ms).
 * - The upstream AdGuard/Quad9 plain server still performs ad-blocking at
 *   the resolver level, so filtering quality is NOT reduced.
 * - Some mobile/carrier Wi-Fi blocks DoH entirely, making plain essential.
 *
 * All DNS queries are sent through VpnProtector so they exit via the
 * underlying (non-VPN) network, avoiding recursive loops.
 */
class DnsUpstreamManager(
    private val okHttpClient: OkHttpClient,
    private val profileManager: DnsProfileManager
) {
    companion object {
        private const val TAG = "Argus/DnsUpstream"
        private const val PLAIN_DNS_TIMEOUT_MS = 1500
        private const val DOH_TIMEOUT_MS = 3000L
        /** Budget for encrypted protocol (DoH) after plain fails. */
        private const val ENCRYPTED_TIMEOUT_MS = 3000L
        private const val FALLBACK_TIMEOUT_MS = 2000L
        // Cap concurrent upstream resolutions so a burst of packets from the
        // TUN can't fork unbounded coroutines and exhaust the IO thread pool.
        private const val MAX_INFLIGHT = 48

        /** Absolute fallback — these are queried only when the active profile fails. */
        val FALLBACK_SERVERS = listOf("8.8.8.8", "1.1.1.1", "9.9.9.9")
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val inflight = Semaphore(MAX_INFLIGHT)

    // Pre-built OkHttp client with a short call timeout for DoH. Built once
    // (not per query) — previous code allocated a new client wrapper on every
    // DNS packet, generating churn under TV-app traffic.
    private val dohClient: OkHttpClient by lazy {
        okHttpClient.newBuilder()
            .callTimeout(DOH_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .build()
    }

    suspend fun resolve(queryBytes: ByteArray): ByteArray? = inflight.withPermit {
        val profile = profileManager.activeProfile()

        // Strategy: Plain UDP resolves in ~5-15ms (single UDP round-trip) while
        // DoH needs TCP+TLS+HTTP (200-500ms). Try plain FIRST — if it answers
        // quickly, skip the expensive encrypted path entirely. This makes every
        // DNS query (and therefore every new connection) dramatically faster.
        // The upstream AdGuard/Quad9 plain server still performs ad-blocking at
        // the resolver level, so filtering quality is NOT reduced.
        val plainResult = if (profile.plainIpv4.isNotEmpty()) {
            withTimeoutOrNull(PLAIN_DNS_TIMEOUT_MS.toLong()) {
                withContext(Dispatchers.IO) { resolvePlain(profile, queryBytes) }
            }
        } else null

        if (plainResult != null) return@withPermit plainResult

        // Plain failed (network blocks UDP/53, or profile has no plain servers).
        // Fall back to encrypted protocols.
        val protocols = profile.resolutionOrder().filter { it != Protocol.PLAIN }
        if (protocols.isEmpty()) {
            return@withPermit withTimeoutOrNull(FALLBACK_TIMEOUT_MS) {
                resolvePlainFallback(queryBytes)
            }
        }

        val winner = coroutineScope {
            val deferreds = protocols.map { proto ->
                async(Dispatchers.IO) {
                    when (proto) {
                        Protocol.DOH -> resolveDoh(profile, queryBytes)
                        Protocol.PLAIN -> null
                    }
                }
            }
            try {
                withTimeoutOrNull(ENCRYPTED_TIMEOUT_MS) {
                    selectFirstNonNull(deferreds)
                }
            } finally {
                deferreds.forEach { it.cancel() }
            }
        }
        if (winner != null) return@withPermit winner

        // All profile protocols failed — generic fallbacks
        Log.w(TAG, "All protocols failed for ${profile.name}, using fallbacks")
        withTimeoutOrNull(FALLBACK_TIMEOUT_MS) { resolvePlainFallback(queryBytes) }
    }

    /**
     * Await the first deferred that completes with a non-null result.
     * Returns null only if all deferreds completed with null.
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private suspend fun <T : Any> selectFirstNonNull(
        deferreds: List<Deferred<T?>>
    ): T? {
        val remaining = deferreds.toMutableList()
        while (remaining.isNotEmpty()) {
            val done = select<Deferred<T?>> {
                remaining.forEach { d -> d.onAwait { d } }
            }
            remaining.remove(done)
            val value = done.getCompleted()
            if (value != null) return value
        }
        return null
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
            dohClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.bytes() else null
            }
        } catch (e: Exception) {
            Log.w(TAG, "DoH failed for ${profile.name}: ${e.message}")
            null
        }
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

    /**
     * Public hook used by [DnsFilterEngine] for critical-allow domains that
     * must not be sent through the filtered upstream. Uses VPN-protected
     * sockets so the query does not loop back through the TUN.
     */
    suspend fun resolvePlainFallbackProtected(queryBytes: ByteArray): ByteArray? =
        inflight.withPermit {
            withTimeoutOrNull(FALLBACK_TIMEOUT_MS) { resolvePlainFallback(queryBytes) }
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
        list.add(Protocol.PLAIN)
        return list
    }

    private enum class Protocol { DOH, PLAIN }
}
