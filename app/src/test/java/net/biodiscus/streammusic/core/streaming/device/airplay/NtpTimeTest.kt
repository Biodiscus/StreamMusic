package net.biodiscus.streammusic.core.streaming.device.airplay

import org.junit.Assert.assertEquals
import org.junit.Test

class NtpTimeTest {

    @Test
    fun `the start of 1970 is 2208988800 seconds after the start of 1900`() {
        assertEquals(2_208_988_800L shl 32, ntpTimeFromEpochMillis(0))
    }

    @Test
    fun `half a second is half of the fraction range`() {
        val fraction = ntpTimeFromEpochMillis(500) and 0xFFFFFFFFL

        assertEquals(0x80000000L, fraction)
    }

    @Test
    fun `one second of audio is one sample rate worth of samples`() {
        val oneSecond = 1L shl 32

        assertEquals(44_100L, sampleNumberAt(oneSecond, 44_100))
    }

    @Test
    fun `sample numbers of today do not overflow`() {
        val now = ntpTimeFromEpochMillis(1_763_000_000_000)

        val expectedSamples = (1_763_000_000L + 2_208_988_800L) * 44_100L
        assertEquals(expectedSamples, sampleNumberAt(now, 44_100))
    }

    @Test
    fun `a sample number gives back the time it plays`() {
        assertEquals(1L shl 32, ntpTimeOfSample(44_100, 44_100))
    }

    @Test
    fun `time and sample number convert back and forth`() {
        val sampleNumber = sampleNumberAt(ntpTimeFromEpochMillis(1_763_000_000_000), 44_100)

        val roundTrip = sampleNumberAt(ntpTimeOfSample(sampleNumber, 44_100), 44_100)

        assertEquals(sampleNumber, roundTrip)
    }
}
