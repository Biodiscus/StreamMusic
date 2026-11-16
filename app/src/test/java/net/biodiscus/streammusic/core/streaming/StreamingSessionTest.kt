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
import net.biodiscus.streammusic.core.model.StreamingDevice
import net.biodiscus.streammusic.core.model.StreamingState
import net.biodiscus.streammusic.core.streaming.aac.AacStreamStarter
import net.biodiscus.streammusic.core.streaming.aac.RunningAacStream
import net.biodiscus.streammusic.core.streaming.device.DeviceStreamer
import net.biodiscus.streammusic.core.streaming.device.DeviceStreamerFactory
import net.biodiscus.streammusic.core.streaming.device.SessionAudio
import net.biodiscus.streammusic.testing.aDevice
import kotlin.coroutines.ContinuationInterceptor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamingSessionTest {

    private val stateRepository = StreamingStateRepository()
    private val aacStarter = RecordingAacStreamStarter()
    private val streamers = FakeStreamerFactory()
    private val phoneVolume = FakePhoneMusicVolume(level = 7)

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

        assertEquals(1, aacStarter.startCount)
        job.cancelAndJoin()
    }

    @Test
    fun `does not start the AAC stream when every device takes raw audio`() = runTest {
        streamers.streamsSuccessfully(study)

        val job = startSession(study)

        assertEquals(0, aacStarter.startCount)
        job.cancelAndJoin()
    }

    @Test
    fun `stops the AAC stream and goes idle when cancelled`() = runTest {
        streamers.streamsSuccessfully(kitchen)
        val job = startSession(kitchen)

        job.cancelAndJoin()

        assertTrue(aacStarter.stopped)
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

    private fun TestScope.startSession(
        vararg devices: StreamingDevice,
        dispatcher: CoroutineDispatcher = StandardTestDispatcher(testScheduler),
    ): Job {
        val capturedPcm: Flow<ByteArray> = flow { awaitCancellation() }
        val session = StreamingSession(devices.toList(), capturedPcm, streamers, aacStarter, stateRepository, phoneVolume, dispatcher)
        val job = launch { session.run() }
        runCurrent()
        return job
    }

    private fun statusOf(device: StreamingDevice): DeviceStreamStatus? =
        (stateRepository.state.value as StreamingState.Active).statusByDeviceId[device.id]
}

private class FakeStreamerFactory : DeviceStreamerFactory {
    private val behaviorByDeviceId = mutableMapOf<String, suspend (() -> Unit) -> Nothing>()
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
            override suspend fun stream(onStreaming: () -> Unit): Nothing = behavior(onStreaming)
        }
    }
}

private class RecordingAacStreamStarter : AacStreamStarter {
    var startCount = 0
        private set
    var stopped = false
        private set

    override fun start(scope: CoroutineScope, pcmFrames: Flow<ByteArray>): RunningAacStream {
        startCount++
        return object : RunningAacStream {
            override val port = 40_000
            override fun stop() {
                stopped = true
            }
        }
    }
}

private class FakePhoneMusicVolume(override var level: Int) : PhoneMusicVolume
