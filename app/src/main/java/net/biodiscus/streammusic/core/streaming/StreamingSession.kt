package net.biodiscus.streammusic.core.streaming

import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.biodiscus.streammusic.core.logging.describeForLog
import net.biodiscus.streammusic.core.model.DeviceStreamStatus
import net.biodiscus.streammusic.core.model.StreamAudioFormat
import net.biodiscus.streammusic.core.model.audioFormatFor
import net.biodiscus.streammusic.core.model.StreamTransport
import net.biodiscus.streammusic.core.model.StreamingDevice
import net.biodiscus.streammusic.core.settings.SettingsRepository
import net.biodiscus.streammusic.core.streaming.device.DeviceStreamerFactory
import net.biodiscus.streammusic.core.streaming.device.SessionAudio
import net.biodiscus.streammusic.core.streaming.http.HttpAudioStreamStarter
import net.biodiscus.streammusic.core.streaming.http.RunningHttpAudioStream
import timber.log.Timber

private const val PCM_FRAMES_KEPT_PER_LISTENER = 100

internal class StreamingSessionFactory @Inject constructor(
    private val streamerFactory: DeviceStreamerFactory,
    private val httpAudioStreamStarter: HttpAudioStreamStarter,
    private val stateRepository: StreamingStateRepository,
    private val phoneMusicVolume: PhoneMusicVolume,
    private val settingsRepository: SettingsRepository,
) {
    fun create(devices: List<StreamingDevice>, capturedPcm: Flow<ByteArray>) = StreamingSession(
        devices,
        capturedPcm,
        streamerFactory,
        httpAudioStreamStarter,
        stateRepository,
        phoneMusicVolume,
        settingsRepository,
        Dispatchers.IO,
    )
}

/**
 * Sends one captured audio stream to several devices at once.
 * A device that fails is marked as failed and does not affect the other devices.
 * Devices can join and leave while the session runs, so the user does not have to pick the audio again.
 * The phone itself is silent while the session runs, so the user can watch other content. The volume the
 * user had comes back when the session ends.
 */
internal class StreamingSession(
    private val initialDevices: List<StreamingDevice>,
    private val capturedPcm: Flow<ByteArray>,
    private val streamerFactory: DeviceStreamerFactory,
    private val httpAudioStreamStarter: HttpAudioStreamStarter,
    private val stateRepository: StreamingStateRepository,
    private val phoneMusicVolume: PhoneMusicVolume,
    private val settingsRepository: SettingsRepository,
    private val dispatcher: CoroutineDispatcher,
) {
    private val deviceChanges = Channel<DeviceChange>(Channel.UNLIMITED)

    // Only touched by the coroutine that runs the session, one change at a time.
    private val streamingJobsByDeviceId = mutableMapOf<String, Job>()
    private val httpStreamsByFormat = mutableMapOf<StreamAudioFormat, RunningHttpAudioStream>()

    /** Starts streaming to a device that is not part of the session yet. Safe to call from any thread. */
    fun addDevice(device: StreamingDevice) {
        deviceChanges.trySend(DeviceChange.Add(device))
    }

    /** Stops streaming to a device of the session. Safe to call from any thread. */
    fun removeDevice(deviceId: String) {
        deviceChanges.trySend(DeviceChange.Remove(deviceId))
    }

    /**
     * Streams until the calling coroutine is cancelled.
     * Everything runs on [dispatcher] because the service calls this from the main thread, where Android
     * forbids network calls, and every streamer uses sockets.
     */
    suspend fun run(): Nothing = withContext(dispatcher) { streamUntilCancelled() }

    private suspend fun streamUntilCancelled(): Nothing = coroutineScope {
        stateRepository.streamingStarted(initialDevices)
        val pcmFrames = sharePcmFrames(scope = this)
        val volumeBeforeStreaming = silencePhone()
        try {
            initialDevices.forEach { device -> startStreamingTo(device, scope = this, pcmFrames) }
            applyDeviceChanges(scope = this, pcmFrames)
        } finally {
            phoneMusicVolume.level = volumeBeforeStreaming
            httpStreamsByFormat.values.forEach { stream -> stream.stop() }
            stateRepository.streamingEnded()
        }
    }

    private suspend fun applyDeviceChanges(scope: CoroutineScope, pcmFrames: Flow<ByteArray>): Nothing {
        while (true) {
            when (val change = deviceChanges.receive()) {
                is DeviceChange.Add -> addToSession(change.device, scope, pcmFrames)
                is DeviceChange.Remove -> removeFromSession(change.deviceId)
            }
        }
    }

    private fun addToSession(device: StreamingDevice, scope: CoroutineScope, pcmFrames: Flow<ByteArray>) {
        if (device.id in streamingJobsByDeviceId) return
        stateRepository.deviceAdded(device)
        startStreamingTo(device, scope, pcmFrames)
    }

    /** Waits until the device has stopped, so adding the same device again cannot overlap with its cleanup. */
    private suspend fun removeFromSession(deviceId: String) {
        val streamingJob = streamingJobsByDeviceId.remove(deviceId) ?: return
        stateRepository.deviceRemoved(deviceId)
        streamingJob.cancelAndJoin()
    }

    private fun silencePhone(): Int {
        val volumeBeforeStreaming = phoneMusicVolume.level
        phoneMusicVolume.level = SILENT_VOLUME_LEVEL
        return volumeBeforeStreaming
    }

    /** Slow listeners lose their oldest frames instead of holding up the recording. */
    private fun sharePcmFrames(scope: CoroutineScope): SharedFlow<ByteArray> =
        capturedPcm
            .buffer(PCM_FRAMES_KEPT_PER_LISTENER, BufferOverflow.DROP_OLDEST)
            .shareIn(scope, SharingStarted.Eagerly)

    private fun startStreamingTo(device: StreamingDevice, scope: CoroutineScope, pcmFrames: Flow<ByteArray>) {
        val audio = SessionAudio(pcmFrames, httpStreamFor(device, scope, pcmFrames))
        streamingJobsByDeviceId[device.id] = scope.launch { streamToDevice(device, audio) }
    }

    /** An HTTP stream only runs when a device needs it, and then it is shared by all devices that need the same format. */
    private fun httpStreamFor(device: StreamingDevice, scope: CoroutineScope, pcmFrames: Flow<ByteArray>): RunningHttpAudioStream? {
        if (device.protocol.transport != StreamTransport.DEVICE_PULLS_HTTP_STREAM) return null
        val format = settingsRepository.currentSettings.audioFormatFor(device.protocol)
        return httpStreamsByFormat.getOrPut(format) { httpAudioStreamStarter.start(scope, pcmFrames, format) }
    }

    private suspend fun streamToDevice(device: StreamingDevice, audio: SessionAudio) {
        Timber.i("Connecting to %s", device.describeForLog())
        stateRepository.deviceStatusChanged(device.id, DeviceStreamStatus.Connecting)
        try {
            streamerFactory.create(device, audio).stream(onStreaming = { markStreaming(device) })
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            Timber.e(failure, "Streaming to %s failed", device.describeForLog())
            val reason = failure.message ?: failure.javaClass.simpleName
            stateRepository.deviceStatusChanged(device.id, DeviceStreamStatus.Failed(reason))
        }
    }

    private fun markStreaming(device: StreamingDevice) {
        Timber.i("Streaming to %s (%s)", device.name, device.protocol)
        stateRepository.deviceStatusChanged(device.id, DeviceStreamStatus.Streaming)
    }
}

private sealed interface DeviceChange {
    data class Add(val device: StreamingDevice) : DeviceChange
    data class Remove(val deviceId: String) : DeviceChange
}
