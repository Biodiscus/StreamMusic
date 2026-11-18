package net.biodiscus.streammusic.core.streaming

import net.biodiscus.streammusic.core.model.SilenceTimeout
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SilenceDetectorTest {

    private val framesInFiveMinutes = 5 * 60 * 1000 / PcmFormat.FRAME_DURATION_MILLIS

    @Test
    fun `a frame of zeros is silent`() {
        assertTrue(isSilentFrame(frameOf(0)))
    }

    @Test
    fun `a frame with a very quiet sample is silent`() {
        assertTrue(isSilentFrame(frameOf(-20)))
    }

    @Test
    fun `a frame with a loud sample is not silent`() {
        assertFalse(isSilentFrame(frameOf(1_000)))
    }

    @Test
    fun `a frame with a loud negative sample is not silent`() {
        assertFalse(isSilentFrame(frameOf(-1_000)))
    }

    @Test
    fun `a frame with one loud sample between silent ones is not silent`() {
        assertFalse(isSilentFrame(frameOf(0, 0, 5_000, 0)))
    }

    @Test
    fun `the audio has not been silent too long before the timeout is reached`() {
        val detector = SilenceDetector(SilenceTimeout.FIVE_MINUTES)

        val results = List(framesInFiveMinutes - 1) { detector.hasBeenSilentTooLong(frameOf(0)) }

        assertTrue(results.none { hasBeenSilentTooLong -> hasBeenSilentTooLong })
    }

    @Test
    fun `the audio has been silent too long when the timeout is reached`() {
        val detector = SilenceDetector(SilenceTimeout.FIVE_MINUTES)

        val results = List(framesInFiveMinutes) { detector.hasBeenSilentTooLong(frameOf(0)) }

        assertTrue(results.last())
    }

    @Test
    fun `sound starts the count again`() {
        val detector = SilenceDetector(SilenceTimeout.FIVE_MINUTES)
        repeat(framesInFiveMinutes - 1) { detector.hasBeenSilentTooLong(frameOf(0)) }
        detector.hasBeenSilentTooLong(frameOf(5_000))

        val hasBeenSilentTooLong = detector.hasBeenSilentTooLong(frameOf(0))

        assertFalse(hasBeenSilentTooLong)
    }

    private fun frameOf(vararg samples: Int): ByteArray =
        samples.flatMap { sample -> listOf(sample.toByte(), (sample shr 8).toByte()) }.toByteArray()
}
