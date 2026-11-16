package net.biodiscus.streammusic.core.groups

import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.biodiscus.streammusic.core.model.DeviceGroup

/** [coerceInputValues] gives a group the default icon or colour when its saved one does not exist any more. */
private val json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

internal fun encodeDeviceGroups(groups: List<DeviceGroup>): String = json.encodeToString(groups)

/** Saved groups that cannot be read (for example after a protocol was removed) count as no groups. */
internal fun decodeDeviceGroups(text: String?): List<DeviceGroup> {
    if (text == null) return emptyList()
    return try {
        json.decodeFromString<List<DeviceGroup>>(text)
    } catch (_: SerializationException) {
        emptyList()
    }
}
