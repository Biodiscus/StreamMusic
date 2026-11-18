package net.biodiscus.streammusic.feature.logs

import java.time.ZoneOffset
import net.biodiscus.streammusic.core.logging.LogLevel
import net.biodiscus.streammusic.testing.aLogEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class LogLineFormatTest {

    @Test
    fun `puts the time, level, tag and message on one line`() {
        val entry = aLogEntry(message = "Connecting to Kitchen", level = LogLevel.INFO, tag = "StreamingSession", timestampMillis = 1_000L)

        assertEquals(
            "00:00:01.000 INFO StreamingSession: Connecting to Kitchen",
            formatLogLine(entry, ZoneOffset.UTC),
        )
    }

    @Test
    fun `leaves out the tag when there is none`() {
        val entry = aLogEntry(message = "Connecting", level = LogLevel.WARNING, tag = null)

        assertEquals("00:00:00.000 WARNING Connecting", formatLogLine(entry, ZoneOffset.UTC))
    }
}
