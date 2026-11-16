package net.biodiscus.streammusic.core.streaming.device.airplay

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.biodiscus.streammusic.core.streaming.PcmFormat

private const val MILLIS_PER_SECOND = 1000L

/** A delay is a whole number of captured frames, so it is rounded down to a multiple of one frame (20 ms). */
private fun capturedFramesIn(delayMillis: Int): Int = delayMillis / PcmFormat.FRAME_DURATION_MILLIS

/**
 * Holds the audio back by [delayMillis]: a frame is passed on when the frame that comes [delayMillis] later arrives.
 * The audio keeps its pace, it only starts later.
 */
internal fun Flow<ByteArray>.delayedByMillis(delayMillis: Int): Flow<ByteArray> {
    val delayedFrameCount = capturedFramesIn(delayMillis)
    if (delayedFrameCount <= 0) return this
    return flow {
        val waitingFrames = ArrayDeque<ByteArray>()
        collect { frame ->
            waitingFrames.addLast(frame)
            if (waitingFrames.size > delayedFrameCount) emit(waitingFrames.removeFirst())
        }
    }
}

/**
 * How many AirPlay samples later the timeline starts, so the delayed audio is timestamped for the moment it
 * is really sent and the receiver plays it that much later.
 */
internal fun airPlaySamplesDelayedBy(delayMillis: Int): Long =
    capturedFramesIn(delayMillis) * PcmFormat.FRAME_DURATION_MILLIS * RAOP_SAMPLE_RATE_HZ / MILLIS_PER_SECOND
