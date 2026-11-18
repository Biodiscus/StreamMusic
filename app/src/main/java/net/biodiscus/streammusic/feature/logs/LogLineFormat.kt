package net.biodiscus.streammusic.feature.logs

import java.time.ZoneId
import net.biodiscus.streammusic.core.logging.LogEntry

internal fun formatLogLine(entry: LogEntry, zone: ZoneId = ZoneId.systemDefault()): String {
    val time = formatLogTime(entry.timestampMillis, zone)
    val tagPrefix = if (entry.tag == null) "" else "${entry.tag}: "
    return "$time ${entry.level.name} $tagPrefix${entry.message}"
}
