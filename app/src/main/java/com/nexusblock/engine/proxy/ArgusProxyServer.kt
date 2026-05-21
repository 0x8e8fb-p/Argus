package com.nexusblock.engine.proxy

import android.util.Log
import com.nexusblock.Constants
import com.nexusblock.engine.transformers.TransformerEngine
import com.nexusblock.router.StrategyRouter
import kotlinx.coroutines.*
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.Charset
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lightweight HTTP/HTTPS/SOCKS5 proxy server for MITM API response rewriting.
 *
 * Modes of operation:
 *  1. HTTP proxy: direct HTTP request proxying
 *  2. HTTPS proxy (CONNECT tunnel): selective TLS MITM or blind tunnel
 *  3. SOCKS5 proxy: for tun2socks Go integration — receives TCP connections
 *     from the native tun2socks binary and routes them through the same
 *     inspection pipeline.
 *
 * Replaces the heavy LittleProxy+Netty stack (~4 MB) with a Kotlin coroutine-based
 * implementation (~800 lines). SOCKS5 support allows integration with a Go-based
 * tun2socks for high-performance full-tunnel routing without rewriting the packet
 * router in Kotlin.
 */
@Singleton
class ArgusProxyServer @Inject constructor(
    private val certificateManager: com.nexusblock.engine.CertificateManager,
    private val transformerEngine: TransformerEngine,
    private val strategyRouter: StrategyRouter
) {
    companion object {
        private const val TAG = "Argus/Proxy"
        private const val PROXY_HOST = Constants.PROXY_HOST
        private const val PROXY_PORT = Constants.PROXY_PORT
        private const val CONNECT_TIMEOUT = 30000
        private const val BUFFER_SIZE = 8192
        private const val HTTP_VERSION = "HTTP/1.1"

        // SOCKS5 constants
        private const val SOCKS5_VERSION: Byte = 0x05
        private const val SOCKS5_AUTH_NONE: Byte = 0x00
        private const val SOCKS5_CMD_CONNECT: Byte = 0x01
        private const val SOCKS5_ATYP_IPV4: Byte = 0x01
        private const val SOCKS5_ATYP_DOMAIN: Byte = 0x03
        private const val SOCKS5_ATYP_IPV6: Byte = 0x04
        private const val SOCKS5_REP_SUCCESS: Byte = 0x00
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var serverSocket: ServerSocket? = null
    private var acceptJob: Job? = null

    @Volatile
    var isRunning = false
        private set

    fun start() {
        if (isRunning) return
        isRunning = true

        acceptJob = scope.launch {
            try {
                serverSocket = ServerSocket(PROXY_PORT, 50, java.net.InetAddress.getByName(PROXY_HOST))
                Log.i(TAG, "ArgusProxyServer started on $PROXY_HOST:$PROXY_PORT (HTTP/HTTPS/SOCKS5)")

                while (isActive && isRunning) {
                    val clientSocket = try {
                        serverSocket?.accept()
                    } catch (e: Exception) {
                        if (isActive) Log.w(TAG, "Accept error", e)
                        delay(100)
                        continue
                    }
                    clientSocket ?: continue

                    launch { handleClient(clientSocket) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Proxy server error", e)
            }
        }
    }

    fun stop() {
        isRunning = false
        acceptJob?.cancel()
        acceptJob = null
        try {
            serverSocket?.close()
            serverSocket = null
        } catch (e: Exception) {
            Log.w(TAG, "Error closing proxy socket", e)
        }
        Log.i(TAG, "ArgusProxyServer stopped")
    }

    private suspend fun handleClient(clientSocket: Socket) = withContext(Dispatchers.IO) {
        clientSocket.soTimeout = CONNECT_TIMEOUT

        try {
            clientSocket.use { client ->
                // Buffer the input stream so we can peek (mark/reset) the first byte.
                // SocketInputStream does not support mark/reset natively.
                val clientIn = BufferedInputStream(client.getInputStream())
                val clientOut = client.getOutputStream()

                // Peek at first byte to detect SOCKS5 vs HTTP
                val firstByte = clientIn.peekFirstByte() ?: return@use

                if (firstByte == SOCKS5_VERSION) {
                    // SOCKS5 mode (from tun2socks)
                    handleSocks5(client, clientIn, clientOut)
                } else {
                    // HTTP/HTTPS proxy mode
                    handleHttpProxy(client, clientIn, clientOut)
                }
            }
        } catch (e: Exception) {
            if (isActive) Log.w(TAG, "Client handler error", e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SOCKS5 Handler (for tun2socks integration)
    // ═══════════════════════════════════════════════════════════════════════

    private suspend fun handleSocks5(
        clientSocket: Socket,
        clientIn: InputStream,
        clientOut: OutputStream
    ) = withContext(Dispatchers.IO) {
        try {
            // SOCKS5 greeting: VER, NMETHODS, METHODS[]
            val ver = clientIn.read()
            if (ver != 5) return@withContext
            val nMethods = clientIn.read()
            if (nMethods < 0) return@withContext

            val methods = ByteArray(nMethods)
            if (clientIn.read(methods) != nMethods) return@withContext

            // Select no-auth (0x00) if offered
            val selectedAuth = if (methods.contains(SOCKS5_AUTH_NONE)) SOCKS5_AUTH_NONE else 0xFF.toByte()
            clientOut.write(byteArrayOf(SOCKS5_VERSION, selectedAuth))
            clientOut.flush()
            if (selectedAuth == 0xFF.toByte()) return@withContext

            // Request: VER, CMD, RSV, ATYP, DST.ADDR, DST.PORT
            val reqVer = clientIn.read()
            val cmd = clientIn.read()
            val rsv = clientIn.read() // reserved
            val atyp = clientIn.read()
            if (reqVer != 5 || cmd != SOCKS5_CMD_CONNECT.toInt()) {
                // Only CONNECT supported
                sendSocks5Reply(clientOut, 0x07) // Command not supported
                return@withContext
            }

            val (targetHost, targetPort) = when (atyp.toByte()) {
                SOCKS5_ATYP_IPV4 -> {
                    val ip = ByteArray(4)
                    clientIn.read(ip)
                    val port = readPort(clientIn)
                    "${ip[0].toInt() and 0xFF}.${ip[1].toInt() and 0xFF}.${ip[2].toInt() and 0xFF}.${ip[3].toInt() and 0xFF}" to port
                }
                SOCKS5_ATYP_DOMAIN -> {
                    val domainLen = clientIn.read()
                    val domain = ByteArray(domainLen)
                    clientIn.read(domain)
                    val port = readPort(clientIn)
                    String(domain, Charsets.UTF_8) to port
                }
                SOCKS5_ATYP_IPV6 -> {
                    // IPv6 not supported for SOCKS5 in this version
                    sendSocks5Reply(clientOut, 0x08) // Address type not supported
                    return@withContext
                }
                else -> {
                    sendSocks5Reply(clientOut, 0x08)
                    return@withContext
                }
            }

            // Determine if we MITM this connection
            val shouldMitm = strategyRouter.shouldMitm(targetHost) && targetPort == 443

            if (shouldMitm) {
                // Reply success, then do TLS MITM
                sendSocks5Reply(clientOut, SOCKS5_REP_SUCCESS)

                certificateManager.initialize()
                val leaf = certificateManager.generateLeafCertificate(targetHost)
                if (leaf != null) {
                    MitmSocketHandler.handleMitmConnection(
                        clientSocket, clientIn, clientOut,
                        targetHost, targetPort,
                        leaf.second, leaf.first,
                        transformerEngine
                    )
                }
            } else {
                // Blind TCP tunnel via SOCKS5
                val upstream = try {
                    Socket().apply {
                        com.nexusblock.service.NexusVpnService.currentInstance?.protect(this)
                        connect(InetSocketAddress(targetHost, targetPort), 15000)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "SOCKS5 upstream connect failed for $targetHost:$targetPort", e)
                    sendSocks5Reply(clientOut, 0x05) // Connection refused
                    return@withContext
                }

                sendSocks5Reply(clientOut, SOCKS5_REP_SUCCESS)

                upstream.use { srv ->
                    val srvIn = srv.getInputStream()
                    val srvOut = srv.getOutputStream()
                    val clientToServer = async { pipeAll(clientIn, srvOut) }
                    val serverToClient = async { pipeAll(srvIn, clientOut) }
                    clientToServer.await()
                    serverToClient.await()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "SOCKS5 handler error", e)
        }
    }

    private fun sendSocks5Reply(out: OutputStream, reply: Byte) {
        // VER, REP, RSV, ATYP(IPv4), BND.ADDR(0.0.0.0), BND.PORT(0)
        out.write(byteArrayOf(
            SOCKS5_VERSION, reply, 0x00, SOCKS5_ATYP_IPV4,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00
        ))
        out.flush()
    }

    private fun readPort(input: InputStream): Int {
        val high = input.read()
        val low = input.read()
        return (high shl 8) or low
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HTTP/HTTPS Proxy Handler
    // ═══════════════════════════════════════════════════════════════════════

    private suspend fun handleHttpProxy(
        clientSocket: Socket,
        clientIn: InputStream,
        clientOut: OutputStream
    ) = withContext(Dispatchers.IO) {
        val requestLine = readLine(clientIn) ?: return@withContext
        val parts = requestLine.split(" ")
        if (parts.size < 3) return@withContext

        val method = parts[0]
        val target = parts[1]

        // Read headers
        val headers = mutableMapOf<String, String>()
        while (true) {
            val line = readLine(clientIn) ?: break
            if (line.isEmpty()) break
            val colonIdx = line.indexOf(':')
            if (colonIdx > 0) {
                headers[line.substring(0, colonIdx).trim().lowercase()] =
                    line.substring(colonIdx + 1).trim()
            }
        }

        if (method.equals("CONNECT", ignoreCase = true)) {
            handleConnect(clientSocket, clientIn, clientOut, target, headers)
        } else {
            handleHttpRequest(clientSocket, clientIn, clientOut, method, target, headers)
        }
    }

    private suspend fun handleConnect(
        clientSocket: Socket,
        clientIn: InputStream,
        clientOut: OutputStream,
        target: String,
        headers: Map<String, String>
    ) = withContext(Dispatchers.IO) {
        val (host, port) = parseHostPort(target, 443)

        val shouldMitm = strategyRouter.shouldMitm(host)

        if (!shouldMitm) {
            val upstream = try {
                Socket(host, port)
            } catch (e: Exception) {
                clientOut.write("$HTTP_VERSION 502 Bad Gateway\r\n\r\n".toByteArray())
                return@withContext
            }

            clientOut.write("$HTTP_VERSION 200 Connection Established\r\n\r\n".toByteArray())
            clientOut.flush()

            upstream.use { srv ->
                val srvIn = srv.getInputStream()
                val srvOut = srv.getOutputStream()
                val clientToServer = async { pipeAll(clientIn, srvOut) }
                val serverToClient = async { pipeAll(srvIn, clientOut) }
                clientToServer.await()
                serverToClient.await()
            }
            return@withContext
        }

        clientOut.write("$HTTP_VERSION 200 Connection Established\r\n\r\n".toByteArray())
        clientOut.flush()

        certificateManager.initialize()
        val leaf = certificateManager.generateLeafCertificate(host)
        if (leaf == null) {
            Log.w(TAG, "Failed to generate leaf cert for $host")
            return@withContext
        }

        MitmSocketHandler.handleMitmConnection(
            clientSocket, clientIn, clientOut,
            host, port,
            leaf.second, leaf.first,
            transformerEngine
        )
    }

    private suspend fun handleHttpRequest(
        clientSocket: Socket,
        clientIn: InputStream,
        clientOut: OutputStream,
        method: String,
        target: String,
        headers: Map<String, String>
    ) = withContext(Dispatchers.IO) {
        val url = if (target.startsWith("http://")) target else "http://$target"
        val parsed = java.net.URL(url)
        val host = parsed.host
        val port = if (parsed.port != -1) parsed.port else 80
        val path = parsed.file.ifEmpty { "/" }

        val upstream = try {
            Socket(host, port)
        } catch (e: Exception) {
            clientOut.write("$HTTP_VERSION 502 Bad Gateway\r\n\r\n".toByteArray())
            return@withContext
        }

        upstream.use { srv ->
            val srvOut = srv.getOutputStream()
            val srvIn = srv.getInputStream()

            val requestBuilder = StringBuilder()
            requestBuilder.append("$method $path $HTTP_VERSION\r\n")
            headers.forEach { (k, v) -> requestBuilder.append("$k: $v\r\n") }
            requestBuilder.append("Host: $host\r\n")
            requestBuilder.append("Connection: close\r\n")
            requestBuilder.append("\r\n")
            srvOut.write(requestBuilder.toString().toByteArray())

            pipeAll(clientIn, srvOut)
            srvOut.flush()

            val responseLine = readLine(srvIn) ?: return@use
            clientOut.write((responseLine + "\r\n").toByteArray())

            val respHeaders = mutableMapOf<String, String>()
            while (true) {
                val line = readLine(srvIn) ?: break
                clientOut.write((line + "\r\n").toByteArray())
                if (line.isEmpty()) break
                val colonIdx = line.indexOf(':')
                if (colonIdx > 0) {
                    respHeaders[line.substring(0, colonIdx).trim().lowercase()] =
                        line.substring(colonIdx + 1).trim()
                }
            }
            clientOut.flush()
            pipeAll(srvIn, clientOut)
        }
    }

    private fun parseHostPort(target: String, defaultPort: Int): Pair<String, Int> {
        val bracketEnd = target.lastIndexOf(']')
        val colonIdx = target.lastIndexOf(':')
        return if (colonIdx > 0 && colonIdx > bracketEnd) {
            val hostPart = target.substring(0, colonIdx)
            val portPart = target.substring(colonIdx + 1).toIntOrNull() ?: defaultPort
            Pair(hostPart, portPart)
        } else {
            Pair(target, defaultPort)
        }
    }

    private fun readLine(input: InputStream): String? {
        val baos = ByteArrayOutputStream(256)
        var b: Int
        try {
            while (true) {
                b = input.read()
                if (b == -1) {
                    return if (baos.size() == 0) null else baos.toString("ISO-8859-1")
                }
                if (b == '\r'.code) {
                    val next = input.read()
                    if (next == '\n'.code) break
                    baos.write(b)
                    if (next != -1) baos.write(next)
                } else if (b == '\n'.code) {
                    break
                } else {
                    baos.write(b)
                }
            }
        } catch (e: Exception) {
            return if (baos.size() == 0) null else baos.toString("ISO-8859-1")
        }
        return baos.toString("ISO-8859-1")
    }

    private suspend fun pipeAll(input: InputStream, output: OutputStream) = withContext(Dispatchers.IO) {
        val buffer = ByteArray(BUFFER_SIZE)
        try {
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                output.write(buffer, 0, read)
            }
        } catch (_: Exception) {}
    }

    /**
     * Peek at the first byte without consuming it. Used for protocol detection.
     * Requires a BufferedInputStream so mark/reset work reliably.
     */
    private fun InputStream.peekFirstByte(): Byte? {
        if (this is java.io.BufferedInputStream) {
            return try {
                mark(8192)
                val b = read()
                reset()
                if (b == -1) null else b.toByte()
            } catch (_: Exception) { null }
        }
        // Fallback for non-buffered streams
        if (available() <= 0) {
            try { Thread.sleep(100) } catch (_: InterruptedException) {}
        }
        return if (available() > 0) {
            try {
                mark(1)
                val b = read()
                reset()
                if (b == -1) null else b.toByte()
            } catch (_: Exception) { null }
        } else null
    }
}
