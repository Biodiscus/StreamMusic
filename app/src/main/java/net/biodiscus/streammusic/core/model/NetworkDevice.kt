package net.biodiscus.streammusic.core.model

private val STREAMING_PREFERENCE = listOf(
    DeviceProtocol.SONOS,
    DeviceProtocol.HEOS,
    DeviceProtocol.SAMSUNG_MULTIROOM,
    DeviceProtocol.CHROMECAST,
    DeviceProtocol.ARIACAST,
    DeviceProtocol.DLNA,
)

/**
 * One physical speaker or TV. The network search finds it once for every protocol it speaks
 * (a TV can be both Chromecast and DLNA), and [options] holds all of those finds.
 * To stream, the app uses [bestStreamingOption].
 */
data class NetworkDevice(
    val host: String,
    val options: List<StreamingDevice>,
) {
    init {
        require(options.isNotEmpty()) { "A device needs at least one way to find it" }
    }

    val id: String = host

    val streamingOptions: List<StreamingDevice> =
        options.filter { option -> option.canReceiveStream }.sortedBy { option -> preferenceRankOf(option.protocol) }

    val bestStreamingOption: StreamingDevice? = streamingOptions.firstOrNull()

    val canReceiveStream: Boolean = bestStreamingOption != null

    val name: String = (bestStreamingOption ?: options.first()).name

    /** The protocols to show: the ones that can stream, or all of them when none can. */
    val protocols: List<DeviceProtocol> =
        (if (canReceiveStream) streamingOptions else options).map { option -> option.protocol }
}

fun List<StreamingDevice>.groupIntoNetworkDevices(): List<NetworkDevice> {
    val optionsByHost = groupBy { device -> device.host }
    return optionsByHost
        .map { entry -> NetworkDevice(host = entry.key, options = entry.value) }
        .sortedBy { networkDevice -> networkDevice.name.lowercase() }
}

private fun preferenceRankOf(protocol: DeviceProtocol): Int = STREAMING_PREFERENCE.indexOf(protocol)
