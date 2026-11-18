package net.biodiscus.streammusic.core.streaming.device.upnp

import java.io.IOException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.withContext
import net.biodiscus.streammusic.core.model.StreamingDevice
import net.biodiscus.streammusic.core.streaming.device.DeviceStreamer
import net.biodiscus.streammusic.core.streaming.device.SessionAudio
import net.biodiscus.streammusic.core.streaming.device.playableUrlFor
import timber.log.Timber

/** Streams to DLNA, Sonos, HEOS and Samsung Multiroom: the device is told to play our HTTP stream. */
internal class UpnpDeviceStreamer(
    private val device: StreamingDevice,
    private val audio: SessionAudio,
    private val client: AvTransportClient,
) : DeviceStreamer {

    override suspend fun stream(onStreaming: () -> Unit): Nothing {
        val controlUrl = checkNotNull(device.avTransportControlUrl) { "${device.name} has no AVTransport control url" }
        val mediaUrl = playableUrlFor(device.protocol, audio.httpStreamUrlFor(device))
        try {
            Timber.i("Telling %s to play %s", device.name, mediaUrl)
            client.setAvTransportUri(controlUrl, mediaUrl)
            client.play(controlUrl)
            Timber.i("%s accepted the stream", device.name)
            onStreaming()
            awaitCancellation()
        } finally {
            withContext(NonCancellable) { stopIgnoringErrors(controlUrl) }
        }
    }

    private suspend fun stopIgnoringErrors(controlUrl: String) {
        try {
            client.stop(controlUrl)
        } catch (error: IOException) {
            Timber.d(error, "Could not tell %s to stop", controlUrl)
            // The device is unreachable, so there is nothing left to stop.
        }
    }
}
