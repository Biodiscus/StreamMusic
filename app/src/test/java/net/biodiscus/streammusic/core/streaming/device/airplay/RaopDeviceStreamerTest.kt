package net.biodiscus.streammusic.core.streaming.device.airplay

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
import net.biodiscus.streammusic.core.model.StreamingDevice
import net.biodiscus.streammusic.core.streaming.PcmFormat
import net.biodiscus.streammusic.core.streaming.device.SessionAudio
import net.biodiscus.streammusic.testing.aDevice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RaopDeviceStreamerTest {

    private val connection = FakeRaopConnection()
    private val connector = RaopConnector { _, _ -> connection }
    private val clearAudioDevice = aDevice(protocol = DeviceProtocol.AIRPLAY, host = "10.0.0.9")
    private val encryptedAudioDevice = clearAudioDevice.copy(raop = RaopServiceInfo(setOf(0, 1), needsPassword = false))

    @Test
    fun `sets the session up in the order the receiver expects`() = runTest {
        val job = startStreaming(frameCount = 0)

        assertEquals(listOf("ANNOUNCE", "SETUP", "RECORD"), connection.rtspMethods)
        job.cancelAndJoin()
    }

    @Test
    fun `describes the stream to the receiver`() = runTest {
        val job = startStreaming(frameCount = 0)

        val description = connection.rtspRequests.first().bodyText()
        assertTrue(description.contains("c=IN IP4 10.0.0.9"))
        assertTrue(description.contains("a=rtpmap:96 AppleLossless"))
        job.cancelAndJoin()
    }

    @Test
    fun `does not encrypt for a receiver that accepts clear audio`() = runTest {
        val job = startStreaming(frameCount = 0)

        assertFalse(connection.rtspRequests.first().bodyText().contains("rsaaeskey"))
        job.cancelAndJoin()
    }

    @Test
    fun `encrypts for a receiver that accepts RSA encryption`() = runTest {
        val job = startStreaming(frameCount = 0, device = encryptedAudioDevice)

        assertTrue(connection.rtspRequests.first().bodyText().contains("a=rsaaeskey:"))
        job.cancelAndJoin()
    }

    @Test
    fun `tells the receiver the ports of the phone`() = runTest {
        val job = startStreaming(frameCount = 0)

        val setup = connection.rtspRequests.first { request -> request.method == "SETUP" }
        assertTrue(setup.headers.getValue("Transport").contains("control_port=50001;timing_port=50002"))
        job.cancelAndJoin()
    }

    @Test
    fun `sends the receiver session with every request after setup`() = runTest {
        val job = startStreaming(frameCount = 0)

        val record = connection.rtspRequests.first { request -> request.method == "RECORD" }
        assertEquals("ABC", record.headers["Session"])
        job.cancelAndJoin()
    }

    @Test
    fun `sends audio to the ports the receiver named`() = runTest {
        val job = startStreaming(frameCount = 0)

        assertEquals(RaopServerPorts(audioPort = 6000, controlPort = 6001, timingPort = 6002), connection.aimedAt)
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
    fun `sends a packet for every 352 frames of captured audio`() = runTest {
        val job = startStreaming(frameCount = 5)

        assertEquals(12, connection.audioPackets.size)
        job.cancelAndJoin()
    }

    @Test
    fun `answers the timing requests of the receiver`() = runTest {
        val job = startStreaming(frameCount = 0)

        val timingRequest = ByteBuffer.allocate(32).putInt(0x80D20007.toInt()).array()
        val answer = checkNotNull(connection.timingAnswerer).invoke(timingRequest)

        assertEquals(0xD3, checkNotNull(answer)[1].toInt() and 0xFF)
        job.cancelAndJoin()
    }

    @Test
    fun `sends a sync packet every second`() = runTest {
        val job = startStreaming(frameCount = 1)

        advanceTimeBy(1_001)

        val syncCount = connection.controlPackets.count { packet -> (packet[1].toInt() and 0xFF) == 0xD4 }
        assertEquals(2, syncCount)
        job.cancelAndJoin()
    }

    @Test
    fun `keeps the session alive while streaming`() = runTest {
        val job = startStreaming(frameCount = 0)

        advanceTimeBy(25_001)

        assertTrue(connection.rtspMethods.contains("OPTIONS"))
        job.cancelAndJoin()
    }

    @Test
    fun `ends the session and closes the connection when cancelled`() = runTest {
        val job = startStreaming(frameCount = 0)

        job.cancelAndJoin()

        assertEquals("TEARDOWN", connection.rtspMethods.last())
        assertTrue(connection.closed)
    }

    @Test
    fun `fails when the receiver refuses the stream`() = runTest {
        connection.answerToRtsp = { RtspResponse(453, "Not Enough Bandwidth", emptyMap()) }

        val result = runCatching { streamerFor(frameCount = 0).stream {} }

        assertEquals("The device refused ANNOUNCE: 453 Not Enough Bandwidth", result.exceptionOrNull()?.message)
        assertTrue(connection.closed)
    }

    @Test
    fun `fails with a clear reason when the receiver wants a password`() = runTest {
        connection.answerToRtsp = { RtspResponse(401, "Unauthorized", emptyMap()) }

        val result = runCatching { streamerFor(frameCount = 0).stream {} }

        assertEquals("The device wants a password", result.exceptionOrNull()?.message)
    }

    @Test
    fun `fails when the receiver does not say where to send the audio`() = runTest {
        connection.answerToRtsp = { request ->
            if (request.method == "SETUP") RtspResponse(200, "OK", mapOf("Session" to "ABC")) else acceptingAnswer(request)
        }

        val result = runCatching { streamerFor(frameCount = 0).stream {} }

        assertTrue(result.exceptionOrNull() is IOException)
    }

    @Test
    fun `starts the timeline one second later for an audio delay of one second`() = runTest {
        val noDelayJob = startStreaming(frameCount = 0)
        val startWithoutDelay = recordRtpTime()
        noDelayJob.cancelAndJoin()
        connection.rtspRequests.clear()

        val delayedJob = startStreaming(frameCount = 0, audioDelayMillis = 1_000)

        assertEquals(startWithoutDelay + 44_100, recordRtpTime())
        delayedJob.cancelAndJoin()
    }

    private fun recordRtpTime(): Long {
        val record = connection.rtspRequests.first { request -> request.method == "RECORD" }
        return record.headers.getValue("RTP-Info").substringAfter("rtptime=").toLong()
    }

    private fun TestScope.startStreaming(
        frameCount: Int,
        device: StreamingDevice = clearAudioDevice,
        audioDelayMillis: Int = 0,
        onStreaming: () -> Unit = {},
    ): Job {
        val job = launch { streamerFor(frameCount, device, audioDelayMillis).stream(onStreaming) }
        runCurrent()
        return job
    }

    private fun streamerFor(
        frameCount: Int,
        device: StreamingDevice = clearAudioDevice,
        audioDelayMillis: Int = 0,
    ): RaopDeviceStreamer {
        val pcmFrames = flow {
            repeat(frameCount) { emit(ByteArray(PcmFormat.BYTES_PER_FRAME)) }
            awaitCancellation()
        }
        return RaopDeviceStreamer(device, SessionAudio(pcmFrames, httpStream = null), connector, audioDelayMillis) { 1_763_000_000_000 }
    }
}

private fun RtspRequest.bodyText(): String = String(body ?: ByteArray(0))
