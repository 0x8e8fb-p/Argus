package com.nexusblock.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class VpnRoutingModeTest {

    @Test
    fun fromStorageKeyParsesKnownModes() {
        assertEquals(VpnRoutingMode.DNS_ONLY, VpnRoutingMode.fromStorageKey("dns_only"))
        assertEquals(VpnRoutingMode.FULL_ROUTE_SAFE, VpnRoutingMode.fromStorageKey("full_route_safe"))
        assertEquals(VpnRoutingMode.FULL_ROUTE_AGGRESSIVE, VpnRoutingMode.fromStorageKey("full_route_aggressive"))
    }

    @Test
    fun fromStorageKeyDefaultsToCurrentAggressiveBehavior() {
        assertEquals(VpnRoutingMode.FULL_ROUTE_AGGRESSIVE, VpnRoutingMode.fromStorageKey(null))
        assertEquals(VpnRoutingMode.FULL_ROUTE_AGGRESSIVE, VpnRoutingMode.fromStorageKey("unknown"))
    }
}