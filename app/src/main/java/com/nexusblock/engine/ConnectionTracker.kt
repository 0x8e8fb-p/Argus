package com.nexusblock.engine

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.os.Process
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.FileReader
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Maps network connections to their owning app packages.
 *
 * On Android 10+ (API 29+): Uses ConnectivityManager.getConnectionOwnerUid()
 * On Android 9 and below: Parses /proc/net/tcp and /proc/net/udp
 *
 * Heavily inspired by RethinkDNS and NetGuard implementations.
 */
@Singleton
class ConnectionTracker @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "NexusBlock/ConnTrack"
        private const val REFRESH_INTERVAL_MS = 5000L

        // procfs paths
        private const val PROC_TCP = "/proc/net/tcp"
        private const val PROC_TCP6 = "/proc/net/tcp6"
        private const val PROC_UDP = "/proc/net/udp"
        private const val PROC_UDP6 = "/proc/net/udp6"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var refreshJob: Job? = null

    // Connection table: key = "localIp:localPort:remoteIp:remotePort:proto"
    // value = owning UID
    private val connectionTable = mutableMapOf<String, Int>()

    // UID to package name cache
    private val uidToPackage = mutableMapOf<Int, String>()

    // Package manager reference
    private val pm = context.packageManager

    // Whether we can use the modern API
    private val useModernApi = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    @Volatile
    var isRunning = false
        private set

    fun start() {
        if (isRunning) return
        isRunning = true

        // Pre-populate UID cache for known apps
        preloadUidCache()

        // Periodically refresh procfs table on older devices
        if (!useModernApi) {
            refreshJob = scope.launch {
                while (isActive) {
                    refreshConnectionTable()
                    delay(REFRESH_INTERVAL_MS)
                }
            }
        }

        Log.i(TAG, "Connection tracker started (modern API: $useModernApi)")
    }

    fun stop() {
        isRunning = false
        refreshJob?.cancel()
        synchronized(connectionTable) {
            connectionTable.clear()
        }
        Log.i(TAG, "Connection tracker stopped")
    }

    /**
     * Get the package name for a given connection tuple.
     * Returns null if unknown or system process.
     */
    fun getPackageForConnection(
        localAddr: InetAddress,
        localPort: Int,
        remoteAddr: InetAddress,
        remotePort: Int,
        protocol: String
    ): String? {
        val uid = if (useModernApi) {
            getUidModern(localAddr, localPort, remoteAddr, remotePort, protocol)
        } else {
            getUidFromTable(localAddr, localPort, remoteAddr, remotePort, protocol)
        }

        return uid?.let { resolveUidToPackage(it) }
    }

    private fun getUidModern(
        localAddr: InetAddress,
        localPort: Int,
        remoteAddr: InetAddress,
        remotePort: Int,
        protocol: String
    ): Int? {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val transport = if (protocol.equals("tcp", ignoreCase = true))
                java.net.StandardProtocolFamily.INET
            else
                java.net.StandardProtocolFamily.INET

            // getConnectionOwnerUid was added in API 29
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val localSockAddr = java.net.InetSocketAddress(localAddr, localPort)
                val remoteSockAddr = java.net.InetSocketAddress(remoteAddr, remotePort)
                val uid = cm?.getConnectionOwnerUid(
                    if (protocol.equals("tcp", ignoreCase = true))
                        android.system.OsConstants.IPPROTO_TCP
                    else
                        android.system.OsConstants.IPPROTO_UDP,
                    localSockAddr,
                    remoteSockAddr
                )
                if (uid != null && uid != Process.INVALID_UID) uid else null
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "Modern API lookup failed", e)
            null
        }
    }

    private fun getUidFromTable(
        localAddr: InetAddress,
        localPort: Int,
        remoteAddr: InetAddress,
        remotePort: Int,
        protocol: String
    ): Int? {
        val localHex = ipToHex(localAddr)
        val remoteHex = ipToHex(remoteAddr)
        val localPortHex = String.format("%04X", localPort)
        val remotePortHex = String.format("%04X", remotePort)

        val procFile = when {
            protocol.equals("tcp", ignoreCase = true) && localAddr.address.size == 16 -> PROC_TCP6
            protocol.equals("tcp", ignoreCase = true) -> PROC_TCP
            protocol.equals("udp", ignoreCase = true) && localAddr.address.size == 16 -> PROC_UDP6
            else -> PROC_UDP
        }

        synchronized(connectionTable) {
            // Try exact lookup first
            val exactKey = "$localHex:$localPortHex:$remoteHex:$remotePortHex:$protocol"
            connectionTable[exactKey]?.let { return it }

            // Try local-only lookup (connection may have been closed)
            val localKey = "$localHex:$localPortHex:*:*:$protocol"
            connectionTable[localKey]?.let { return it }
        }

        // Fallback: read procfs directly for this connection
        return readProcForConnection(procFile, localHex, localPortHex, remoteHex, remotePortHex)
    }

    private fun refreshConnectionTable() {
        val files = listOf(PROC_TCP, PROC_UDP, PROC_TCP6, PROC_UDP6)
        synchronized(connectionTable) {
            connectionTable.clear()
            for (file in files) {
                readProcFile(file)
            }
        }
    }

    private fun readProcFile(path: String) {
        try {
            BufferedReader(FileReader(path)).use { reader ->
                // Skip header line
                reader.readLine()

                reader.lineSequence().forEach { line ->
                    val parts = line.trim().split(Regex("\\s+"))
                    if (parts.size < 10) return@forEach

                    val local = parts[1]
                    val rem = parts[2]
                    val uid = parts[8].toIntOrNull() ?: return@forEach

                    // Skip system/server processes unless explicitly tracked
                    if (uid < 10000) return@forEach

                    val proto = when {
                        path.contains("tcp") -> "tcp"
                        path.contains("udp") -> "udp"
                        else -> "unknown"
                    }

                    val key = "$local:$rem:$proto"
                    connectionTable[key] = uid

                    // Also index by local address only (for lookup after remote closes)
                    val localOnly = local.substringBefore(":") + ":" +
                            local.substringAfter(":") + ":*:*:$proto"
                    connectionTable[localOnly] = uid
                }
            }
        } catch (e: Exception) {
            // /proc/net may not be accessible on some devices
            Log.w(TAG, "Cannot read $path", e)
        }
    }

    private fun readProcForConnection(
        procFile: String,
        localHex: String,
        localPortHex: String,
        remoteHex: String,
        remotePortHex: String
    ): Int? {
        return try {
            val searchKey = "$localHex:$localPortHex $remoteHex:$remotePortHex"
            BufferedReader(FileReader(procFile)).use { reader ->
                reader.readLine() // skip header
                reader.lineSequence().mapNotNull { line ->
                    if (line.contains(searchKey)) {
                        val parts = line.trim().split(Regex("\\s+"))
                        parts.getOrNull(8)?.toIntOrNull()
                    } else null
                }.firstOrNull()
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun resolveUidToPackage(uid: Int): String? {
        // Check cache first
        uidToPackage[uid]?.let { return it }

        return try {
            val packages = pm.getPackagesForUid(uid)
            val pkgName = packages?.firstOrNull()
            if (pkgName != null) {
                uidToPackage[uid] = pkgName
            }
            pkgName
        } catch (e: Exception) {
            null
        }
    }

    private fun preloadUidCache() {
        try {
            val installed = pm.getInstalledApplications(0)
            for (app in installed) {
                try {
                    uidToPackage[app.uid] = app.packageName
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to preload UID cache", e)
        }
    }

    private fun ipToHex(addr: InetAddress): String {
        val bytes = addr.address
        return if (bytes.size == 4) {
            // IPv4 in procfs is stored as little-endian hex
            val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            String.format("%08X", buf.int)
        } else {
            // IPv6: 32 hex chars
            bytes.joinToString("") { "%02X".format(it) }
        }
    }
}
