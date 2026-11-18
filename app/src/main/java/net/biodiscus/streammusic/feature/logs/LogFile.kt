package net.biodiscus.streammusic.feature.logs

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import net.biodiscus.streammusic.core.logging.LogEntry

private val LOG_FILE_NAME_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")

/** Oldest line first, like a log file is read. Every line is followed by its stack trace, if it has one. */
internal fun formatLogFileText(entries: List<LogEntry>, zone: ZoneId = ZoneId.systemDefault()): String =
    entries.joinToString(separator = "") { entry -> formatLogLineWithStackTrace(entry, zone) + "\n" }

internal fun logFileName(timestampMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String {
    val time = LOG_FILE_NAME_TIME_FORMAT.format(Instant.ofEpochMilli(timestampMillis).atZone(zone))
    return "streammusic-log-$time.txt"
}

private fun formatLogLineWithStackTrace(entry: LogEntry, zone: ZoneId): String {
    val line = formatLogLine(entry, zone)
    val stackTrace = entry.stackTrace ?: return line
    return "$line\n${stackTrace.trimEnd()}"
}
