package net.biodiscus.streammusic.core.streaming

import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.biodiscus.streammusic.core.model.DeviceStreamStatus
import net.biodiscus.streammusic.core.model.StreamTransport
import net.biodiscus.streammusic.core.model.StreamingDevice
import net.biodiscus.streammusic.core.streaming.aac.AacStreamStarter
import net.biodiscus.streammusic.core.streaming.aac.RunningAacStream
import net.biodiscus.streammusic.core.streaming.device.DeviceStreamerFactory
import net.biodiscus.streammusic.core.streaming.device.SessionAudio

private const val PCM_FRAMES_KEPT_PER_LISTENER = 100

internal class StreamingSessionFactory @Inject constructor(
    private val streamerFactory: DeviceStreamerFactory,
    private val aacStreamStarter: AacStreamStarter,
    private val stateRepository: StreamingStateRepository,
) {
    fun create(devices: List<StreamingDevice>, capturedPcm: Flow<ByteArray>) =
        StreamingSession(devices, capturedPcm, streamerFactory, aacStreamStarter, stateRepository, Dispatchers.IO)
}

/**
 * Sends one captured audio stream to several devices at once.
 * A device that fails is marked as failed and does not affect the other devices.
 */
internal class StreamingSession(
    private val devices: List<StreamingDevice>,
    private val capturedPcm: Flow<ByteArray>,
    private val streamerFactory: DeviceStreamerFactory,
    private val aacStreamStarter: AacStreamStarter,
    private val stateRepository: StreamingStateRepository,
    private val dispatcher: CoroutineDispatcher,
) {

    /**
     * Streams until the calling coroutine is cancelled.
     * Everything runs on [dispatcher] because the service calls this from the main thread, where Android
     * forbids network calls, and every streamer uses sockets.
     */
    suspend fun run(): Nothing = withContext(dispatcher) { streamUntilCancelled() }

    private suspend fun streamUntilCancelled(): Nothing = coroutineScope {
        stateRepository.streamingStarted(devices)
        val pcmFrames = sharePcmFrames(scope = this)
        val aacStream = startAacStreamIfAnyDeviceNeedsIt(scope = this, pcmFrames)
        val audio = SessionAudio(pcmFrames, aacStream?.port)

        devices.forEach { device -> launch { streamToDevice(device, audio) } }

        try {
            awaitCancellation()
        } finally {
            aacStream?.stop()
            stateRepository.streamingEnded()
        }
    }

    /** Slow listeners lose their oldest frames instead of holding up the recording. */
    private fun sharePcmFrames(scope: CoroutineScope): SharedFlow<ByteArray> =
        capturedPcm
            .buffer(PCM_FRAMES_KEPT_PER_LISTENER, BufferOverflow.DROP_OLDEST)
            .shareIn(scope, SharingStarted.Eagerly)

    private fun startAacStreamIfAnyDeviceNeedsIt(scope: CoroutineScope, pcmFrames: Flow<ByteArray>): RunningAacStream? {
        val anyDevicePlaysHttpStream = devices.any { it.protocol.transport == StreamTransport.DEVICE_PULLS_HTTP_STREAM }
        return if (anyDevicePlaysHttpStream) aacStreamStarter.start(scope, pcmFrames) else null
    }

    private suspend fun streamToDevice(device: StreamingDevice, audio: SessionAudio) {
        stateRepository.deviceStatusChanged(device.id, DeviceStreamStatus.Connecting)
        try {
            streamerFactory.create(device, audio).stream(onStreaming = { markStreaming(device) })
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            val reason = failure.message ?: failure.javaClass.simpleName
            stateRepository.deviceStatusChanged(device.id, DeviceStreamStatus.Failed(reason))
        }
    }

    private fun markStreaming(device: StreamingDevice) {
        stateRepository.deviceStatusChanged(device.id, DeviceStreamStatus.Streaming)
    }
}
