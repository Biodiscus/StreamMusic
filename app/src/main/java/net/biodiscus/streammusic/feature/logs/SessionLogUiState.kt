package net.biodiscus.streammusic.feature.logs

import net.biodiscus.streammusic.core.logging.LogEntry
import net.biodiscus.streammusic.core.logging.LogLevel

internal data class SessionLogUiState(
    val entries: List<LogEntry> = emptyList(),
    val shownLevels: Set<LogLevel> = LogLevel.entries.toSet(),
) {
    /** Newest first, because the newest lines explain the failure the user just saw. */
    val shownEntries: List<LogEntry> =
        entries.filter { entry -> entry.level in shownLevels }.asReversed()

    val totalEntryCount: Int = entries.size
}

internal fun Set<LogLevel>.withLevelToggled(level: LogLevel): Set<LogLevel> =
    if (level in this) this - level else this + level
