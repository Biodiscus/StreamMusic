package net.biodiscus.streammusic.core.streaming.device

import net.biodiscus.streammusic.core.model.DeviceProtocol

private const val SONOS_RADIO_SCHEME = "x-rincon-mp3radio://"

/** Sonos only plays a live stream when its address uses Sonos' own radio scheme instead of `http://`. */
internal fun playableUrlFor(protocol: DeviceProtocol, httpStreamUrl: String): String =
    when (protocol) {
        DeviceProtocol.SONOS -> httpStreamUrl.replaceFirst("http://", SONOS_RADIO_SCHEME)
        else -> httpStreamUrl
    }
