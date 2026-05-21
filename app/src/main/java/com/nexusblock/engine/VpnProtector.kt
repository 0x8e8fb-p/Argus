package com.nexusblock.engine

import android.net.VpnService
import com.nexusblock.service.NexusVpnService
import java.net.DatagramSocket
import java.net.Socket

/**
 * Utility to protect sockets from being routed back into the VPN tunnel.
 * Must be called on any socket that communicates with the real internet
 * when full-tunnel mode is active.
 */
object VpnProtector {

    /**
     * Protect a [Socket] so its traffic bypasses the VPN and uses the
     * underlying physical network instead.
     */
    fun protect(socket: Socket): Boolean {
        return try {
            NexusVpnService.currentInstance?.protect(socket) == true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Protect a [DatagramSocket] so its traffic bypasses the VPN.
     */
    fun protect(socket: DatagramSocket): Boolean {
        return try {
            NexusVpnService.currentInstance?.protect(socket) == true
        } catch (_: Exception) {
            false
        }
    }
}
