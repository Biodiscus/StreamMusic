package net.biodiscus.streammusic.core.streaming.device.cast

import java.io.IOException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.biodiscus.streammusic.core.model.DeviceProtocol
import net.biodiscus.streammusic.core.model.StreamAudioFormat
import net.biodiscus.streammusic.core.streaming.device.DeviceEndedOnPurposeException
import net.biodiscus.streammusic.core.streaming.device.SessionAudio
import net.biodiscus.streammusic.testing.FakeHttpAudioStream
import net.biodiscus.streammusic.testing.aDevice
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChromecastDeviceStreamerTest {

    private val channel = ScriptedCastChannel()
    private val device = aDevice(protocol = DeviceProtocol.CHROMECAST, host = "127.0.0.1")
    private val audio = SessionAudio(pcmFrames = emptyFlow(), httpStream = FakeHttpAudioStream(port = 40_000))
    private val streamer = ChromecastDeviceStreamer(device, audio) { _, _ -> channel }

    @Test
    fun `starts the media player and loads the stream`() = runTest {
        channel.receives(receiverStatusWithMediaPlayer, playingMediaStatus)

        val job = launch { streamer.stream {} }
        runCurrent()

        val sentTypes = channel.sentWithoutHeartbeats().map { payloadType(it.payload) }
        assertEquals(listOf("CONNECT", "LAUNCH", "CONNECT", "LOAD"), sentTypes)
        job.cancelAndJoin()
    }

    @Test
    fun `connects to the media player under its transport id`() = runTest {
        channel.receives(receiverStatusWithMediaPlayer, playingMediaStatus)

        val job = launch { streamer.stream {} }
        runCurrent()

        val load = channel.sentWithoutHeartbeats().last()
        assertEquals("web-5", load.destinationId)
        assertEquals(CastNamespace.MEDIA, load.namespace)
        assertEquals(
            "http://127.0.0.1:40000/stream.aac",
            JSONObject(load.payload).getJSONObject("media").getString("contentId"),
        )
        job.cancelAndJoin()
    }

    @Test
    fun `reports streaming once the player status arrives`() = runTest {
        var streaming = false
        channel.receives(receiverStatusWithMediaPlayer, playingMediaStatus)

        val job = launch { streamer.stream { streaming = true } }
        runCurrent()

        assertTrue(streaming)
        job.cancelAndJoin()
    }

    @Test
    fun `does not report streaming before the player status arrives`() = runTest {
        var streaming = false
        channel.receives(receiverStatusWithMediaPlayer)

        val job = launch { streamer.stream { streaming = true } }
        runCurrent()

        assertFalse(streaming)
        job.cancelAndJoin()
    }

    @Test
    fun `loads the content type of the stream format`() = runTest {
        val wavAudio = SessionAudio(emptyFlow(), FakeHttpAudioStream(format = StreamAudioFormat.WAV))
        channel.receives(receiverStatusWithMediaPlayer, playingMediaStatus)

        val job = launch { ChromecastDeviceStreamer(device, wavAudio) { _, _ -> channel }.stream {} }
        runCurrent()

        val media = JSONObject(channel.sentWithoutHeartbeats().last().payload).getJSONObject("media")
        assertEquals("audio/wav", media.getString("contentType"))
        job.cancelAndJoin()
    }

    @Test
    fun `fails when the Chromecast is muted and disconnecting on mute is on`() = runTest {
        channel.receives(receiverStatusWithMediaPlayer, playingMediaStatus, receiverStatusWithVolume(isMuted = false), receiverStatusWithVolume(isMuted = true))

        val result = runCatching { streamerThatDisconnectsOnMute().stream {} }

        assertEquals("Chromecast was muted", result.exceptionOrNull()?.message)
    }

    @Test
    fun `being muted on purpose ends the device on purpose`() = runTest {
        channel.receives(receiverStatusWithMediaPlayer, playingMediaStatus, receiverStatusWithVolume(isMuted = false), receiverStatusWithVolume(isMuted = true))

        val result = runCatching { streamerThatDisconnectsOnMute().stream {} }

        assertTrue(result.exceptionOrNull() is DeviceEndedOnPurposeException)
    }

    @Test
    fun `keeps streaming when the Chromecast is muted and disconnecting on mute is off`() = runTest {
        var streaming = false
        channel.receives(receiverStatusWithMediaPlayer, playingMediaStatus, receiverStatusWithVolume(isMuted = false), receiverStatusWithVolume(isMuted = true))

        val job = launch { streamer.stream { streaming = true } }
        runCurrent()

        assertTrue(streaming)
        assertTrue(job.isActive)
        job.cancelAndJoin()
    }

    @Test
    fun `keeps streaming when the Chromecast was already muted before streaming`() = runTest {
        channel.receives(receiverStatusWithVolume(isMuted = true), receiverStatusWithMediaPlayer, playingMediaStatus)

        val job = launch { streamerThatDisconnectsOnMute().stream {} }
        runCurrent()

        assertTrue(job.isActive)
        job.cancelAndJoin()
    }

    @Test
    fun `answers a ping from the Chromecast with a pong`() = runTest {
        channel.receives(heartbeatPing, receiverStatusWithMediaPlayer, playingMediaStatus)

        val job = launch { streamer.stream {} }
        runCurrent()

        val pongs = channel.sent.filter { payloadType(it.payload) == "PONG" }
        assertEquals(1, pongs.size)
        job.cancelAndJoin()
    }

    @Test
    fun `stops the media player and closes the connection when cancelled`() = runTest {
        channel.receives(receiverStatusWithMediaPlayer, playingMediaStatus)
        val job = launch { streamer.stream {} }
        runCurrent()

        job.cancelAndJoin()

        val stop = channel.sent.last()
        assertEquals("STOP", payloadType(stop.payload))
        assertEquals("abc-123", JSONObject(stop.payload).getString("sessionId"))
        assertTrue(channel.closed)
    }

    @Test
    fun `fails when the Chromecast refuses the stream`() = runTest {
        channel.receives(receiverStatusWithMediaPlayer, CastMessage("web-5", SENDER_ID, CastNamespace.MEDIA, """{"type":"LOAD_FAILED"}"""))

        val result = runCatching { streamer.stream {} }

        assertTrue(result.exceptionOrNull() is IOException)
        assertTrue(channel.closed)
    }

    @Test
    fun `fails when the Chromecast closes the connection while streaming`() = runTest {
        channel.receives(receiverStatusWithMediaPlayer, playingMediaStatus, connectionClosed)

        val result = runCatching { streamer.stream {} }

        assertEquals("Chromecast closed the connection", result.exceptionOrNull()?.message)
    }

    @Test
    fun `fails when the player goes idle with an error while streaming`() = runTest {
        val idleWithError = mediaStatus("""{"playerState":"IDLE","idleReason":"ERROR"}""")
        channel.receives(receiverStatusWithMediaPlayer, playingMediaStatus, idleWithError)

        val result = runCatching { streamer.stream {} }

        assertEquals("Chromecast stopped playing: ERROR", result.exceptionOrNull()?.message)
    }

    @Test
    fun `a person stopping the player ends the device on purpose`() = runTest {
        val cancelledByPerson = mediaStatus("""{"playerState":"IDLE","idleReason":"CANCELLED"}""")
        channel.receives(receiverStatusWithMediaPlayer, playingMediaStatus, cancelledByPerson)

        val result = runCatching { streamer.stream {} }

        assertTrue(result.exceptionOrNull() is DeviceEndedOnPurposeException)
    }

    @Test
    fun `something else being sent to the Chromecast ends the device on purpose`() = runTest {
        val interrupted = mediaStatus("""{"playerState":"IDLE","idleReason":"INTERRUPTED"}""")
        channel.receives(receiverStatusWithMediaPlayer, playingMediaStatus, interrupted)

        val result = runCatching { streamer.stream {} }

        assertTrue(result.exceptionOrNull() is DeviceEndedOnPurposeException)
    }

    @Test
    fun `a player that goes idle with an error does not end the device on purpose`() = runTest {
        val idleWithError = mediaStatus("""{"playerState":"IDLE","idleReason":"ERROR"}""")
        channel.receives(receiverStatusWithMediaPlayer, playingMediaStatus, idleWithError)

        val result = runCatching { streamer.stream {} }

        assertFalse(result.exceptionOrNull() is DeviceEndedOnPurposeException)
    }

    private val receiverStatusWithMediaPlayer = CastMessage(
        sourceId = PLATFORM_RECEIVER_ID,
        destinationId = SENDER_ID,
        namespace = CastNamespace.RECEIVER,
        payload = """{"type":"RECEIVER_STATUS","status":{"applications":[
            {"appId":"CC1AD845","transportId":"web-5","sessionId":"abc-123"}]}}""",
    )
    private fun receiverStatusWithVolume(isMuted: Boolean) = CastMessage(
        sourceId = PLATFORM_RECEIVER_ID,
        destinationId = SENDER_ID,
        namespace = CastNamespace.RECEIVER,
        payload = """{"type":"RECEIVER_STATUS","status":{"volume":{"level":0.5,"muted":$isMuted}}}""",
    )

    private fun streamerThatDisconnectsOnMute() =
        ChromecastDeviceStreamer(device, audio, disconnectsOnMute = true) { _, _ -> channel }

    private val playingMediaStatus = mediaStatus("""{"playerState":"PLAYING"}""")
    private val heartbeatPing = CastMessage(PLATFORM_RECEIVER_ID, SENDER_ID, CastNamespace.HEARTBEAT, pingPayload())
    private val connectionClosed =
        CastMessage(PLATFORM_RECEIVER_ID, SENDER_ID, CastNamespace.CONNECTION, """{"type":"CLOSE"}""")

    private fun mediaStatus(statusJson: String) =
        CastMessage("web-5", SENDER_ID, CastNamespace.MEDIA, """{"type":"MEDIA_STATUS","status":[$statusJson]}""")
}

private class ScriptedCastChannel : CastChannel {
    val sent = mutableListOf<CastMessage>()
    var closed = false
        private set

    private val incoming = Channel<CastMessage>(Channel.UNLIMITED)

    fun receives(vararg messages: CastMessage) {
        messages.forEach { incoming.trySend(it) }
    }

    fun sentWithoutHeartbeats(): List<CastMessage> = sent.filter { it.namespace != CastNamespace.HEARTBEAT }

    override suspend fun send(message: CastMessage) {
        sent.add(message)
    }

    override suspend fun receive(): CastMessage = incoming.receive()

    override fun close() {
        closed = true
    }
}
