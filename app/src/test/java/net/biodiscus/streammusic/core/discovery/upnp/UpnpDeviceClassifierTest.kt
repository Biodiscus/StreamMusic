package net.biodiscus.streammusic.core.discovery.upnp

import net.biodiscus.streammusic.core.model.DeviceProtocol
import org.junit.Assert.assertEquals
import org.junit.Test

class UpnpDeviceClassifierTest {

    private val rendererTarget = "urn:schemas-upnp-org:device:MediaRenderer:1"

    @Test
    fun `device that answers the ZonePlayer search is Sonos`() {
        val protocol = classifyUpnpDevice("urn:schemas-upnp-org:device:ZonePlayer:1", description())

        assertEquals(DeviceProtocol.SONOS, protocol)
    }

    @Test
    fun `device made by Sonos is Sonos`() {
        val protocol = classifyUpnpDevice(rendererTarget, description(manufacturer = "Sonos, Inc."))

        assertEquals(DeviceProtocol.SONOS, protocol)
    }

    @Test
    fun `device that answers the Denon search is HEOS`() {
        val protocol = classifyUpnpDevice("urn:schemas-denon-com:device:ACT-Denon:1", description())

        assertEquals(DeviceProtocol.HEOS, protocol)
    }

    @Test
    fun `device with HEOS in its model name is HEOS`() {
        val protocol = classifyUpnpDevice(rendererTarget, description(modelName = "HEOS 7"))

        assertEquals(DeviceProtocol.HEOS, protocol)
    }

    @Test
    fun `Samsung WAM speaker is Samsung Multiroom`() {
        val protocol = classifyUpnpDevice(rendererTarget, description("Samsung Electronics", "WAM3501"))

        assertEquals(DeviceProtocol.SAMSUNG_MULTIROOM, protocol)
    }

    @Test
    fun `Samsung TV is plain DLNA`() {
        val protocol = classifyUpnpDevice(rendererTarget, description("Samsung Electronics", "UE55TU8000"))

        assertEquals(DeviceProtocol.DLNA, protocol)
    }

    @Test
    fun `unknown renderer is plain DLNA`() {
        val protocol = classifyUpnpDevice(rendererTarget, description("Acme", "AC-100"))

        assertEquals(DeviceProtocol.DLNA, protocol)
    }

    private fun description(manufacturer: String? = null, modelName: String? = null) =
        UpnpDescription(
            friendlyName = "Device",
            roomName = null,
            manufacturer = manufacturer,
            modelName = modelName,
            avTransportControlUrl = "http://10.0.0.2/control",
        )
}
