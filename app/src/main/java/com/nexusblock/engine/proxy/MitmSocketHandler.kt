package com.nexusblock.engine.proxy

import android.util.Log
import com.nexusblock.engine.transformers.TransformerEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.nio.charset.Charset
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.Socket
import java.security.PrivateKey
import java.security.cert.X509Certificate
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLServerSocketFactory
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Handles TLS termination and re-encryption for MITM connections.
 *
 * Flow:
 * 1. Wrap client socket in an SSLServerSocket using the dynamically generated leaf cert.
 * 2. The client sees a valid certificate for the target domain.
 * 3. We read the decrypted HTTP request from the client.
 * 4. Open an upstream SSL connection to the real server.
 * 5. Forward the request upstream, receive response.
 * 6. Run response through TransformerEngine (strip ads from JSON/protobuf).
 * 7. Re-encrypt transformed response and send back to client.
 */
object MitmSocketHandler {

    private const val TAG = "Argus/Mitm"
    private const val BUFFER_SIZE = 8192

    @Suppress("CustomX509TrustManager")
    private val trustAll = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    }

    suspend fun handleMitmConnection(
        clientSocket: Socket,
        rawClientIn: InputStream,
        rawClientOut: OutputStream,
        host: String,
        port: Int,
        leafCert: X509Certificate,
        leafKey: PrivateKey,
        transformerEngine: TransformerEngine
    ) = withContext(Dispatchers.IO) {
        try {
            // Step 1: Create SSLServerSocket for the client using leaf cert
            val keyStore = java.security.KeyStore.getInstance(java.security.KeyStore.getDefaultType())
            keyStore.load(null, null)
            keyStore.setKeyEntry("leaf", leafKey, "argus".toCharArray(), arrayOf(leafCert))

            val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
            kmf.init(keyStore, "argus".toCharArray())

            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(kmf.keyManagers, arrayOf(trustAll), java.security.SecureRandom())

            val serverFactory: SSLServerSocketFactory = sslContext.serverSocketFactory
            val sslServerSocket = serverFactory.createServerSocket(0, 0, java.net.InetAddress.getByName("127.0.0.1"))
            val mitmPort = sslServerSocket.localPort

            // Handshake with client on a separate thread (SSLServerSocket.accept is blocking)
            val clientHandshakeThread = Thread {
                try {
                    sslServerSocket.use { ss ->
                        val sslClient = ss.accept() as SSLSocket
                        sslClient.startHandshake()

                        val sslClientIn = sslClient.getInputStream()
                        val sslClientOut = sslClient.getOutputStream()

                        // Step 3: Read decrypted HTTP request from client
                        val request = readHttpRequest(sslClientIn)
                        if (request == null) {
                            sslClient.close()
                            return@use
                        }

                        // Step 4: Connect upstream to real server
                        val upstreamResponseBytes = connectUpstreamAndGetResponse(
                            host, port, request, transformerEngine
                        )

                        // Step 7: Send transformed response back
                        if (upstreamResponseBytes != null) {
                            sslClientOut.write(upstreamResponseBytes)
                            sslClientOut.flush()
                        }

                        sslClient.shutdownOutput()
                        sslClient.close()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "MITM handshake thread error", e)
                }
            }
            clientHandshakeThread.start()

            // Step 2: Our side connects to the local SSLServerSocket as a client
            val clientSslContext = SSLContext.getInstance("TLS")
            clientSslContext.init(null, arrayOf(trustAll), java.security.SecureRandom())
            val clientFactory: SSLSocketFactory = clientSslContext.socketFactory

            clientFactory.createSocket("127.0.0.1", mitmPort).use { mitmClient ->
                (mitmClient as SSLSocket).startHandshake()
                // Pipe raw client data into MITM client
                pipeAll(rawClientIn, mitmClient.getOutputStream())
                mitmClient.shutdownOutput()
            }

            clientHandshakeThread.join(30000)
        } catch (e: Exception) {
            Log.w(TAG, "MITM connection error for $host", e)
        }
    }

    private fun connectUpstreamAndGetResponse(
        host: String,
        port: Int,
        request: HttpRequest,
        transformerEngine: TransformerEngine
    ): ByteArray? {
        return try {
            val upstreamSocket = SSLSocketFactory.getDefault().createSocket(host, port) as SSLSocket
            com.nexusblock.service.NexusVpnService.currentInstance?.protect(upstreamSocket)
            upstreamSocket.use { socket ->
                socket.startHandshake()
                val out = socket.getOutputStream()
                val ins = socket.getInputStream()

                // Write request upstream
                out.write(request.toBytes())
                out.flush()

                // Read full HTTP response
                val responseBytes = ins.readBytes()

                // Parse response, run transformer
                val parsed = parseHttpResponse(responseBytes) ?: return responseBytes
                val hostWithScheme = if (port == 443) "https://$host" else "https://$host:$port"
                val url = hostWithScheme + request.path

                val transformedBody = transformerEngine.transform(
                    url = url,
                    host = host,
                    requestPath = request.path,
                    contentType = parsed.contentType,
                    requestMethod = request.method,
                    body = parsed.body
                )

                if (transformedBody !== parsed.body) {
                    // Body was transformed — rebuild response
                    rebuildResponse(parsed, transformedBody)
                } else {
                    responseBytes
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Upstream connection error to $host", e)
            null
        }
    }

    private fun readHttpRequest(input: InputStream): HttpRequest? {
        val reader = BufferedReader(InputStreamReader(input, "ISO-8859-1"))
        val requestLine = reader.readLine() ?: return null
        val parts = requestLine.split(" ")
        if (parts.size < 3) return null

        val method = parts[0]
        val path = parts[1]
        val httpVersion = parts[2]

        val headers = mutableMapOf<String, String>()
        while (true) {
            val line = reader.readLine() ?: break
            if (line.isEmpty()) break
            val idx = line.indexOf(':')
            if (idx > 0) {
                headers[line.substring(0, idx).trim().lowercase()] = line.substring(idx + 1).trim()
            }
        }

        // Read body if Content-Length present
        val body = if (headers.containsKey("content-length")) {
            val len = headers["content-length"]?.toIntOrNull() ?: 0
            if (len > 0) {
                val bodyBytes = ByteArray(len)
                var totalRead = 0
                while (totalRead < len) {
                    val read = input.read(bodyBytes, totalRead, len - totalRead)
                    if (read <= 0) break
                    totalRead += read
                }
                bodyBytes
            } else null
        } else null

        return HttpRequest(method, path, httpVersion, headers, body)
    }

    private fun parseHttpResponse(data: ByteArray): ParsedResponse? {
        val headerEnd = findHeaderEnd(data) ?: return null
        val headerText = String(data, 0, headerEnd, Charset.defaultCharset())
        val lines = headerText.split("\r\n")
        if (lines.isEmpty()) return null

        val statusLine = lines[0]
        val headers = mutableMapOf<String, String>()
        for (i in 1 until lines.size) {
            val line = lines[i]
            if (line.isEmpty()) continue
            val idx = line.indexOf(':')
            if (idx > 0) {
                headers[line.substring(0, idx).trim().lowercase()] = line.substring(idx + 1).trim()
            }
        }

        val body = if (headerEnd + 4 < data.size) {
            data.copyOfRange(headerEnd + 4, data.size)
        } else null

        val contentType = headers["content-type"] ?: ""
        return ParsedResponse(statusLine, headers, contentType, body)
    }

    private fun rebuildResponse(parsed: ParsedResponse, newBody: ByteArray?): ByteArray {
        val out = ByteArrayOutputStream()
        out.write((parsed.statusLine + "\r\n").toByteArray())

        // Update Content-Length and remove Transfer-Encoding
        val skipKeys = setOf("content-length", "transfer-encoding", "content-encoding")
        parsed.headers.forEach { (k, v) ->
            if (k !in skipKeys) {
                out.write(("$k: $v\r\n").toByteArray())
            }
        }
        out.write("Content-Length: ${newBody?.size ?: 0}\r\n".toByteArray())
        out.write("Connection: close\r\n".toByteArray())
        out.write("\r\n".toByteArray())

        if (newBody != null) {
            out.write(newBody)
        }
        return out.toByteArray()
    }

    private fun findHeaderEnd(data: ByteArray): Int? {
        for (i in 0 until data.size - 3) {
            if (data[i] == '\r'.toByte() && data[i + 1] == '\n'.toByte() &&
                data[i + 2] == '\r'.toByte() && data[i + 3] == '\n'.toByte()
            ) {
                return i
            }
        }
        return null
    }

    private fun pipeAll(input: InputStream, output: OutputStream) {
        val buffer = ByteArray(BUFFER_SIZE)
        try {
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                output.write(buffer, 0, read)
            }
        } catch (_: Exception) {}
    }

    private data class HttpRequest(
        val method: String,
        val path: String,
        val httpVersion: String,
        val headers: Map<String, String>,
        val body: ByteArray?
    ) {
        fun toBytes(): ByteArray {
            val sb = StringBuilder()
            sb.append("$method $path $httpVersion\r\n")
            headers.forEach { (k, v) -> sb.append("$k: $v\r\n") }
            sb.append("\r\n")
            val headerBytes = sb.toString().toByteArray()
            return if (body != null) headerBytes + body else headerBytes
        }
    }

    private data class ParsedResponse(
        val statusLine: String,
        val headers: Map<String, String>,
        val contentType: String,
        val body: ByteArray?
    )
}
