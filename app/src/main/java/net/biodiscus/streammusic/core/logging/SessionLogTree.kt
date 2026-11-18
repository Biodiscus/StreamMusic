package net.biodiscus.streammusic.core.logging

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber

private const val ENTRIES_KEPT = 500

/**
 * Keeps the newest log lines of this app session in memory, for the log screen. Nothing is written to disk,
 * so the log is gone when the app process ends.
 *
 * It extends [Timber.DebugTree] only to get the class name of the caller as the tag. It never writes to logcat.
 */
@Singleton
class SessionLogTree internal constructor(
    private val entriesKept: Int,
    private val currentTimeMillis: () -> Long,
) : Timber.DebugTree(), SessionLog {

    @Inject
    constructor() : this(ENTRIES_KEPT, System::currentTimeMillis)

    private val newestEntries = MutableStateFlow<List<LogEntry>>(emptyList())

    override val entries: StateFlow<List<LogEntry>> = newestEntries.asStateFlow()

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val entry = LogEntry(
            timestampMillis = currentTimeMillis(),
            level = priority.toLogLevel(),
            tag = tag,
            message = message,
            stackTrace = t?.stackTraceToString(),
        )
        newestEntries.update { entries -> (entries + entry).takeLast(entriesKept) }
    }
}

private fun Int.toLogLevel(): LogLevel = when (this) {
    Log.ERROR, Log.ASSERT -> LogLevel.ERROR
    Log.WARN -> LogLevel.WARNING
    Log.INFO -> LogLevel.INFO
    else -> LogLevel.DEBUG
}
