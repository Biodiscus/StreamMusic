package net.biodiscus.streammusic.core.protocolchoices

import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.biodiscus.streammusic.core.model.DeviceProtocol

internal fun encodeProtocolChoices(choices: Map<String, DeviceProtocol>): String = Json.encodeToString(choices)

/** Saved choices that cannot be read (for example after a protocol was removed) count as no choices. */
internal fun decodeProtocolChoices(text: String?): Map<String, DeviceProtocol> {
    if (text == null) return emptyMap()
    return try {
        Json.decodeFromString<Map<String, DeviceProtocol>>(text)
    } catch (_: SerializationException) {
        emptyMap()
    }
}
