package net.biodiscus.streammusic.core.streaming.device.airplay2

import java.io.IOException
import java.nio.ByteBuffer
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.biodiscus.streammusic.core.model.DeviceProtocol
import net.biodiscus.streammusic.core.model.RaopServiceInfo
import net.biodiscus.streammusic.core.streaming.PcmFormat
import net.biodiscus.streammusic.core.streaming.device.SessionAudio
import net.biodiscus.streammusic.core.streaming.device.airplay.FakeRaopConnection
import net.biodiscus.streammusic.core.streaming.device.airplay.RaopConnector
import net.biodiscus.streammusic.core.streaming.device.airplay.RaopServerPorts
import net.biodiscus.streammusic.core.streaming.device.airplay.RtspRequest
import net.biodiscus.streammusic.core.streaming.device.airplay.RtspResponse
import net.biodiscus.streammusic.core.streaming.device.airplay.nonceFromCounter
import net.biodiscus.streammusic.core.streaming.device.airplay.openWithChaCha20Poly1305
import net.biodiscus.streammusic.testing.aDevice
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AirPlay2DeviceStreamerTest {

    private val connection = FakeRaopConnection()
    private val receiver = FakeAirPlay2Receiver()
    private val connector = RaopConnector { _, _ -> connection }
    private val device = aDevice(protocol = DeviceProtocol.AIRPLAY_2, host = "10.0.0.9")
        .copy(raop = RaopServiceInfo(setOf(4), needsPassword = false, supportsTransientPairing = true))

    init {
        connection.answerToRtsp = receiver::answer
    }

    @Test
    fun `pairs, sets up the session and the stream, and then starts recording`() = runTest {
        val job = startStreaming(frameCount = 0)

        val expected = listOf(
            "GET /info", "POST /pair-pin-start", "POST /pair-setup", "POST /pair-setup",
            "SETUP", "SETUP", "RECORD", "FLUSH",
        )
        assertEquals(expected, receiver.methodsAndUrls)
        job.cancelAndJoin()
    }

    @Test
    fun `encrypts the connection after pairing and before the first setup`() = runTest {
        var requestsWhenEncrypted = -1
        connection.answerToRtsp = { request ->
            if (request.method == "SETUP" && requestsWhenEncrypted == -1) requestsWhenEncrypted = connection.airPlay2Steps.size
            receiver.answer(request)
        }

        val job = startStreaming(frameCount = 0)

        assertEquals(1, requestsWhenEncrypted)
        assertEquals("encrypted", connection.airPlay2Steps.first())
        job.cancelAndJoin()
    }

    @Test
    fun `opens the event channel on the port of the session setup, before the stream setup`() = runTest {
        var stepsAtStreamSetup: List<String>? = null
        var setups = 0
        connection.answerToRtsp = { request ->
            if (request.method == "SETUP" && ++setups == 2) stepsAtStreamSetup = connection.airPlay2Steps.toList()
            receiver.answer(request)
        }

        val job = startStreaming(frameCount = 0)

        assertEquals(listOf("encrypted", "events:$EVENT_PORT"), stepsAtStreamSetup)
        job.cancelAndJoin()
    }

    @Test
    fun `answers the events of the receiver`() = runTest {
        val job = startStreaming(frameCount = 0)

        assertTrue(connection.answeredEvents)
        job.cancelAndJoin()
    }

    @Test
    fun `reads the event channel with the key that also encrypts the audio`() = runTest {
        val job = startStreaming(frameCount = 0)

        val sharedKey = sharedKeyOf(receiver.requests.filter { it.method == "SETUP" }[1])
        assertArrayEquals(sharedKey, connection.eventReceiveKey)
        assertFalse(sharedKey.contentEquals(connection.eventSendKey))
        job.cancelAndJoin()
    }

    @Test
    fun `writes and reads the connection with different keys`() = runTest {
        val job = startStreaming(frameCount = 0)

        assertFalse(connection.rtspSendKey.contentEquals(connection.rtspReceiveKey))
        job.cancelAndJoin()
    }

    @Test
    fun `tells the receiver the ports of the phone in the stream setup`() = runTest {
        val job = startStreaming(frameCount = 0)

        val streamSetup = receiver.requests.filter { it.method == "SETUP" }[1]
        val stream = ((decodeBinaryPlist(streamSetup.body ?: ByteArray(0)) as Map<*, *>)["streams"] as List<*>).single() as Map<*, *>
        assertEquals(connection.controlPort.toLong(), stream["controlPort"])
        job.cancelAndJoin()
    }

    @Test
    fun `tells the receiver where the timing server of the phone listens`() = runTest {
        val job = startStreaming(frameCount = 0)

        val sessionSetup = receiver.requests.first { it.method == "SETUP" }
        val body = decodeBinaryPlist(sessionSetup.body ?: ByteArray(0)) as Map<*, *>
        assertEquals(connection.timingPort.toLong(), body["timingPort"])
        job.cancelAndJoin()
    }

    @Test
    fun `sends audio to the ports of the stream`() = runTest {
        val job = startStreaming(frameCount = 0)

        assertEquals(RaopServerPorts(audioPort = AUDIO_PORT, controlPort = CONTROL_PORT, timingPort = null), connection.aimedAt)
        job.cancelAndJoin()
    }

    @Test
    fun `flushes from the first packet on`() = runTest {
        val job = startStreaming(frameCount = 0)

        val flush = receiver.requests.first { it.method == "FLUSH" }
        assertEquals("npt=0-", flush.headers["Range"])
        assertTrue(flush.headers.getValue("RTP-Info").startsWith("seq=1;rtptime="))
        job.cancelAndJoin()
    }

    @Test
    fun `reports streaming once recording has started`() = runTest {
        var streaming = false

        val job = startStreaming(frameCount = 0, onStreaming = { streaming = true })

        assertTrue(streaming)
        job.cancelAndJoin()
    }

    @Test
    fun `sends encrypted packets that the receiver can open with the key of the stream setup`() = runTest {
        val job = startStreaming(frameCount = 1)

        val sharedKey = sharedKeyOf(receiver.requests.filter { it.method == "SETUP" }[1])
        val packet = connection.audioPackets.first()
        val sealedAudio = packet.copyOfRange(12, packet.size - 8)
        val opened = openWithChaCha20Poly1305(sharedKey, nonceFromCounter(0), sealedAudio, packet.copyOfRange(4, 12))
        assertEquals(352 * 4, opened.size)
        job.cancelAndJoin()
    }

    @Test
    fun `sends a packet for every 352 frames of captured audio`() = runTest {
        val job = startStreaming(frameCount = 5)

        assertEquals(12, connection.audioPackets.size)
        job.cancelAndJoin()
    }

    @Test
    fun `marks the audio with the id of the stream`() = runTest {
        val job = startStreaming(frameCount = 1)

        val streamSetup = receiver.requests.filter { it.method == "SETUP" }[1]
        val stream = ((decodeBinaryPlist(streamSetup.body ?: ByteArray(0)) as Map<*, *>)["streams"] as List<*>).single() as Map<*, *>
        assertEquals((stream["streamConnectionID"] as Long).toInt(), ByteBuffer.wrap(connection.audioPackets.first()).getInt(8))
        job.cancelAndJoin()
    }

    @Test
    fun `sends feedback every two seconds`() = runTest {
        val job = startStreaming(frameCount = 0)

        advanceTimeBy(4_001)

        assertEquals(2, receiver.requests.count { it.url == "/feedback" })
        job.cancelAndJoin()
    }

    @Test
    fun `answers the timing requests of the receiver`() = runTest {
        val job = startStreaming(frameCount = 0)

        val timingRequest = ByteBuffer.allocate(32).putInt(0, 0x80D20007.toInt()).array()
        val answer = checkNotNull(connection.timingAnswerer).invoke(timingRequest)

        assertEquals(0xD3, checkNotNull(answer)[1].toInt() and 0xFF)
        job.cancelAndJoin()
    }

    @Test
    fun `ends the session and closes the connection when cancelled`() = runTest {
        val job = startStreaming(frameCount = 0)

        job.cancelAndJoin()

        assertEquals("TEARDOWN", receiver.requests.last().method)
        assertTrue(connection.closed)
    }

    @Test
    fun `fails when pairing is refused`() = runTest {
        connection.answerToRtsp = { request ->
            if (request.url == "/pair-setup") RtspResponse(470, "Connection Authorization Required", emptyMap()) else receiver.answer(request)
        }

        val result = runCatching { streamerFor(frameCount = 0).stream {} }

        assertEquals("The device refused to pair: 470 Connection Authorization Required", result.exceptionOrNull()?.message)
        assertTrue(connection.closed)
    }

    @Test
    fun `does not send a teardown when it never got to set up the session`() = runTest {
        connection.answerToRtsp = { request ->
            if (request.url == "/pair-setup") RtspResponse(500, "Error", emptyMap()) else receiver.answer(request)
        }

        runCatching { streamerFor(frameCount = 0).stream {} }

        assertFalse(connection.rtspMethods.contains("TEARDOWN"))
    }

    @Test
    fun `fails when the receiver refuses the stream setup`() = runTest {
        var setups = 0
        connection.answerToRtsp = { request ->
            if (request.method == "SETUP" && ++setups == 2) RtspResponse(500, "Internal Server Error", emptyMap()) else receiver.answer(request)
        }

        val result = runCatching { streamerFor(frameCount = 0).stream {} }

        assertEquals("The device refused SETUP: 500 Internal Server Error", result.exceptionOrNull()?.message)
    }

    @Test
    fun `fails when the receiver sends no event port`() = runTest {
        connection.answerToRtsp = { request ->
            if (request.method == "SETUP") RtspResponse(200, "OK", emptyMap(), encodeBinaryPlist(mapOf("other" to 1))) else receiver.answer(request)
        }

        val result = runCatching { streamerFor(frameCount = 0).stream {} }

        assertTrue(result.exceptionOrNull() is IOException)
    }

    private fun TestScope.startStreaming(frameCount: Int, onStreaming: () -> Unit = {}): Job {
        val job = launch { streamerFor(frameCount).stream(onStreaming) }
        runCurrent()
        return job
    }

    private fun streamerFor(frameCount: Int): AirPlay2DeviceStreamer {
        val pcmFrames = flow {
            repeat(frameCount) { emit(ByteArray(PcmFormat.BYTES_PER_FRAME)) }
            awaitCancellation()
        }
        return AirPlay2DeviceStreamer(device, SessionAudio(pcmFrames, aacStreamPort = null), connector) { 1_763_000_000_000 }
    }

    private fun sharedKeyOf(streamSetup: RtspRequest): ByteArray {
        val stream = ((decodeBinaryPlist(streamSetup.body ?: ByteArray(0)) as Map<*, *>)["streams"] as List<*>).single() as Map<*, *>
        return stream["shk"] as ByteArray
    }
}
