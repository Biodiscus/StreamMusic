package net.biodiscus.streammusic.core.settings

import net.biodiscus.streammusic.core.model.ProtocolFamily
import net.biodiscus.streammusic.core.model.StreamAudioFormat

private const val FAMILY_AND_FORMAT_SEPARATOR = ":"

internal fun encodeAudioFormats(formats: Map<ProtocolFamily, StreamAudioFormat>): Set<String> =
    formats.map { (family, format) -> "${family.name}$FAMILY_AND_FORMAT_SEPARATOR${format.name}" }.toSet()

/** Entries with a family or format that no longer exists (from an older version of the app) are dropped. */
internal fun decodeAudioFormats(entries: Set<String>): Map<ProtocolFamily, StreamAudioFormat> =
    entries.mapNotNull { entry -> decodeAudioFormatEntry(entry) }.toMap()

private fun decodeAudioFormatEntry(entry: String): Pair<ProtocolFamily, StreamAudioFormat>? {
    val familyName = entry.substringBefore(FAMILY_AND_FORMAT_SEPARATOR)
    val formatName = entry.substringAfter(FAMILY_AND_FORMAT_SEPARATOR, missingDelimiterValue = "")
    val family = ProtocolFamily.entries.firstOrNull { candidate -> candidate.name == familyName } ?: return null
    val format = StreamAudioFormat.entries.firstOrNull { candidate -> candidate.name == formatName } ?: return null
    return family to format
}
