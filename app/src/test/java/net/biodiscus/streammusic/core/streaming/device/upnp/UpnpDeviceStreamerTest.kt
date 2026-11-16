package net.biodiscus.streammusic.core.streaming.device.upnp

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import net.biodiscus.streammusic.core.model.DeviceProtocol
import net.biodiscus.streammusic.core.streaming.device.SessionAudio
import net.biodiscus.streammusic.testing.aDevice
import org.junit.Assert.assertEquals
import org.junit.Test

class UpnpDeviceStreamerTest {

    private val client = RecordingAvTransportClient()
    private val device = aDevice(protocol = DeviceProtocol.DLNA, host = "127.0.0.1")
    private val audio = SessionAudio(pcmFrames = emptyFlow(), aacStreamPort = 40_000)

    @Test
    fun `sets the stream address and then plays`() = runTest {
        val job = launch(start = CoroutineStart.UNDISPATCHED) { streamer().stream {} }

        assertEquals(
            listOf("setUri http://127.0.0.1/control http://127.0.0.1:40000/stream.aac", "play http://127.0.0.1/control"),
            client.calls,
        )
        job.cancel()
    }

    @Test
    fun `reports streaming after play succeeded`() = runTest {
        var streaming = false

        val job = launch(start = CoroutineStart.UNDISPATCHED) { streamer().stream { streaming = true } }

        assertEquals(true, streaming)
        job.cancel()
    }

    @Test
    fun `stops the device when cancelled`() = runTest {
        val job = launch(start = CoroutineStart.UNDISPATCHED) { streamer().stream {} }

        job.cancel()
        job.join()

        assertEquals("stop http://127.0.0.1/control", client.calls.last())
    }

    @Test
    fun `Sonos gets a radio scheme address`() = runTest {
        val sonos = aDevice(protocol = DeviceProtocol.SONOS, host = "127.0.0.1")

        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            UpnpDeviceStreamer(sonos, audio, client).stream {}
        }

        assertEquals(
            "setUri http://127.0.0.1/control x-rincon-mp3radio://127.0.0.1:40000/stream.aac",
            client.calls.first(),
        )
        job.cancel()
    }

    private fun streamer() = UpnpDeviceStreamer(device, audio, client)
}

private class RecordingAvTransportClient : AvTransportClient {
    val calls = mutableListOf<String>()

    override suspend fun setAvTransportUri(controlUrl: String, mediaUrl: String) {
        calls.add("setUri $controlUrl $mediaUrl")
    }

    override suspend fun play(controlUrl: String) {
        calls.add("play $controlUrl")
    }

    override suspend fun stop(controlUrl: String) {
        calls.add("stop $controlUrl")
    }
}
