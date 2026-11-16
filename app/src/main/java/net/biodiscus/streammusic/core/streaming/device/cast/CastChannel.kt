package net.biodiscus.streammusic.core.streaming.device.cast

/** A connection to one Chromecast that sends and receives [CastMessage]s. */
internal interface CastChannel {
    suspend fun send(message: CastMessage)
    suspend fun receive(): CastMessage
    fun close()
}

internal fun interface CastChannelOpener {
    suspend fun open(host: String, port: Int): CastChannel
}
