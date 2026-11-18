package net.biodiscus.streammusic.feature.logs

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val LOG_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

internal fun formatLogTime(timestampMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    LOG_TIME_FORMAT.format(Instant.ofEpochMilli(timestampMillis).atZone(zone))
