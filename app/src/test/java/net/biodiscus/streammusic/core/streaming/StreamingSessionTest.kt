package net.biodiscus.streammusic.core.streaming

import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.biodiscus.streammusic.core.model.DeviceProtocol
import net.biodiscus.streammusic.core.model.DeviceStreamStatus
import net.biodiscus.streammusic.core.model.ProtocolFamily
import net.biodiscus.streammusic.core.model.StreamAudioFormat
import net.biodiscus.streammusic.core.model.StreamingDevice
import net.biodiscus.streammusic.core.model.StreamingState
import net.biodiscus.streammusic.core.streaming.device.DeviceStreamer
import net.biodiscus.streammusic.core.streaming.device.DeviceStreamerFactory
import net.biodiscus.streammusic.core.streaming.device.SessionAudio
import net.biodiscus.streammusic.core.streaming.http.HttpAudioStreamStarter
import net.biodiscus.streammusic.core.streaming.http.RunningHttpAudioStream
import net.biodiscus.streammusic.testing.FakeHttpAudioStream
import net.biodiscus.streammusic.testing.FakeSettingsRepository
import net.biodiscus.streammusic.testing.aDevice
import kotlin.coroutines.ContinuationInterceptor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamingSessionTest {

    private val stateRepository = StreamingStateRepository()
    private val httpStarter = RecordingHttpAudioStreamStarter()
    private val settings = FakeSettingsRepository()
    private val streamers = FakeStreamerFactory()
    private val phoneVolume = FakePhoneMusicVolume(level = 7)
    private lateinit var session: StreamingSession

    private val kitchen = aDevice(name = "Kitchen", protocol = DeviceProtocol.DLNA, host = "10.0.0.2")
    private val study = aDevice(name = "Study", protocol = DeviceProtocol.ARIACAST, host = "10.0.0.3")

    @Test
    fun `marks every device as connecting when the session starts`() = runTest {
        streamers.neverFinishesConnecting(kitchen, study)

        val job = startSession(kitchen, study)

        assertEquals(DeviceStreamStatus.Connecting, statusOf(kitchen))
        assertEquals(DeviceStreamStatus.Connecting, statusOf(study))
        job.cancelAndJoin()
    }

    @Test
    fun `marks a device as streaming when its streamer reports it`() = runTest {
        streamers.streamsSuccessfully(kitchen)

        val job = startSession(kitchen)

        assertEquals(DeviceStreamStatus.Streaming, statusOf(kitchen))
        job.cancelAndJoin()
    }

    @Test
    fun `a failing device is marked as failed with the reason`() = runTest {
        streamers.fails(kitchen, IOException("Connection refused"))

        val job = startSession(kitchen)

        assertEquals(DeviceStreamStatus.Failed("Connection refused"), statusOf(kitchen))
        job.cancelAndJoin()
    }

    @Test
    fun `a failing device does not stop the other devices`() = runTest {
        streamers.fails(kitchen, IOException("Connection refused"))
        streamers.streamsSuccessfully(study)

        val job = startSession(kitchen, study)

        assertEquals(DeviceStreamStatus.Streaming, statusOf(study))
        job.cancelAndJoin()
    }

    @Test
    fun `starts the AAC stream when a device plays an HTTP stream`() = runTest {
        streamers.streamsSuccessfully(kitchen)

        val job = startSession(kitchen)

        assertEquals(1, httpStarter.startedFormats.size)
        job.cancelAndJoin()
    }

    @Test
    fun `does not start the AAC stream when every device takes raw audio`() = runTest {
        streamers.streamsSuccessfully(study)

        val job = startSession(study)

        assertEquals(0, httpStarter.startedFormats.size)
        job.cancelAndJoin()
    }

    @Test
    fun `stops the AAC stream and goes idle when cancelled`() = runTest {
        streamers.streamsSuccessfully(kitchen)
        val job = startSession(kitchen)

        job.cancelAndJoin()

        assertTrue(httpStarter.isEveryStreamStopped)
        assertEquals(StreamingState.Idle, stateRepository.state.value)
    }

    @Test
    fun `runs every streamer on the dispatcher given to the session`() = runTest {
        val sessionDispatcher = StandardTestDispatcher(testScheduler)
        streamers.recordsItsDispatcher(kitchen)

        val job = startSession(kitchen, dispatcher = sessionDispatcher)

        assertSame(sessionDispatcher, streamers.dispatcherOf(kitchen))
        job.cancelAndJoin()
    }

    @Test
    fun `stays active while streaming`() = runTest {
        streamers.streamsSuccessfully(kitchen)

        val job = startSession(kitchen)

        assertFalse(stateRepository.state.value is StreamingState.Idle)
        job.cancelAndJoin()
    }

    @Test
    fun `silences the phone while streaming`() = runTest {
        streamers.streamsSuccessfully(kitchen)

        val job = startSession(kitchen)

        assertEquals(SILENT_VOLUME_LEVEL, phoneVolume.level)
        job.cancelAndJoin()
    }

    @Test
    fun `gives the phone its volume back when the session ends`() = runTest {
        streamers.streamsSuccessfully(kitchen)
        val job = startSession(kitchen)

        job.cancelAndJoin()

        assertEquals(7, phoneVolume.level)
    }

    @Test
    fun `a device added while streaming starts streaming`() = runTest {
        streamers.streamsSuccessfully(kitchen, study)
        val job = startSession(kitchen)

        session.addDevice(study)
        runCurrent()

        assertEquals(listOf(kitchen, study), devicesInState())
        assertEquals(DeviceStreamStatus.Streaming, statusOf(study))
        job.cancelAndJoin()
    }

    @Test
    fun `adding a device that is already streaming changes nothing`() = runTest {
        streamers.streamsSuccessfully(kitchen)
        val job = startSession(kitchen)

        session.addDevice(kitchen)
        runCurrent()

        assertEquals(listOf(kitchen), devicesInState())
        job.cancelAndJoin()
    }

    @Test
    fun `starts the AAC stream when the first device that needs it is added later`() = runTest {
        streamers.streamsSuccessfully(study, kitchen)
        val job = startSession(study)

        session.addDevice(kitchen)
        runCurrent()

        assertEquals(1, httpStarter.startedFormats.size)
        job.cancelAndJoin()
    }

    @Test
    fun `shares one AAC stream between devices that need it`() = runTest {
        val bedroom = aDevice(name = "Bedroom", protocol = DeviceProtocol.DLNA, host = "10.0.0.4")
        streamers.streamsSuccessfully(kitchen, bedroom)
        val job = startSession(kitchen)

        session.addDevice(bedroom)
        runCurrent()

        assertEquals(1, httpStarter.startedFormats.size)
        job.cancelAndJoin()
    }

    @Test
    fun `starts a stream in the format chosen for the protocol of the device`() = runTest {
        settings.setAudioFormat(ProtocolFamily.DLNA, StreamAudioFormat.WAV)
        streamers.streamsSuccessfully(kitchen)

        val job = startSession(kitchen)

        assertEquals(listOf(StreamAudioFormat.WAV), httpStarter.startedFormats)
        job.cancelAndJoin()
    }

    @Test
    fun `starts a separate stream for a device that needs another format`() = runTest {
        val speaker = aDevice(name = "Speaker", protocol = DeviceProtocol.SONOS, host = "10.0.0.5")
        settings.setAudioFormat(ProtocolFamily.SONOS, StreamAudioFormat.WAV)
        streamers.streamsSuccessfully(kitchen, speaker)

        val job = startSession(kitchen, speaker)

        assertEquals(listOf(StreamAudioFormat.AAC, StreamAudioFormat.WAV), httpStarter.startedFormats)
        job.cancelAndJoin()
    }

    @Test
    fun `a removed device is stopped and leaves the stream`() = runTest {
        streamers.streamsSuccessfully(kitchen, study)
        val job = startSession(kitchen, study)

        session.removeDevice(study.id)
        runCurrent()

        assertTrue(streamers.hasStopped(study))
        assertEquals(listOf(kitchen), devicesInState())
        job.cancelAndJoin()
    }

    @Test
    fun `removing a device does not stop the other devices`() = runTest {
        streamers.streamsSuccessfully(kitchen, study)
        val job = startSession(kitchen, study)

        session.removeDevice(study.id)
        runCurrent()

        assertFalse(streamers.hasStopped(kitchen))
        job.cancelAndJoin()
    }

    @Test
    fun `a device that was removed can be added again`() = runTest {
        streamers.streamsSuccessfully(kitchen, study)
        val job = startSession(kitchen, study)
        session.removeDevice(study.id)
        runCurrent()

        session.addDevice(study)
        runCurrent()

        assertEquals(listOf(kitchen, study), devicesInState())
        assertEquals(DeviceStreamStatus.Streaming, statusOf(study))
        job.cancelAndJoin()
    }

    @Test
    fun `a device that failed can be removed`() = runTest {
        streamers.fails(kitchen, IOException("Connection refused"))
        streamers.streamsSuccessfully(study)
        val job = startSession(kitchen, study)

        session.removeDevice(kitchen.id)
        runCurrent()

        assertEquals(listOf(study), devicesInState())
        job.cancelAndJoin()
    }

    private fun TestScope.startSession(
        vararg devices: StreamingDevice,
        dispatcher: CoroutineDispatcher = StandardTestDispatcher(testScheduler),
    ): Job {
        val capturedPcm: Flow<ByteArray> = flow { awaitCancellation() }
        session = StreamingSession(
            devices.toList(), capturedPcm, streamers, httpStarter, stateRepository, phoneVolume, settings, dispatcher,
        )
        val job = launch { session.run() }
        runCurrent()
        return job
    }

    private fun statusOf(device: StreamingDevice): DeviceStreamStatus? =
        (stateRepository.state.value as StreamingState.Active).statusByDeviceId[device.id]

    private fun devicesInState(): List<StreamingDevice> =
        (stateRepository.state.value as StreamingState.Active).devices
}

private class FakeStreamerFactory : DeviceStreamerFactory {
    private val behaviorByDeviceId = mutableMapOf<String, suspend (() -> Unit) -> Nothing>()
    private val stoppedDeviceIds = mutableListOf<String>()
    private val dispatcherByDeviceId = mutableMapOf<String, ContinuationInterceptor?>()

    fun streamsSuccessfully(vararg devices: StreamingDevice) {
        devices.forEach { device ->
            behaviorByDeviceId[device.id] = { onStreaming ->
                onStreaming()
                awaitCancellation()
            }
        }
    }

    fun recordsItsDispatcher(device: StreamingDevice) {
        behaviorByDeviceId[device.id] = { onStreaming ->
            dispatcherByDeviceId[device.id] = currentCoroutineContext()[ContinuationInterceptor]
            onStreaming()
            awaitCancellation()
        }
    }

    fun hasStopped(device: StreamingDevice): Boolean = device.id in stoppedDeviceIds

    fun dispatcherOf(device: StreamingDevice): ContinuationInterceptor? = dispatcherByDeviceId[device.id]

    fun neverFinishesConnecting(vararg devices: StreamingDevice) {
        devices.forEach { device -> behaviorByDeviceId[device.id] = { awaitCancellation() } }
    }

    fun fails(device: StreamingDevice, error: Exception) {
        behaviorByDeviceId[device.id] = { throw error }
    }

    override fun create(device: StreamingDevice, audio: SessionAudio): DeviceStreamer {
        val behavior = behaviorByDeviceId.getValue(device.id)
        return object : DeviceStreamer {
            override suspend fun stream(onStreaming: () -> Unit): Nothing =
                try {
                    behavior(onStreaming)
                } finally {
                    stoppedDeviceIds += device.id
                }
        }
    }
}

private class RecordingHttpAudioStreamStarter : HttpAudioStreamStarter {
    private val startedStreams = mutableListOf<FakeHttpAudioStream>()

    val startedFormats: List<StreamAudioFormat>
        get() = startedStreams.map { stream -> stream.format }

    val isEveryStreamStopped: Boolean
        get() = startedStreams.isNotEmpty() && startedStreams.all { stream -> stream.isStopped }

    override fun start(scope: CoroutineScope, pcmFrames: Flow<ByteArray>, format: StreamAudioFormat): RunningHttpAudioStream {
        val stream = FakeHttpAudioStream(format = format)
        startedStreams += stream
        return stream
    }
}

private class FakePhoneMusicVolume(override var level: Int) : PhoneMusicVolume
