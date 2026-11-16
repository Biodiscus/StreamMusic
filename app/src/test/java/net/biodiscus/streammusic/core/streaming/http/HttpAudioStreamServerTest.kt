package net.biodiscus.streammusic.core.streaming.http

import net.biodiscus.streammusic.core.model.StreamAudioFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HttpAudioStreamServerTest {

    @Test
    fun `response announces an AAC audio stream`() {
        val headers = buildStreamResponseHeaders(StreamAudioFormat.AAC)

        assertTrue(headers.startsWith("HTTP/1.0 200 OK\r\n"))
        assertTrue(headers.contains("Content-Type: audio/aac\r\n"))
    }

    @Test
    fun `response announces a WAV audio stream`() {
        assertTrue(buildStreamResponseHeaders(StreamAudioFormat.WAV).contains("Content-Type: audio/wav\r\n"))
    }

    @Test
    fun `response announces the sample rate and channels of an LPCM stream`() {
        val headers = buildStreamResponseHeaders(StreamAudioFormat.LPCM)

        assertTrue(headers.contains("Content-Type: audio/L16;rate=48000;channels=2\r\n"))
    }

    @Test
    fun `response headers end with an empty line`() {
        assertTrue(buildStreamResponseHeaders(StreamAudioFormat.AAC).endsWith("\r\n\r\n"))
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
    fun `builds the AAC stream url from host and port`() {
        assertEquals("http://192.168.1.5:40000/stream.aac", streamUrl("192.168.1.5", 40000, StreamAudioFormat.AAC))
    }

    @Test
    fun `builds the WAV stream url from host and port`() {
        assertEquals("http://192.168.1.5:40000/stream.wav", streamUrl("192.168.1.5", 40000, StreamAudioFormat.WAV))
    }

    @Test
    fun `builds the LPCM stream url from host and port`() {
        assertEquals("http://192.168.1.5:40000/stream.l16", streamUrl("192.168.1.5", 40000, StreamAudioFormat.LPCM))
    }
}
