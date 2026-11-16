package net.biodiscus.streammusic.core.discovery.ssdp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SsdpMessagesTest {

    @Test
    fun `search request asks for the given search target`() {
        val request = buildSsdpSearchRequest("urn:schemas-upnp-org:device:MediaRenderer:1")

        assertTrue(request.contains("ST: urn:schemas-upnp-org:device:MediaRenderer:1\r\n"))
    }

    @Test
    fun `search request is a multicast discover message`() {
        val request = buildSsdpSearchRequest("ssdp:all")

        assertTrue(request.startsWith("M-SEARCH * HTTP/1.1\r\n"))
        assertTrue(request.contains("MAN: \"ssdp:discover\"\r\n"))
        assertTrue(request.endsWith("\r\n\r\n"))
    }

    @Test
    fun `reads location and search target from a response`() {
        val response = parseSsdpResponse(sonosResponse)

        assertEquals("http://192.168.1.20:1400/xml/device_description.xml", response?.location)
        assertEquals("urn:schemas-upnp-org:device:ZonePlayer:1", response?.searchTarget)
    }

    @Test
    fun `header names are matched ignoring case`() {
        val response = parseSsdpResponse("HTTP/1.1 200 OK\r\nlocation: http://10.0.0.2/d.xml\r\nSt: x\r\n\r\n")

        assertEquals("http://10.0.0.2/d.xml", response?.location)
    }

    @Test
    fun `ignores messages that are not a successful response`() {
        val notify = "NOTIFY * HTTP/1.1\r\nLOCATION: http://10.0.0.2/d.xml\r\n\r\n"

        assertNull(parseSsdpResponse(notify))
    }

    @Test
    fun `ignores a response without location`() {
        assertNull(parseSsdpResponse("HTTP/1.1 200 OK\r\nST: x\r\n\r\n"))
    }

    private val sonosResponse = "HTTP/1.1 200 OK\r\n" +
        "CACHE-CONTROL: max-age = 1800\r\n" +
        "LOCATION: http://192.168.1.20:1400/xml/device_description.xml\r\n" +
        "ST: urn:schemas-upnp-org:device:ZonePlayer:1\r\n" +
        "USN: uuid:RINCON_000E58::urn:schemas-upnp-org:device:ZonePlayer:1\r\n\r\n"
}
