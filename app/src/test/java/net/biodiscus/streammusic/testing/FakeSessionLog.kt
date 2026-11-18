package net.biodiscus.streammusic.testing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.biodiscus.streammusic.core.logging.LogEntry
import net.biodiscus.streammusic.core.logging.LogLevel
import net.biodiscus.streammusic.core.logging.SessionLog

class FakeSessionLog : SessionLog {

    private val loggedEntries = MutableStateFlow<List<LogEntry>>(emptyList())

    override val entries: StateFlow<List<LogEntry>> = loggedEntries

    fun log(vararg newEntries: LogEntry) {
        loggedEntries.value = loggedEntries.value + newEntries
    }
}

fun aLogEntry(
    message: String = "Streaming",
    level: LogLevel = LogLevel.INFO,
    tag: String? = "StreamingSession",
    timestampMillis: Long = 0L,
    stackTrace: String? = null,
) = LogEntry(timestampMillis, level, tag, message, stackTrace)
