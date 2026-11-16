package net.biodiscus.streammusic.core.streaming.aac

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AacHttpStreamTest {

    @Test
    fun `response announces an AAC audio stream`() {
        val headers = buildStreamResponseHeaders()

        assertTrue(headers.startsWith("HTTP/1.0 200 OK\r\n"))
        assertTrue(headers.contains("Content-Type: audio/aac\r\n"))
    }

    @Test
    fun `response headers end with an empty line`() {
        assertTrue(buildStreamResponseHeaders().endsWith("\r\n\r\n"))
    }

    @Test
    fun `recognizes a HEAD request`() {
        assertTrue(isHeadRequest("HEAD /stream.aac HTTP/1.1"))
    }

    @Test
    fun `a GET request is not a HEAD request`() {
        assertFalse(isHeadRequest("GET /stream.aac HTTP/1.1"))
    }

    @Test
    fun `builds the stream url from host and port`() {
        assertEquals("http://192.168.1.5:40000/stream.aac", streamUrl("192.168.1.5", 40000))
    }
}
