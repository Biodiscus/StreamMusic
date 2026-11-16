package net.biodiscus.streammusic.core.streaming.device

import kotlinx.coroutines.flow.Flow
import net.biodiscus.streammusic.core.model.StreamingDevice
import net.biodiscus.streammusic.core.streaming.aac.streamUrl
import net.biodiscus.streammusic.core.streaming.localAddressFacing

/** The audio of the running session, in the two shapes devices can consume it. */
internal class SessionAudio(
    val pcmFrames: Flow<ByteArray>,
    private val aacStreamPort: Int?,
) {
    fun aacStreamUrlFor(device: StreamingDevice): String {
        val port = checkNotNull(aacStreamPort) { "The AAC stream was not started for ${device.name}" }
        return streamUrl(localAddressFacing(device.host), port)
    }
}

/** Sends the session audio to one device for as long as the calling coroutine is active. */
internal interface DeviceStreamer {

    /**
     * Connects to the device and starts the audio, then calls [onStreaming] and keeps going.
     * It never returns: cancelling the coroutine stops the device, and an exception means the device failed.
     */
    suspend fun stream(onStreaming: () -> Unit): Nothing
}

internal interface DeviceStreamerFactory {
    fun create(device: StreamingDevice, audio: SessionAudio): DeviceStreamer
}
