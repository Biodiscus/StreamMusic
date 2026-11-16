package net.biodiscus.streammusic.core.streaming

/**
 * What a screen can ask of the streaming service once it runs.
 * Starting is not here: it needs Android's own permission and screen sharing dialogs first.
 */
internal interface StreamingController {
    fun addDevice(deviceId: String)

    fun removeDevice(deviceId: String)

    fun stop()
}
