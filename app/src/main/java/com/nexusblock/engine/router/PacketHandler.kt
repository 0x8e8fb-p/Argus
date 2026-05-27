package com.nexusblock.engine.router

import com.nexusblock.engine.PacketRouter
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

/**
 * Chain of Responsibility handler for packet routing.
 *
 * Each handler evaluates a packet and returns one of:
 * - [Action.HANDLED]  – packet was consumed (e.g. DNS response injected, dropped)
 * - [Action.PASS]     – handler declined, let next handler try
 *
 * Handlers MUST NOT throw unhandled exceptions; any error should result in
 * a safe fallback (fail-open or fail-closed as appropriate).
 */
interface PacketHandler {
    enum class Action { HANDLED, PASS }

    /**
     * Examine the packet and optionally take action.
     *
     * @param buffer      ByteBuffer positioned at start of IP packet
     * @param pos         absolute position in buffer where IP header starts
     * @param ipHeaderLen IP header length in bytes
     * @param protocol    IP protocol number (TCP=6, UDP=17, ICMP=1)
     * @param output      TUN output channel for injecting responses
     * @param router      the parent router (for stats, callbacks, scope)
     * @return [Action.HANDLED] if this handler consumed the packet; [Action.PASS] otherwise
     */
    fun handle(
        buffer: ByteBuffer,
        pos: Int,
        ipHeaderLen: Int,
        protocol: Int,
        output: FileChannel,
        router: PacketRouter
    ): Action
}
