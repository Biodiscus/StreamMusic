package net.biodiscus.streammusic.core.model

import net.biodiscus.streammusic.testing.aDevice
import net.biodiscus.streammusic.testing.aNetworkDevice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkDeviceTest {

    private val host = "10.0.0.2"
    private val chromecast = aDevice(name = "Living room TV", protocol = DeviceProtocol.CHROMECAST, host = host)
    private val dlna = aDevice(name = "Samsung TV", protocol = DeviceProtocol.DLNA, host = host)
    private val sonos = aDevice(name = "Kitchen", protocol = DeviceProtocol.SONOS, host = host)
    private val airPlay = aDevice(name = "Apple TV", protocol = DeviceProtocol.AIRPLAY, host = host)

    @Test
    fun `streams with Chromecast when the device also speaks DLNA`() {
        val device = aNetworkDevice(dlna, chromecast)

        assertEquals(chromecast, device.bestStreamingOption)
    }

    @Test
    fun `streams with Sonos when the device also speaks Chromecast`() {
        val device = aNetworkDevice(chromecast, sonos)

        assertEquals(sonos, device.bestStreamingOption)
    }

    @Test
    fun `is named after the option it streams with`() {
        val device = aNetworkDevice(dlna, chromecast)

        assertEquals("Living room TV", device.name)
    }

    @Test
    fun `shows every protocol it can stream with, best first`() {
        val device = aNetworkDevice(dlna, airPlay, chromecast)

        assertEquals(listOf(DeviceProtocol.CHROMECAST, DeviceProtocol.DLNA), device.protocols)
    }

    @Test
    fun `can stream when at least one protocol is supported`() {
        assertTrue(aNetworkDevice(airPlay, dlna).canReceiveStream)
    }

    @Test
    fun `cannot stream when no protocol is supported`() {
        val device = aNetworkDevice(airPlay)

        assertFalse(device.canReceiveStream)
        assertNull(device.bestStreamingOption)
    }

    @Test
    fun `shows the unsupported protocols when none can stream`() {
        assertEquals(listOf(DeviceProtocol.AIRPLAY), aNetworkDevice(airPlay).protocols)
    }

    @Test
    fun `finds with the same host become one device`() {
        val study = aDevice(name = "Study", protocol = DeviceProtocol.SONOS, host = "10.0.0.3")

        val devices = listOf(dlna, study, chromecast).groupIntoNetworkDevices()

        assertEquals(listOf(aNetworkDevice(dlna, chromecast), aNetworkDevice(study)).toSet(), devices.toSet())
    }

    @Test
    fun `sorts devices by name ignoring case`() {
        val attic = aDevice(name = "attic", host = "10.0.0.3")
        val bedroom = aDevice(name = "Bedroom", host = "10.0.0.4")

        val devices = listOf(bedroom, attic).groupIntoNetworkDevices()

        assertEquals(listOf("attic", "Bedroom"), devices.map { it.name })
    }
}
