package net.biodiscus.streammusic.core.model

/** The audio format of the HTTP stream that devices pull. AAC is what the app always streamed. */
enum class StreamAudioFormat(val displayName: String) {
    AAC("AAC"),
    LPCM("LPCM"),
    WAV("WAV"),
}

/**
 * A group of protocols that the user switches on or off together, because they are one kind of device to them.
 * [selectableAudioFormats] is empty when the devices do not pull an HTTP stream, or when there is no choice to make.
 */
enum class ProtocolFamily(
    val displayName: String,
    val protocols: Set<DeviceProtocol>,
    val selectableAudioFormats: List<StreamAudioFormat> = emptyList(),
) {
    AIRPLAY("AirPlay", setOf(DeviceProtocol.AIRPLAY)),
    AIRPLAY_2("AirPlay 2", setOf(DeviceProtocol.AIRPLAY_2)),
    ARIACAST("AriaCast", setOf(DeviceProtocol.ARIACAST)),
    DLNA("DLNA / UPnP", setOf(DeviceProtocol.DLNA), listOf(StreamAudioFormat.AAC, StreamAudioFormat.LPCM, StreamAudioFormat.WAV)),
    GOOGLE_CAST("Google Cast", setOf(DeviceProtocol.CHROMECAST), listOf(StreamAudioFormat.AAC, StreamAudioFormat.WAV)),
    HEOS("HEOS", setOf(DeviceProtocol.HEOS)),
    SAMSUNG_MULTIROOM(
        "Samsung Multiroom",
        setOf(DeviceProtocol.SAMSUNG_MULTIROOM),
        listOf(StreamAudioFormat.AAC, StreamAudioFormat.WAV),
    ),
    SONOS("Sonos", setOf(DeviceProtocol.SONOS), listOf(StreamAudioFormat.AAC, StreamAudioFormat.WAV)),
}

const val MAX_AIRPLAY_AUDIO_DELAY_MILLIS = 5_000
const val AIRPLAY_AUDIO_DELAY_STEP_MILLIS = 100

data class AppSettings(
    /** Devices that are found with one of these protocols are not shown. */
    val disabledProtocols: Set<DeviceProtocol> = emptySet(),
    /** Asks Android to share the whole screen only, without the choice to share a single app. */
    val hidesSingleAppCapture: Boolean = false,
    /** The format the user chose per family. A family without an entry streams AAC. */
    val audioFormats: Map<ProtocolFamily, StreamAudioFormat> = emptyMap(),
    /** How much later AirPlay and AirPlay 2 devices play than the phone sends, to line them up with devices that buffer more. */
    val airPlayAudioDelayMillis: Int = 0,
    /** Stops streaming to a Chromecast as soon as it is muted. */
    val disconnectsChromecastOnMute: Boolean = false,
)

fun AppSettings.isEnabled(family: ProtocolFamily): Boolean =
    family.protocols.none { protocol -> protocol in disabledProtocols }

fun AppSettings.withFamilyEnabled(family: ProtocolFamily, isEnabled: Boolean): AppSettings {
    val newDisabledProtocols = if (isEnabled) disabledProtocols - family.protocols else disabledProtocols + family.protocols
    return copy(disabledProtocols = newDisabledProtocols)
}

fun List<StreamingDevice>.withoutDisabledProtocols(settings: AppSettings): List<StreamingDevice> =
    filter { device -> device.protocol !in settings.disabledProtocols }

fun AppSettings.audioFormatOf(family: ProtocolFamily): StreamAudioFormat {
    val chosenFormat = audioFormats[family]
    val isChoiceStillOffered = chosenFormat != null && chosenFormat in family.selectableAudioFormats
    return if (isChoiceStillOffered) chosenFormat else StreamAudioFormat.AAC
}

/** The format of the stream a device of this protocol pulls. */
fun AppSettings.audioFormatFor(protocol: DeviceProtocol): StreamAudioFormat {
    val family = ProtocolFamily.entries.firstOrNull { candidate -> protocol in candidate.protocols }
    return if (family == null) StreamAudioFormat.AAC else audioFormatOf(family)
}

fun AppSettings.withAudioFormat(family: ProtocolFamily, format: StreamAudioFormat): AppSettings =
    copy(audioFormats = audioFormats + (family to format))
