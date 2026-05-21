package com.nexusblock.engine

import java.net.InetAddress
import java.net.Socket
import javax.net.SocketFactory

/**
 * A [SocketFactory] that protects every created [Socket] from being routed
 * back into the VPN tunnel by calling [VpnProtector.protect].
 *
 * This is used for OkHttp so that upstream DoH / blocklist download requests
 * bypass the TUN interface and avoid being blocked by our own SNI inspection.
 */
class VpnProtectedSocketFactory : SocketFactory() {

    private val delegate = getDefault()

    override fun createSocket(): Socket = delegate.createSocket().also { VpnProtector.protect(it) }
    override fun createSocket(host: String?, port: Int): Socket =
        delegate.createSocket(host, port).also { VpnProtector.protect(it) }
    override fun createSocket(host: String?, port: Int, localHost: InetAddress?, localPort: Int): Socket =
        delegate.createSocket(host, port, localHost, localPort).also { VpnProtector.protect(it) }
    override fun createSocket(host: InetAddress?, port: Int): Socket =
        delegate.createSocket(host, port).also { VpnProtector.protect(it) }
    override fun createSocket(address: InetAddress?, port: Int, localAddress: InetAddress?, localPort: Int): Socket =
        delegate.createSocket(address, port, localAddress, localPort).also { VpnProtector.protect(it) }
}
