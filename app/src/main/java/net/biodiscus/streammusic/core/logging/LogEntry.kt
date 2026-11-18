package net.biodiscus.streammusic.core.logging

enum class LogLevel { DEBUG, INFO, WARNING, ERROR }

data class LogEntry(
    val timestampMillis: Long,
    val level: LogLevel,
    val tag: String?,
    val message: String,
    val stackTrace: String?,
)
