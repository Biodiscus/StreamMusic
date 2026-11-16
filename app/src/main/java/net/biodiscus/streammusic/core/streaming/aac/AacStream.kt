package net.biodiscus.streammusic.core.streaming.aac

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

/** A running AAC stream that devices can play from `http://<phone>:<port>/stream.aac`. */
internal interface RunningAacStream {
    val port: Int
    fun stop()
}

internal fun interface AacStreamStarter {
    fun start(scope: CoroutineScope, pcmFrames: Flow<ByteArray>): RunningAacStream
}
