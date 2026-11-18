package net.biodiscus.streammusic.feature.logs

import java.time.ZoneOffset
import net.biodiscus.streammusic.core.logging.LogLevel
import net.biodiscus.streammusic.testing.aLogEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class LogFileTest {

    private val connecting = aLogEntry(message = "Connecting", level = LogLevel.INFO, tag = "Session", timestampMillis = 1_000L)
    private val failed = aLogEntry(
        message = "Failed",
        level = LogLevel.ERROR,
        tag = "Session",
        timestampMillis = 2_000L,
        stackTrace = "java.io.IOException: Refused\n\tat Streamer.stream(Streamer.kt:10)\n",
    )

    @Test
    fun `writes an empty file when nothing was logged`() {
        assertEquals("", formatLogFileText(emptyList(), ZoneOffset.UTC))
    }

    @Test
    fun `writes one line per entry, oldest first`() {
        val text = formatLogFileText(listOf(connecting, aLogEntry(message = "Playing", tag = null, timestampMillis = 1_500L)), ZoneOffset.UTC)

        assertEquals(
            "00:00:01.000 INFO Session: Connecting\n" +
                "00:00:01.500 INFO Playing\n",
            text,
        )
    }

    @Test
    fun `writes the stack trace under the line of an error`() {
        val text = formatLogFileText(listOf(failed), ZoneOffset.UTC)

        assertEquals(
            "00:00:02.000 ERROR Session: Failed\n" +
                "java.io.IOException: Refused\n" +
                "\tat Streamer.stream(Streamer.kt:10)\n",
            text,
        )
    }

    @Test
    fun `names the file after the date and time it was saved`() {
        val savedAt = (13 * 3600 + 5 * 60 + 9) * 1000L

        assertEquals("streammusic-log-19700101-130509.txt", logFileName(savedAt, ZoneOffset.UTC))
    }
}
