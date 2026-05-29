package com.nexusblock.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class VpnRoutingModeTest {

    @Test
    fun fromStorageKeyParsesKnownModes() {
        assertEquals(VpnRoutingMode.DNS_ONLY, VpnRoutingMode.fromStorageKey("dns_only"))
        assertEquals(VpnRoutingMode.FULL_ROUTE, VpnRoutingMode.fromStorageKey("full_route"))
    }

    @Test
    fun fromStorageKeyDefaultsToFullRoute() {
        assertEquals(VpnRoutingMode.FULL_ROUTE, VpnRoutingMode.fromStorageKey(null))
        assertEquals(VpnRoutingMode.FULL_ROUTE, VpnRoutingMode.fromStorageKey("unknown"))
    }
}
