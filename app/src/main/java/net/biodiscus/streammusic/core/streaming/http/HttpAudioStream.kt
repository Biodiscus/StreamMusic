package net.biodiscus.streammusic.core.streaming.http

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import net.biodiscus.streammusic.core.model.StreamAudioFormat

/** A running HTTP audio stream that devices can play from `http://<phone>:<port>/stream.<extension>`. */
internal interface RunningHttpAudioStream {
    val port: Int
    val format: StreamAudioFormat
    fun stop()
}

internal fun interface HttpAudioStreamStarter {
    fun start(scope: CoroutineScope, pcmFrames: Flow<ByteArray>, format: StreamAudioFormat): RunningHttpAudioStream
}
