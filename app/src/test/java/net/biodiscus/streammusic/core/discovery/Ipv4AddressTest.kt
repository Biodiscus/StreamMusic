package net.biodiscus.streammusic.core.discovery

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Ipv4AddressTest {

    @Test
    fun `accepts a normal address`() {
        assertTrue(isIpv4Address("192.168.1.20"))
    }

    @Test
    fun `rejects an octet above 255`() {
        assertFalse(isIpv4Address("192.168.1.256"))
    }

    @Test
    fun `rejects an IPv6 address`() {
        assertFalse(isIpv4Address("fe80::1"))
    }

    @Test
    fun `rejects a host name`() {
        assertFalse(isIpv4Address("speaker.local"))
    }
}
