package net.biodiscus.streammusic.core.discovery.upnp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpnpDescriptionParserTest {

    private val location = "http://192.168.1.20:1400/xml/device_description.xml"

    @Test
    fun `reads the root device names and manufacturer`() {
        val description = parseUpnpDescription(rendererXml, location)

        assertEquals("Living Room TV", description.friendlyName)
        assertEquals("Acme", description.manufacturer)
        assertEquals("AC-100", description.modelName)
    }

    @Test
    fun `finds the AVTransport control url and resolves it against the location`() {
        val description = parseUpnpDescription(rendererXml, location)

        assertEquals("http://192.168.1.20:1400/MediaRenderer/AVTransport/Control", description.avTransportControlUrl)
    }

    @Test
    fun `a device without AVTransport has no control url`() {
        val description = parseUpnpDescription(mediaServerXml, location)

        assertNull(description.avTransportControlUrl)
    }

    @Test
    fun `reads the Sonos room name`() {
        val description = parseUpnpDescription(rendererXml, location)

        assertEquals("Kitchen", description.roomName)
    }

    @Test
    fun `display name prefers the room name`() {
        val description = UpnpDescription("Friendly", "Kitchen", null, null, null)

        assertEquals("Kitchen", description.displayName(fallback = "10.0.0.1"))
    }

    @Test
    fun `display name falls back to the host when no name is known`() {
        val description = UpnpDescription(null, null, null, null, null)

        assertEquals("10.0.0.1", description.displayName(fallback = "10.0.0.1"))
    }

    private val rendererXml = """
        <?xml version="1.0"?>
        <root xmlns="urn:schemas-upnp-org:device-1-0">
          <device>
            <friendlyName>Living Room TV</friendlyName>
            <manufacturer>Acme</manufacturer>
            <modelName>AC-100</modelName>
            <roomName>Kitchen</roomName>
            <serviceList>
              <service>
                <serviceType>urn:schemas-upnp-org:service:RenderingControl:1</serviceType>
                <controlURL>/MediaRenderer/RenderingControl/Control</controlURL>
              </service>
            </serviceList>
            <deviceList>
              <device>
                <friendlyName>Embedded Renderer</friendlyName>
                <manufacturer>Embedded Maker</manufacturer>
                <serviceList>
                  <service>
                    <serviceType>urn:schemas-upnp-org:service:AVTransport:1</serviceType>
                    <controlURL>/MediaRenderer/AVTransport/Control</controlURL>
                  </service>
                </serviceList>
              </device>
            </deviceList>
          </device>
        </root>
    """.trimIndent()

    private val mediaServerXml = """
        <?xml version="1.0"?>
        <root><device>
          <friendlyName>NAS</friendlyName>
          <serviceList><service>
            <serviceType>urn:schemas-upnp-org:service:ContentDirectory:1</serviceType>
            <controlURL>/cd</controlURL>
          </service></serviceList>
        </device></root>
    """.trimIndent()
}
