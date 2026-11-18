package net.biodiscus.streammusic.core.streaming.device.ariacast

import java.io.IOException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import net.biodiscus.streammusic.core.model.StreamingDevice
import net.biodiscus.streammusic.core.streaming.device.DeviceStreamer
import net.biodiscus.streammusic.core.streaming.device.SessionAudio
import timber.log.Timber

/**
 * The AriaCast server documents `/audio` on its streaming port, 12889 by default, but the port a server
 * announces over mDNS is not documented. The documented default is used instead.
 */
internal const val ARIACAST_STREAMING_PORT = 12889

/** Pushes the raw PCM frames to an AriaCast server over a WebSocket. */
internal class AriaCastDeviceStreamer(
    private val device: StreamingDevice,
    private val audio: SessionAudio,
    private val connector: AriaCastConnector,
) : DeviceStreamer {

    override suspend fun stream(onStreaming: () -> Unit): Nothing {
        Timber.d("Connecting to AriaCast at %s:%d", device.host, ARIACAST_STREAMING_PORT)
        val connection = connector.connect(device.host, ARIACAST_STREAMING_PORT)
        Timber.i("Connected to AriaCast on %s", device.name)
        try {
            onStreaming()
            coroutineScope {
                launch { sendAllFrames(connection) }
                val error = connection.awaitClosed()
                throw IOException("AriaCast connection closed", error)
            }
        } finally {
            connection.close()
        }
    }

    private suspend fun sendAllFrames(connection: AriaCastConnection) {
        audio.pcmFrames.collect { frame ->
            val wasQueued = connection.sendAudioFrame(frame)
            if (!wasQueued) {
                Timber.w("AriaCast could not queue an audio frame, so the connection is no longer usable")
                throw IOException("AriaCast could not accept more audio")
            }
        }
    }
}
