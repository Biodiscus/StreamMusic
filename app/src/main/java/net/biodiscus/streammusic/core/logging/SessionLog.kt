package net.biodiscus.streammusic.core.logging

import kotlinx.coroutines.flow.StateFlow

/** The log lines of this app session. The oldest entry comes first. */
interface SessionLog {
    val entries: StateFlow<List<LogEntry>>
}
