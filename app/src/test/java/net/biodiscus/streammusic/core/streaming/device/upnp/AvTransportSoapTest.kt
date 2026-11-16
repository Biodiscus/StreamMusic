package net.biodiscus.streammusic.core.streaming.device.upnp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AvTransportSoapTest {

    @Test
    fun `soap action header names the AVTransport service and action in quotes`() {
        assertEquals(
            "\"urn:schemas-upnp-org:service:AVTransport:1#Play\"",
            soapActionHeader("Play"),
        )
    }

    @Test
    fun `body wraps the action and always addresses instance 0`() {
        val body = buildSoapRequestBody("Stop", emptyList())

        assertTrue(body.contains("""<u:Stop xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">"""))
        assertTrue(body.contains("<InstanceID>0</InstanceID></u:Stop>"))
    }

    @Test
    fun `body contains the arguments in order`() {
        val body = buildSoapRequestBody("SetAVTransportURI", listOf("CurrentURI" to "http://a/b", "CurrentURIMetaData" to ""))

        assertTrue(body.contains("<CurrentURI>http://a/b</CurrentURI><CurrentURIMetaData></CurrentURIMetaData>"))
    }

    @Test
    fun `argument values are xml escaped`() {
        val body = buildSoapRequestBody("SetAVTransportURI", listOf("CurrentURI" to "http://a/b?x=1&y=<2>"))

        assertTrue(body.contains("<CurrentURI>http://a/b?x=1&amp;y=&lt;2&gt;</CurrentURI>"))
    }
}
