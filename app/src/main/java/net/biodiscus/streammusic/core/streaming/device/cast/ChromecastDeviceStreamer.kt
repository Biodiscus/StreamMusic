package net.biodiscus.streammusic.core.streaming.device.cast

import java.io.IOException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.biodiscus.streammusic.core.model.StreamingDevice
import net.biodiscus.streammusic.core.streaming.device.DeviceStreamer
import net.biodiscus.streammusic.core.streaming.device.SessionAudio
import timber.log.Timber

private const val HEARTBEAT_INTERVAL_MILLIS = 5_000L
private const val LAUNCH_REQUEST_ID = 1
private const val LOAD_REQUEST_ID = 2
private const val STOP_REQUEST_ID = 3
private val LOAD_FAILURE_TYPES = setOf("LOAD_FAILED", "LOAD_CANCELLED", "INVALID_REQUEST")

/** Streams to a Chromecast by starting its built-in media player and telling it to play our HTTP stream. */
internal class ChromecastDeviceStreamer(
    private val device: StreamingDevice,
    private val audio: SessionAudio,
    private val disconnectsOnMute: Boolean = false,
    private val channelOpener: CastChannelOpener,
) : DeviceStreamer {

    override suspend fun stream(onStreaming: () -> Unit): Nothing {
        Timber.d("Opening a Cast channel to %s:%d", device.host, device.port)
        val channel = channelOpener.open(device.host, device.port)
        Timber.d("Cast channel to %s is open", device.name)
        val conversation = CastConversation(channel, disconnectsOnMute)
        coroutineScope {
            launch(start = CoroutineStart.UNDISPATCHED) { conversation.endWhenCancelled() }
            launch { conversation.sendHeartbeatsForever() }
            conversation.startPlaying(audio.httpStreamUrlFor(device), audio.httpContentTypeFor(device))
            onStreaming()
            conversation.listenUntilPlaybackStops()
        }
    }
}

/** The back-and-forth with one Chromecast. Each step waits for the answer it needs. */
private class CastConversation(
    private val channel: CastChannel,
    private val disconnectsOnMute: Boolean,
) {

    @Volatile
    private var launchedSessionId: String? = null

    /** Null until the Chromecast has told us. The first answer is the starting point, not a change. */
    private var isMuted: Boolean? = null

    suspend fun startPlaying(streamUrl: String, contentType: String) {
        send(PLATFORM_RECEIVER_ID, CastNamespace.CONNECTION, connectPayload())
        send(
            PLATFORM_RECEIVER_ID,
            CastNamespace.RECEIVER,
            launchPayload(DEFAULT_MEDIA_RECEIVER_APP_ID, LAUNCH_REQUEST_ID),
        )
        val app = waitForLaunchedApp()
        launchedSessionId = app.sessionId
        Timber.d("The Chromecast started its media player (transport %s)", app.transportId)

        send(app.transportId, CastNamespace.CONNECTION, connectPayload())
        Timber.i("Asking the Chromecast to load %s (%s)", streamUrl, contentType)
        send(app.transportId, CastNamespace.MEDIA, loadLiveAudioPayload(streamUrl, contentType, LOAD_REQUEST_ID))
        waitForPlayerStatus()
        Timber.i("The Chromecast is playing")
    }

    suspend fun listenUntilPlaybackStops(): Nothing {
        while (true) {
            val message = receiveWithoutHeartbeats()
            if (payloadType(message.payload) != "MEDIA_STATUS") continue
            val idleReason = findIdleReason(message.payload)
            if (idleReason != null) failBecauseOf("Chromecast stopped playing: $idleReason", message.payload)
        }
    }

    suspend fun sendHeartbeatsForever() {
        while (true) {
            send(PLATFORM_RECEIVER_ID, CastNamespace.HEARTBEAT, pingPayload())
            delay(HEARTBEAT_INTERVAL_MILLIS)
        }
    }

    /** Runs until the streaming coroutine is cancelled, then ends the Chromecast app and closes the connection. */
    suspend fun endWhenCancelled() {
        try {
            awaitCancellation()
        } finally {
            withContext(NonCancellable) {
                stopMediaPlayerIgnoringErrors()
                channel.close()
            }
        }
    }

    private suspend fun waitForLaunchedApp(): LaunchedCastApp {
        while (true) {
            val message = receiveWithoutHeartbeats()
            val type = payloadType(message.payload)
            if (type == "LAUNCH_ERROR") failBecauseOf("Chromecast could not start its media player", message.payload)
            if (type != "RECEIVER_STATUS") continue
            val app = findLaunchedApp(message.payload, DEFAULT_MEDIA_RECEIVER_APP_ID)
            if (app != null) return app
        }
    }

    private suspend fun waitForPlayerStatus() {
        while (true) {
            val message = receiveWithoutHeartbeats()
            val type = payloadType(message.payload)
            if (type in LOAD_FAILURE_TYPES) failBecauseOf("Chromecast refused the stream: $type", message.payload)
            if (type == "MEDIA_STATUS" && isPlayerStatus(message.payload)) return
        }
    }

    /** Answers the Chromecast's own pings on the way, and fails when it closes the connection. */
    private suspend fun receiveWithoutHeartbeats(): CastMessage {
        while (true) {
            val message = channel.receive()
            val type = payloadType(message.payload)
            if (message.namespace == CastNamespace.CONNECTION && type == "CLOSE") {
                failBecauseOf("Chromecast closed the connection", message.payload)
            }
            if (message.namespace != CastNamespace.HEARTBEAT) {
                Timber.d("The Chromecast sent %s on %s", type, message.namespace)
                failWhenMutedOnPurpose(message)
                return message
            }
            if (type == "PING") send(message.sourceId, CastNamespace.HEARTBEAT, pongPayload())
        }
    }

    /** The Chromecast explains itself in the payload, which the short error message for the screen cannot hold. */
    private fun failBecauseOf(problem: String, payload: String): Nothing {
        Timber.w("%s. The Chromecast said: %s", problem, payload)
        throw IOException(problem)
    }

    private fun failWhenMutedOnPurpose(message: CastMessage) {
        val isMutedNow = findMuted(message.payload) ?: return
        val wasUnmuted = isMuted == false
        isMuted = isMutedNow
        if (disconnectsOnMute && isMutedNow && wasUnmuted) throw IOException("Chromecast was muted")
    }

    private suspend fun stopMediaPlayerIgnoringErrors() {
        val sessionId = launchedSessionId ?: return
        try {
            send(PLATFORM_RECEIVER_ID, CastNamespace.RECEIVER, stopPayload(sessionId, STOP_REQUEST_ID))
        } catch (error: IOException) {
            Timber.d(error, "Could not stop the Chromecast media player")
            // The Chromecast is unreachable, so its player stops by itself.
        }
    }

    private suspend fun send(destinationId: String, namespace: String, payload: String) {
        channel.send(CastMessage(SENDER_ID, destinationId, namespace, payload))
    }
}
