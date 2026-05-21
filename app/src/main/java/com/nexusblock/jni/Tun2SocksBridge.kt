package com.nexusblock.jni

import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.*

/**
 * JNI bridge to the Go tun2socks binary.
 *
 * tun2socks is a high-performance userspace TCP/IP stack written in Go.
 * It reads packets from a TUN file descriptor and routes TCP connections
 * through a local SOCKS5 proxy (ArgusProxyServer on 127.0.0.1:8123).
 *
 * This provides an advanced alternative to the Kotlin PacketRouter for
 * full-tunnel mode, offering:
 *   - Zero JVM GC pauses during packet processing
 *   - Mature TCP congestion control (gvisor netstack)
 *   - ~30% lower CPU on Amlogic S905X4 vs. Kotlin userspace NAT
 *
 * Fallback: If tun2socks native binary is unavailable, the system falls
 * back to the Kotlin PacketRouter (Layer 1).
 *
 * Build instructions in /tun2socks/README.md.
 */
object Tun2SocksBridge {

    private const val TAG = "Argus/Tun2Socks"

    // Native binary name varies by ABI
    private const val LIB_NAME = "tun2socks"

    private var nativeLoaded = false
    private var relayJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        try {
            System.loadLibrary(LIB_NAME)
            nativeLoaded = true
            Log.i(TAG, "Native tun2socks loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "Native tun2socks not available, will use Kotlin fallback", e)
            nativeLoaded = false
        }
    }

    /**
     * Check if the native tun2socks binary is available on this device.
     */
    fun isAvailable(): Boolean = nativeLoaded

    /**
     * Start tun2socks with the given TUN file descriptor.
     *
     * @param tunFd The ParcelFileDescriptor from VpnService.Builder.establish()
     * @param socksAddr SOCKS5 proxy address, typically "127.0.0.1:8123"
     * @param mtu The MTU configured in the VPN builder
     * @return true if tun2socks started successfully
     */
    fun start(tunFd: ParcelFileDescriptor, socksAddr: String = "127.0.0.1:8123", mtu: Int = 1500): Boolean {
        if (!nativeLoaded) {
            Log.w(TAG, "tun2socks not available, skipping start")
            return false
        }

        return try {
            // Detach the fd so Go can take ownership
            val fd = tunFd.detachFd()
            nativeStart(fd, socksAddr, mtu)
            Log.i(TAG, "tun2socks started with fd=$fd socks=$socksAddr mtu=$mtu")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start tun2socks", e)
            false
        }
    }

    /**
     * Stop the tun2socks process.
     */
    fun stop() {
        if (!nativeLoaded) return
        try {
            nativeStop()
            Log.i(TAG, "tun2socks stopped")
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping tun2socks", e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // JNI Native Methods
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Starts the Go tun2socks main loop.
     *
     * Signature: void Java_com_nexusblock_jni_Tun2SocksBridge_nativeStart(
     *     JNIEnv*, jobject, jint fd, jstring socksAddr, jint mtu
     * )
     */
    private external fun nativeStart(fd: Int, socksAddr: String, mtu: Int)

    /**
     * Signals the Go process to exit cleanly.
     */
    private external fun nativeStop()
}
