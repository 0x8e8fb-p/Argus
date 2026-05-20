package com.nexusblock.engine

import android.net.VpnService
import android.util.Log
import com.nexusblock.data.repository.StatsRepository
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.littleshoot.proxy.HttpFilters
import org.littleshoot.proxy.HttpFiltersAdapter
import org.littleshoot.proxy.HttpFiltersSourceAdapter
import org.littleshoot.proxy.HttpProxyServer
import org.littleshoot.proxy.MitmManager
import org.littleshoot.proxy.impl.DefaultHttpProxyServer
import java.net.InetSocketAddress
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MitmProxyManager @Inject constructor(
    private val certificateManager: CertificateManager,
    private val statsRepo: StatsRepository,
    private val settingsRepo: com.nexusblock.data.repository.SettingsRepository
) {
    companion object {
        private const val TAG = "NexusBlock/Proxy"
        private const val PROXY_PORT = 8123

        // Known ad/tracking patterns — ONLY actual ad serving paths.
        // DO NOT block analytics/QOE/engagement paths or YouTube
        // recommendations will stop working.
        private val BLOCKED_PATHS = listOf(
            "/youtubei/v1/ads",
            // NOTE: /youtubei/v1/log_event is analytics, NOT ads. Removed.
            "/pagead",
            "/_get_ads",
            // NOTE: /get_video_metadata is video info, NOT ads. Removed.
            "/api/stats/ads",
            // NOTE: /api/stats/qoe is Quality-of-Experience metrics, NOT ads. Removed.
            "/ad_data",
            "/ads_",
            "/advertisement"
        )

        private val BLOCKED_HOST_PATTERNS = listOf(
            Regex(".*googleadservices\\.com.*"),
            Regex(".*doubleclick\\.net.*"),
            Regex(".*googlesyndication\\.com.*"),
            Regex(".*google-analytics\\.com.*"),
            Regex(".*facebook\\.com/tr.*"),
            // NOTE: Removed blanket youtube.com/api/stats block — that broke
            // QoE/playback stats which feed recommendations.
            // Only block explicit ad stats pages:
            Regex(".*youtube\\.com/api/stats/ads.*"),
            Regex(".*youtube\\.com/pagead.*"),
            Regex(".*youtube\\.com/ptracking.*")
        )
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var proxyServer: HttpProxyServer? = null
    private var techniquesJob: Job? = null
    private var techniques = com.nexusblock.data.repository.BlockingTechniques()

    @Volatile
    var isRunning = false
        private set

    fun start() {
        if (isRunning) return

        techniquesJob?.cancel()
        techniquesJob = scope.launch {
            settingsRepo.observeTechniques().collect {
                techniques = it
            }
        }

        try {
            proxyServer = DefaultHttpProxyServer.bootstrap()
                .withPort(PROXY_PORT)
                .withAddress(InetSocketAddress("127.0.0.1", PROXY_PORT))
                .withFiltersSource(object : HttpFiltersSourceAdapter() {
                    override fun filterRequest(
                        originalRequest: HttpRequest
                    ): HttpFilters {
                        return NexusBlockHttpFilters(originalRequest, statsRepo, scope, techniques)
                    }
                })
                .withConnectTimeout(30000)
                .withIdleConnectionTimeout(60)
                .start()

            isRunning = true
            Log.i(TAG, "MITM proxy started on port $PROXY_PORT")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start MITM proxy", e)
        }
    }

    fun stop() {
        isRunning = false
        techniquesJob?.cancel()
        techniquesJob = null
        try {
            proxyServer?.stop()
            proxyServer = null
            Log.i(TAG, "MITM proxy stopped")
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping proxy", e)
        }
    }

    fun getProxyAddress(): InetSocketAddress? {
        return if (isRunning) {
            InetSocketAddress("127.0.0.1", PROXY_PORT)
        } else null
    }

    private class NexusBlockHttpFilters(
        request: HttpRequest,
        private val statsRepo: StatsRepository,
        private val scope: CoroutineScope,
        private val techniques: com.nexusblock.data.repository.BlockingTechniques
    ) : HttpFilters {

        private var shouldBlock = false
        private var blockedHost = ""

        override fun clientToProxyRequest(httpObject: HttpObject): HttpResponse? {
            if (httpObject is HttpRequest) {
                val uri = httpObject.uri
                val host = httpObject.headers().get(HttpHeaderNames.HOST) ?: ""

                // 1. Check blocked paths
                for (path in BLOCKED_PATHS) {
                    if (uri.contains(path, ignoreCase = true)) {
                        shouldBlock = true
                        blockedHost = host
                        Log.v(TAG, "Blocked by path: $uri")
                        return createBlockedResponse()
                    }
                }

                // 2. Check blocked host patterns
                for (pattern in BLOCKED_HOST_PATTERNS) {
                    if (pattern.matches(host)) {
                        shouldBlock = true
                        blockedHost = host
                        Log.v(TAG, "Blocked by host: $host")
                        return createBlockedResponse()
                    }
                }

                // 3. Header Filtering (if enabled)
                if (techniques.headerFilter) {
                    val headers = httpObject.headers()
                    headers.remove("X-App-Usage")
                    headers.remove("X-FB-HTTP-Engine")
                    headers.remove("X-Pusher-Engine")
                    headers.remove("X-Telemetry")
                    headers.remove("Referer")
                }

                // 4. User-Agent spoofing for analytics trackers only.
                // Do NOT spoof YouTube/GoogleVideo UA — it breaks TV app playback.
                if (host.contains("google-analytics", ignoreCase = true) ||
                    host.contains("googlesyndication", ignoreCase = true) ||
                    host.contains("doubleclick", ignoreCase = true)
                ) {
                    httpObject.headers().set(
                        "User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
                    )
                }
            }
            return null
        }

        override fun proxyToServerRequest(httpObject: HttpObject): HttpResponse? = null
        override fun proxyToServerRequestSent() {}
        override fun proxyToServerRequestSending() {}
        override fun serverToProxyResponse(httpObject: HttpObject): HttpObject? {
            if (shouldBlock && httpObject is HttpResponse) {
                if (httpObject is FullHttpResponse) {
                    httpObject.content().clear()
                }
            }
            return httpObject
        }

        override fun serverToProxyResponseTimedOut() {}
        override fun serverToProxyResponseReceiving() {}
        override fun serverToProxyResponseReceived() {
            if (shouldBlock && blockedHost.isNotEmpty()) {
                scope.launch {
                    statsRepo.logBlocked(blockedHost, type = "https")
                }
            }
        }

        override fun proxyToClientResponse(httpObject: HttpObject): HttpObject = httpObject
        override fun proxyToServerConnectionQueued() {}
        override fun proxyToServerConnectionStarted() {}
        override fun proxyToServerConnectionSSLHandshakeStarted() {}
        override fun proxyToServerConnectionFailed() {}
        override fun proxyToServerConnectionSucceeded(serverCtx: ChannelHandlerContext?) {}
        override fun proxyToServerResolutionStarted(hostAndPort: String?): InetSocketAddress? = null
        override fun proxyToServerResolutionFailed(hostAndPort: String?) {}
        override fun proxyToServerResolutionSucceeded(serverIp: String?, res: InetSocketAddress?) {}

        private fun createBlockedResponse(): FullHttpResponse {
            return DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.NO_CONTENT
            ).apply {
                headers().set(HttpHeaderNames.CONTENT_LENGTH, 0)
                headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
            }
        }
    }
}
