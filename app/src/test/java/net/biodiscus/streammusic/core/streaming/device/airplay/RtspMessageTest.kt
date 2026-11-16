package net.biodiscus.streammusic.core.streaming.device.airplay

import java.io.ByteArrayInputStream
import java.io.IOException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class RtspMessageTest {

    @Test
    fun `encodes a request without a body`() {
        val request = RtspRequest("RECORD", "rtsp://10.0.0.1/123", mapOf("Range" to "npt=0-"))

        val encoded = encodeRtspRequest(request, sequenceNumber = 4, userAgent = "Phone/1").decodeToString()

        assertEquals(
            "RECORD rtsp://10.0.0.1/123 RTSP/1.0\r\nRange: npt=0-\r\nCSeq: 4\r\nUser-Agent: Phone/1\r\n\r\n",
            encoded,
        )
    }

    @Test
    fun `encodes a request with a body and its length`() {
        val request = RtspRequest("ANNOUNCE", "rtsp://10.0.0.1/123", body = "v=0\r\n".toByteArray())

        val encoded = encodeRtspRequest(request, sequenceNumber = 1, userAgent = "Phone/1").decodeToString()

        assertTrue(encoded.contains("Content-Length: 5\r\n"))
        assertTrue(encoded.endsWith("\r\n\r\nv=0\r\n"))
    }

    @Test
    fun `reads the status and the headers of a response`() {
        val response = readRtspResponse(streamOf("RTSP/1.0 200 OK\r\nCSeq: 1\r\nSession: DEADBEEF\r\n\r\n"))

        assertEquals(200, response.statusCode)
        assertEquals("OK", response.reason)
        assertEquals("DEADBEEF", response.header("Session"))
    }

    @Test
    fun `header names are not case sensitive`() {
        val response = readRtspResponse(streamOf("RTSP/1.0 200 OK\r\naudio-latency: 11025\r\n\r\n"))

        assertEquals("11025", response.header("Audio-Latency"))
    }

    @Test
    fun `a missing header is null`() {
        val response = readRtspResponse(streamOf("RTSP/1.0 200 OK\r\n\r\n"))

        assertNull(response.header("Session"))
    }

    @Test
    fun `only a 200 answer is a success`() {
        val response = readRtspResponse(streamOf("RTSP/1.0 453 Not Enough Bandwidth\r\n\r\n"))

        assertFalse(response.isSuccess)
        assertEquals("Not Enough Bandwidth", response.reason)
    }

    @Test
    fun `skips the body and stops at the next response`() {
        val input = streamOf("RTSP/1.0 200 OK\r\nContent-Length: 4\r\n\r\nbodyRTSP/1.0 404 Not Found\r\n\r\n")

        readRtspResponse(input)
        val second = readRtspResponse(input)

        assertEquals(404, second.statusCode)
    }

    @Test
    fun `fails when the device closes the connection`() {
        try {
            readRtspResponse(streamOf("RTSP/1.0 200 OK\r\nCSeq"))
            fail("Expected an IOException")
        } catch (expected: IOException) {
            assertEquals("The device closed the connection", expected.message)
        }
    }

    @Test
    fun `fails on an answer that is not RTSP`() {
        try {
            readRtspResponse(streamOf("hello\r\n\r\n"))
            fail("Expected an IOException")
        } catch (expected: IOException) {
            assertTrue(expected.message.orEmpty().startsWith("Not an RTSP response"))
        }
    }

    @Test
    fun `an HTTP request has no sequence number`() {
        val request = RtspRequest("POST", "/pair-setup", protocol = HTTP_PROTOCOL)

        val encoded = encodeRtspRequest(request, sequenceNumber = 4, userAgent = "Phone/1").decodeToString()

        assertTrue(encoded.startsWith("POST /pair-setup HTTP/1.1\r\n"))
        assertFalse(encoded.contains("CSeq"))
    }

    @Test
    fun `keeps the user agent of the request`() {
        val request = RtspRequest("GET", "/info", mapOf("user-agent" to "AirPlay/550.10"))

        val encoded = encodeRtspRequest(request, sequenceNumber = 1, userAgent = "Phone/1").decodeToString()

        assertTrue(encoded.contains("user-agent: AirPlay/550.10\r\n"))
        assertFalse(encoded.contains("Phone/1"))
    }

    @Test
    fun `encodes the bytes of a binary body unchanged`() {
        val body = byteArrayOf(0, 1, 2, 0xFF.toByte())
        val request = RtspRequest("SETUP", "rtsp://10.0.0.1/1", body = body)

        val encoded = encodeRtspRequest(request, sequenceNumber = 1, userAgent = "Phone/1")

        assertArrayEquals(body, encoded.copyOfRange(encoded.size - 4, encoded.size))
    }

    @Test
    fun `keeps the body of a response`() {
        val response = readRtspResponse(streamOf("RTSP/1.0 200 OK\r\nContent-Length: 4\r\n\r\nbody"))

        assertEquals("body", String(response.body))
    }

    @Test
    fun `reads an HTTP response like an RTSP response`() {
        val response = readRtspResponse(streamOf("HTTP/1.1 200 OK\r\nContent-Length: 0\r\n\r\n"))

        assertTrue(response.isSuccess)
    }

    @Test
    fun `reads a request of the receiver`() {
        val request = readRtspRequest(streamOf("POST /command RTSP/1.0\r\nCSeq: 3\r\nContent-Length: 2\r\n\r\nhi"))

        assertEquals("POST", request.method)
        assertEquals("/command", request.url)
        assertEquals("RTSP/1.0", request.protocol)
        assertEquals("hi", String(request.body ?: ByteArray(0)))
    }

    @Test
    fun `fails on a request that is not RTSP`() {
        try {
            readRtspRequest(streamOf("nonsense\r\n\r\n"))
            fail("Expected an IOException")
        } catch (expected: IOException) {
            assertTrue(expected.message.orEmpty().startsWith("Not an RTSP request"))
        }
    }

    @Test
    fun `answers an event with an empty positive answer that repeats its sequence number`() {
        val request = RtspRequest("POST", "/command", mapOf("CSeq" to "3", "Server" to "AirTunes/1"), protocol = "RTSP/1.0")

        val answer = encodeEventResponse(request).decodeToString()

        assertEquals("RTSP/1.0 200 OK\r\nContent-Length: 0\r\nAudio-Latency: 0\r\nServer: AirTunes/1\r\nCSeq: 3\r\n\r\n", answer)
    }

    private fun streamOf(text: String) = ByteArrayInputStream(text.toByteArray())
}
