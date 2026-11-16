package net.biodiscus.streammusic.core.streaming.device.ariacast

import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.biodiscus.streammusic.core.model.DeviceProtocol
import net.biodiscus.streammusic.core.streaming.device.SessionAudio
import net.biodiscus.streammusic.testing.aDevice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AriaCastDeviceStreamerTest {

    private val device = aDevice(protocol = DeviceProtocol.ARIACAST, host = "10.0.0.8")
    private val connection = FakeAriaCastConnection()
    private val connector = RecordingConnector(connection)

    @Test
    fun `connects to the documented streaming port`() = runTest {
        val job = launch { streamerFor(frames = emptyList()).stream {} }
        runCurrent()

        assertEquals("10.0.0.8:12889", connector.connectedTo)
        job.cancelAndJoin()
    }

    @Test
    fun `sends every audio frame in order`() = runTest {
        val frames = listOf(byteArrayOf(1), byteArrayOf(2), byteArrayOf(3))

        val job = launch { streamerFor(frames).stream {} }
        runCurrent()

        assertEquals(listOf(1, 2, 3), connection.sentFrames.map { it.single().toInt() })
        job.cancelAndJoin()
    }

    @Test
    fun `reports streaming once connected`() = runTest {
        var streaming = false

        val job = launch { streamerFor(frames = emptyList()).stream { streaming = true } }
        runCurrent()

        assertTrue(streaming)
        job.cancelAndJoin()
    }

    @Test
    fun `fails when the connection closes`() = runTest {
        connection.closeWith(IOException("socket reset"))

        val result = runCatching { streamerFor(frames = emptyList()).stream {} }

        assertEquals("AriaCast connection closed", result.exceptionOrNull()?.message)
        assertTrue(connection.closed)
    }

    @Test
    fun `fails when a frame cannot be queued`() = runTest {
        connection.acceptsFrames = false

        val result = runCatching { streamerFor(frames = listOf(byteArrayOf(1))).stream {} }

        assertEquals("AriaCast could not accept more audio", result.exceptionOrNull()?.message)
    }

    @Test
    fun `closes the connection when cancelled`() = runTest {
        val job = launch { streamerFor(frames = emptyList()).stream {} }
        runCurrent()

        job.cancelAndJoin()

        assertTrue(connection.closed)
    }

    private fun streamerFor(frames: List<ByteArray>): AriaCastDeviceStreamer {
        val pcmFrames = flow {
            frames.forEach { emit(it) }
            awaitCancellation()
        }
        return AriaCastDeviceStreamer(device, SessionAudio(pcmFrames, aacStreamPort = null), connector)
    }
}

private class RecordingConnector(private val connection: AriaCastConnection) : AriaCastConnector {
    var connectedTo: String? = null
        private set

    override suspend fun connect(host: String, port: Int): AriaCastConnection {
        connectedTo = "$host:$port"
        return connection
    }
}

private class FakeAriaCastConnection : AriaCastConnection {
    val sentFrames = mutableListOf<ByteArray>()
    var acceptsFrames = true
    var closed = false
        private set

    private val closeSignal = CompletableDeferred<Throwable?>()

    fun closeWith(error: Throwable?) {
        closeSignal.complete(error)
    }

    override fun sendAudioFrame(frame: ByteArray): Boolean {
        if (acceptsFrames) sentFrames.add(frame)
        return acceptsFrames
    }

    override suspend fun awaitClosed(): Throwable? = closeSignal.await()

    override fun close() {
        closed = true
    }
}
