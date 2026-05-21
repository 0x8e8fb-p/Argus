package com.nexusblock.engine

import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.dnsoverhttps.DnsOverHttps
import okhttp3.tls.HandshakeCertificates
import org.xbill.DNS.DClass
import org.xbill.DNS.Flags
import org.xbill.DNS.Message
import org.xbill.DNS.Opcode
import org.xbill.DNS.Rcode
import org.xbill.DNS.Record
import org.xbill.DNS.Section
import org.xbill.DNS.*
import java.io.IOException
import java.net.InetAddress
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlinx.coroutines.*

/**
 * Manages upstream DNS resolution with support for plain DNS, DNS-over-TLS (DoT),
 * and DNS-over-HTTPS (DoH).
 *
 * Default upstreams:
 * - Plain: 8.8.8.8, 1.1.1.1
 * - DoH: https://cloudflare-dns.com/dns-query, https://dns.google/dns-query
 * - DoT: cloudflare-dns.com, dns.google
 */
class DnsUpstreamManager(
    private val okHttpClient: OkHttpClient,
    private var mode: DnsMode = DnsMode.PLAIN
) {
    companion object {
        private const val TAG = "NexusBlock/DnsUpstream"

        val CLOUDFLARE_DOH = "https://cloudflare-dns.com/dns-query"
        val GOOGLE_DOH = "https://dns.google/dns-query"
        val QUAD9_DOH = "https://dns.quad9.net/dns-query"

        val PLAIN_DNS_SERVERS = listOf("8.8.8.8", "1.1.1.1", "9.9.9.9")
    }

    private var plainDnsClient: PlainDnsClient? = null

    init {
        initializeClient()
    }

    fun setMode(newMode: DnsMode) {
        if (mode == newMode) return
        mode = newMode
        initializeClient()
        Log.i(TAG, "Switched to DNS mode: $mode")
    }

    fun getMode(): DnsMode = mode

    private fun initializeClient() {
        when (mode) {
            DnsMode.PLAIN -> {
                plainDnsClient = PlainDnsClient(PLAIN_DNS_SERVERS)
            }
            DnsMode.DOH_CLOUDFLARE, DnsMode.DOH_GOOGLE, DnsMode.DOH_QUAD9 -> {
                // Using raw DoH messaging in resolve()
                plainDnsClient = PlainDnsClient(PLAIN_DNS_SERVERS)
            }
        }
    }

    fun resolve(queryBytes: ByteArray): ByteArray? {
        return runBlocking {
            when (mode) {
                DnsMode.PLAIN -> plainDnsClient?.resolve(queryBytes)
                else -> {
                    // Parallel resolution: DoH and Fallback Plain DNS
                    val dohDeferred = async(Dispatchers.IO) { resolveDohMessage(queryBytes) }
                    val plainDeferred = async(Dispatchers.IO) { plainDnsClient?.resolve(queryBytes) }
                    
                    val dohResult = dohDeferred.await()
                    if (dohResult != null) {
                        plainDeferred.cancel()
                        dohResult
                    } else {
                        plainDeferred.await()
                    }
                }
            }
        }
    }

    private fun resolveDohMessage(queryBytes: ByteArray): ByteArray? {
        val url = when (mode) {
            DnsMode.DOH_CLOUDFLARE -> CLOUDFLARE_DOH
            DnsMode.DOH_GOOGLE -> GOOGLE_DOH
            DnsMode.DOH_QUAD9 -> QUAD9_DOH
            else -> return null
        }

        val request = Request.Builder()
            .url(url)
            .header("Content-Type", "application/dns-message")
            .header("Accept", "application/dns-message")
            .post(queryBytes.toRequestBody("application/dns-message".toMediaType()))
            .build()

        return try {
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.bytes()
                } else null
            }
        } catch (e: Exception) {
            Log.w(TAG, "DoH Message resolution failed for $url", e)
            null
        }
    }

    /**
     * Plain UDP DNS client with retry and failover
     */
    private class PlainDnsClient(
        private val servers: List<String>,
        private val port: Int = 53,
        private val timeoutMs: Int = 5000
    ) {
        fun resolve(queryBytes: ByteArray): ByteArray? {
            for (server in servers) {
                try {
                    java.net.DatagramSocket().use { socket ->
                        VpnProtector.protect(socket)
                        socket.soTimeout = timeoutMs

                        val packet = java.net.DatagramPacket(
                            queryBytes,
                            queryBytes.size,
                            java.net.InetAddress.getByName(server),
                            port
                        )
                        socket.send(packet)

                        val responseBuffer = ByteArray(4096)
                        val responsePacket = java.net.DatagramPacket(responseBuffer, responseBuffer.size)
                        socket.receive(responsePacket)

                        return responsePacket.data.copyOf(responsePacket.length)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "DNS server $server failed", e)
                }
            }
            return null
        }
    }

    enum class DnsMode {
        PLAIN, DOH_CLOUDFLARE, DOH_GOOGLE, DOH_QUAD9
    }
}
