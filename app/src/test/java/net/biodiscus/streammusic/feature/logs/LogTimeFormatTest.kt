package net.biodiscus.streammusic.feature.logs

import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class LogTimeFormatTest {

    @Test
    fun `formats a time with milliseconds`() {
        val timestamp = (13 * 3600 + 5 * 60 + 9) * 1000L + 42

        assertEquals("13:05:09.042", formatLogTime(timestamp, ZoneOffset.UTC))
    }

    @Test
    fun `formats the time in the given time zone`() {
        val oneHourAhead = ZoneOffset.ofHours(1)

        assertEquals("01:00:00.000", formatLogTime(0L, oneHourAhead))
    }
}
