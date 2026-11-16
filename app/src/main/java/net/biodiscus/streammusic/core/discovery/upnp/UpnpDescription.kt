package net.biodiscus.streammusic.core.discovery.upnp

internal data class UpnpDescription(
    val friendlyName: String?,
    val roomName: String?,
    val manufacturer: String?,
    val modelName: String?,
    val avTransportControlUrl: String?,
) {
    /** Sonos puts the name the user chose in `roomName`; other brands only have `friendlyName`. */
    fun displayName(fallback: String): String = roomName ?: friendlyName ?: fallback
}
