package net.biodiscus.streammusic.core.discovery.mdns

import net.biodiscus.streammusic.core.model.DeviceProtocol

internal data class MdnsServiceKind(
    val serviceType: String,
    val protocol: DeviceProtocol,
)

/** The Bonjour service types we browse for, and the kind of device each one announces. */
internal val MDNS_SERVICE_KINDS = listOf(
    MdnsServiceKind("_googlecast._tcp", DeviceProtocol.CHROMECAST),
    MdnsServiceKind("_airplay._tcp", DeviceProtocol.AIRPLAY),
    MdnsServiceKind("_raop._tcp", DeviceProtocol.AIRPLAY),
    MdnsServiceKind("_audiocast._tcp", DeviceProtocol.ARIACAST),
    MdnsServiceKind("_amzn-wplay._tcp", DeviceProtocol.FIRE_TV),
    MdnsServiceKind("_spotify-connect._tcp", DeviceProtocol.SPOTIFY_CONNECT),
    MdnsServiceKind("_soundtouch._tcp", DeviceProtocol.BOSE_SOUNDTOUCH),
)

/**
 * Chromecast announces a technical id as service name and keeps the name the user chose in the
 * `fn` text record. AirPlay audio (`_raop`) prefixes its name with the MAC address and an `@`.
 */
internal fun friendlyMdnsName(
    protocol: DeviceProtocol,
    serviceName: String,
    textRecords: Map<String, String>,
): String =
    when (protocol) {
        DeviceProtocol.CHROMECAST -> textRecords["fn"] ?: serviceName
        DeviceProtocol.AIRPLAY -> serviceName.substringAfter("@")
        else -> serviceName
    }
