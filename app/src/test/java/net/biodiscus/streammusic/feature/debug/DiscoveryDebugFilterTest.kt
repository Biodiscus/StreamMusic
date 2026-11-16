package net.biodiscus.streammusic.feature.debug

import net.biodiscus.streammusic.core.model.DeviceProtocol
import net.biodiscus.streammusic.testing.aDevice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoveryDebugFilterTest {

    private val chromecast = aDevice(protocol = DeviceProtocol.CHROMECAST)
    private val fireTv = aDevice(protocol = DeviceProtocol.FIRE_TV)
    private val sonos = aDevice(protocol = DeviceProtocol.SONOS)

    @Test
    fun `the default filter matches everything`() {
        assertTrue(DiscoveryDebugFilter().matches(chromecast))
        assertTrue(DiscoveryDebugFilter().matches(fireTv))
    }

    @Test
    fun `the implemented filter keeps only protocols the app can stream to`() {
        val filter = DiscoveryDebugFilter(implementation = ImplementationFilter.IMPLEMENTED)

        assertTrue(filter.matches(chromecast))
        assertFalse(filter.matches(fireTv))
    }

    @Test
    fun `the not implemented filter keeps only protocols the app cannot stream to`() {
        val filter = DiscoveryDebugFilter(implementation = ImplementationFilter.NOT_IMPLEMENTED)

        assertFalse(filter.matches(chromecast))
        assertTrue(filter.matches(fireTv))
    }

    @Test
    fun `a protocol filter keeps only the chosen protocols`() {
        val filter = DiscoveryDebugFilter(protocols = setOf(DeviceProtocol.CHROMECAST, DeviceProtocol.SONOS))

        assertTrue(filter.matches(chromecast))
        assertTrue(filter.matches(sonos))
        assertFalse(filter.matches(fireTv))
    }

    @Test
    fun `a find must match both the implementation and the protocol filter`() {
        val filter = DiscoveryDebugFilter(
            implementation = ImplementationFilter.NOT_IMPLEMENTED,
            protocols = setOf(DeviceProtocol.CHROMECAST),
        )

        assertFalse(filter.matches(chromecast))
    }

    @Test
    fun `toggling a protocol adds it`() {
        val filter = DiscoveryDebugFilter().withProtocolToggled(DeviceProtocol.SONOS)

        assertEquals(setOf(DeviceProtocol.SONOS), filter.protocols)
    }

    @Test
    fun `toggling a chosen protocol removes it`() {
        val filter = DiscoveryDebugFilter(protocols = setOf(DeviceProtocol.SONOS)).withProtocolToggled(DeviceProtocol.SONOS)

        assertEquals(emptySet<DeviceProtocol>(), filter.protocols)
    }
}
