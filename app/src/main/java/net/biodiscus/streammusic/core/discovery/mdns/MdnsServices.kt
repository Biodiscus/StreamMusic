package net.biodiscus.streammusic.core.discovery.mdns

import net.biodiscus.streammusic.core.model.DeviceProtocol
import net.biodiscus.streammusic.core.model.RaopServiceInfo

internal data class MdnsServiceKind(
    val serviceType: String,
    val protocol: DeviceProtocol,
)

/**
 * The Bonjour service types we browse for, and the kind of device each one announces.
 * AirPlay audio is found through `_raop`. `_airplay` is not browsed: it names the same device with another port,
 * and the audio is sent to the `_raop` one.
 */
internal val MDNS_SERVICE_KINDS = listOf(
    MdnsServiceKind("_googlecast._tcp", DeviceProtocol.CHROMECAST),
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
        DeviceProtocol.AIRPLAY, DeviceProtocol.AIRPLAY_2 -> serviceName.substringAfter("@")
        else -> serviceName
    }

private const val SUPPORTS_SYSTEM_PAIRING_BIT = 43
private const val SUPPORTS_CORE_UTILS_PAIRING_BIT = 48

/**
 * The `et` record lists the encryption types the receiver accepts, such as `0,1,3`. A receiver that does not
 * say accepts no encryption. The `pw` record says whether it asks for a password. The `ft` record lists
 * the features, of which two mean that the receiver pairs without a code.
 */
internal fun parseRaopServiceInfo(textRecords: Map<String, String>): RaopServiceInfo {
    val encryptionTypes = textRecords["et"]
        ?.split(",")
        ?.mapNotNull { type -> type.trim().toIntOrNull() }
        ?.toSet()
        ?: setOf(0)
    val needsPassword = textRecords["pw"] in setOf("true", "1")
    val features = parseAirPlayFeatures(textRecords["ft"])
    return RaopServiceInfo(encryptionTypes, needsPassword, supportsTransientPairing = supportsTransientPairing(features))
}

/** Features are written as one or two hexadecimal numbers: `0x1234` or `0x1234,0x5678`, where the second holds the high 32 bits. */
internal fun parseAirPlayFeatures(text: String?): Long {
    val parts = text?.split(",")?.map { part -> part.trim().removePrefix("0x").removePrefix("0X") } ?: return 0
    val low = parts.getOrNull(0)?.toLongOrNull(radix = 16) ?: return 0
    val high = parts.getOrNull(1)?.toLongOrNull(radix = 16) ?: 0
    return (high shl 32) or low
}

private fun supportsTransientPairing(features: Long): Boolean =
    isBitSet(features, SUPPORTS_SYSTEM_PAIRING_BIT) || isBitSet(features, SUPPORTS_CORE_UTILS_PAIRING_BIT)

private fun isBitSet(features: Long, bit: Int): Boolean = (features shr bit) and 1L == 1L

/**
 * What a receiver found with [protocol] can be streamed to. An AirPlay receiver is listed as AirPlay (1),
 * and also as AirPlay 2 when it pairs without a code. A receiver can accept one of them or both.
 */
internal fun protocolsAnnouncedBy(protocol: DeviceProtocol, textRecords: Map<String, String>): List<DeviceProtocol> {
    if (protocol != DeviceProtocol.AIRPLAY) return listOf(protocol)
    val announcesAirPlay2 = parseRaopServiceInfo(textRecords).supportsTransientPairing
    return if (announcesAirPlay2) listOf(DeviceProtocol.AIRPLAY_2, DeviceProtocol.AIRPLAY) else listOf(DeviceProtocol.AIRPLAY)
}
