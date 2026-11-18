package net.biodiscus.streammusic.core.model

/** How the audio reaches a device of a given protocol. */
enum class StreamTransport {
    /** The phone serves an HTTP audio stream and tells the device to play that URL. */
    DEVICE_PULLS_HTTP_STREAM,

    /** The phone pushes raw PCM audio to the device over a socket. */
    PHONE_PUSHES_PCM,

    /** The device is listed, but this app cannot stream to it. */
    NOT_SUPPORTED,
}

enum class DeviceProtocol(
    val displayName: String,
    val transport: StreamTransport,
) {
    SONOS("Sonos", StreamTransport.DEVICE_PULLS_HTTP_STREAM),
    HEOS("HEOS", StreamTransport.DEVICE_PULLS_HTTP_STREAM),
    SAMSUNG_MULTIROOM("Samsung Multiroom", StreamTransport.DEVICE_PULLS_HTTP_STREAM),
    DLNA("DLNA / UPnP", StreamTransport.DEVICE_PULLS_HTTP_STREAM),
    CHROMECAST("Chromecast", StreamTransport.DEVICE_PULLS_HTTP_STREAM),
    ARIACAST("AriaCast", StreamTransport.PHONE_PUSHES_PCM),
    AIRPLAY("AirPlay (legacy V1)", StreamTransport.PHONE_PUSHES_PCM),
    AIRPLAY_2("AirPlay", StreamTransport.PHONE_PUSHES_PCM),
    FIRE_TV("Fire TV", StreamTransport.NOT_SUPPORTED),
    SPOTIFY_CONNECT("Spotify Connect", StreamTransport.NOT_SUPPORTED),
    BOSE_SOUNDTOUCH("Bose SoundTouch", StreamTransport.NOT_SUPPORTED),
    ;

    /** True when the app can stream to devices of this protocol. The others are only listed. */
    val isImplemented: Boolean = transport != StreamTransport.NOT_SUPPORTED

    val isAirPlay: Boolean get() = this == AIRPLAY || this == AIRPLAY_2
}
