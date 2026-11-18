package net.biodiscus.streammusic.feature.logs

import net.biodiscus.streammusic.core.logging.LogLevel
import net.biodiscus.streammusic.testing.aLogEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionLogUiStateTest {

    private val connecting = aLogEntry(message = "Connecting", level = LogLevel.INFO)
    private val soapAnswered = aLogEntry(message = "SOAP answered", level = LogLevel.DEBUG)
    private val refused = aLogEntry(message = "Refused", level = LogLevel.WARNING)
    private val failed = aLogEntry(message = "Failed", level = LogLevel.ERROR)

    private val allEntries = listOf(connecting, soapAnswered, refused, failed)

    @Test
    fun `shows the newest entry first`() {
        val state = SessionLogUiState(allEntries)

        assertEquals(listOf(failed, refused, soapAnswered, connecting), state.shownEntries)
    }

    @Test
    fun `shows every level by default`() {
        val state = SessionLogUiState(allEntries)

        assertEquals(4, state.shownEntries.size)
    }

    @Test
    fun `shows only the entries of the chosen levels`() {
        val state = SessionLogUiState(allEntries, shownLevels = setOf(LogLevel.INFO, LogLevel.ERROR))

        assertEquals(listOf(failed, connecting), state.shownEntries)
    }

    @Test
    fun `can show a level without the levels above it`() {
        val state = SessionLogUiState(allEntries, shownLevels = setOf(LogLevel.DEBUG))

        assertEquals(listOf(soapAnswered), state.shownEntries)
    }

    @Test
    fun `shows nothing when no level is chosen`() {
        val state = SessionLogUiState(allEntries, shownLevels = emptySet())

        assertEquals(emptyList<Any>(), state.shownEntries)
    }

    @Test
    fun `toggling a shown level hides it`() {
        val levels = setOf(LogLevel.INFO, LogLevel.ERROR)

        assertEquals(setOf(LogLevel.ERROR), levels.withLevelToggled(LogLevel.INFO))
    }

    @Test
    fun `toggling a hidden level shows it`() {
        val levels = setOf(LogLevel.ERROR)

        assertEquals(setOf(LogLevel.ERROR, LogLevel.WARNING), levels.withLevelToggled(LogLevel.WARNING))
    }

    @Test
    fun `counts the hidden entries in the total`() {
        val state = SessionLogUiState(allEntries, shownLevels = setOf(LogLevel.ERROR))

        assertEquals(4, state.totalEntryCount)
    }
}
