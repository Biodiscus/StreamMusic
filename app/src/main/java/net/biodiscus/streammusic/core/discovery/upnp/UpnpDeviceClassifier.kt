package net.biodiscus.streammusic.core.discovery.upnp

import net.biodiscus.streammusic.core.model.DeviceProtocol

internal fun classifyUpnpDevice(searchTarget: String, description: UpnpDescription): DeviceProtocol =
    when {
        isSonos(searchTarget, description) -> DeviceProtocol.SONOS
        isHeos(searchTarget, description) -> DeviceProtocol.HEOS
        isSamsungMultiroomSpeaker(description) -> DeviceProtocol.SAMSUNG_MULTIROOM
        else -> DeviceProtocol.DLNA
    }

private fun isSonos(searchTarget: String, description: UpnpDescription): Boolean =
    searchTarget.contains("ZonePlayer") || description.manufacturer.containsIgnoringCase("sonos")

private fun isHeos(searchTarget: String, description: UpnpDescription): Boolean =
    searchTarget.contains("schemas-denon-com") || description.modelName.containsIgnoringCase("heos")

/** Samsung also makes TVs that are plain DLNA renderers, so the manufacturer alone is not enough. */
private fun isSamsungMultiroomSpeaker(description: UpnpDescription): Boolean {
    val isSamsung = description.manufacturer.containsIgnoringCase("samsung")
    val modelName = description.modelName.orEmpty()
    val isMultiroomModel = modelName.startsWith("WAM", ignoreCase = true) ||
        modelName.contains("multiroom", ignoreCase = true)
    return isSamsung && isMultiroomModel
}

private fun String?.containsIgnoringCase(part: String): Boolean =
    this?.contains(part, ignoreCase = true) == true
