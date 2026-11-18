package net.biodiscus.streammusic.core.streaming

import kotlin.math.abs
import net.biodiscus.streammusic.core.model.SilenceTimeout

private const val MILLIS_PER_MINUTE = 60_000L

// A recording of nothing is not always exactly zero, so very quiet noise counts as silence too.
private const val QUIET_SAMPLE_LIMIT = 32

/** Thrown by the session when the phone played nothing for as long as the user allows. */
internal class StoppedBecauseOfSilenceException(timeout: SilenceTimeout) :
    Exception("The phone played nothing for ${timeout.minutes} minutes")

/** Watches captured frames one by one and tells when they have been silent for the whole [timeout]. */
internal class SilenceDetector(timeout: SilenceTimeout) {

    private val silentFramesBeforeStopping = timeout.minutes * MILLIS_PER_MINUTE / PcmFormat.FRAME_DURATION_MILLIS
    private var silentFramesInARow = 0L

    fun hasBeenSilentTooLong(pcmFrame: ByteArray): Boolean {
        silentFramesInARow = if (isSilentFrame(pcmFrame)) silentFramesInARow + 1 else 0
        return silentFramesInARow >= silentFramesBeforeStopping
    }
}

/** Reads the frame as 16-bit little-endian samples, which is what the capture records. */
internal fun isSilentFrame(pcmFrame: ByteArray): Boolean {
    for (firstByte in pcmFrame.indices step PcmFormat.BYTES_PER_SAMPLE) {
        if (abs(sampleStartingAt(pcmFrame, firstByte)) > QUIET_SAMPLE_LIMIT) return false
    }
    return true
}

private fun sampleStartingAt(pcmFrame: ByteArray, firstByte: Int): Int {
    val lowByte = pcmFrame[firstByte].toInt() and 0xFF
    val highByte = pcmFrame[firstByte + 1].toInt()
    return (highByte shl 8) or lowByte
}
