package net.biodiscus.streammusic.core.streaming.device.airplay

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.biodiscus.streammusic.core.streaming.device.SessionAudio

private const val SYNC_INTERVAL_MILLIS = 1_000L

/**
 * What both AirPlay generations do once the session is set up: answer requests for lost packets, send a sync
 * packet every second, keep the session alive, and send the captured audio until the coroutine is cancelled.
 */
internal suspend fun streamAudioForever(
    connection: RaopConnection,
    sender: RaopAudioSender,
    audio: SessionAudio,
    keepAliveIntervalMillis: Long,
    keepAlive: suspend () -> Unit,
    audioDelayMillis: Int,
    onStreaming: () -> Unit,
): Nothing = coroutineScope {
    launch { connection.answerControlRequests { request -> sender.retransmissionsFor(request) } }
    launch { sendSyncPacketsForever(sender) }
    launch { repeatForever(keepAliveIntervalMillis, keepAlive) }
    onStreaming()

    audio.pcmFrames.delayedByMillis(audioDelayMillis).collect { capturedPcm -> sender.sendCapturedAudio(capturedPcm) }
    awaitCancellation()
}

private suspend fun sendSyncPacketsForever(sender: RaopAudioSender): Nothing {
    while (true) {
        delay(SYNC_INTERVAL_MILLIS)
        sender.sendSyncPacket()
    }
}

private suspend fun repeatForever(intervalMillis: Long, action: suspend () -> Unit): Nothing {
    while (true) {
        delay(intervalMillis)
        action()
    }
}
