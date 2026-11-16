package net.biodiscus.streammusic.core.streaming.capture

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import net.biodiscus.streammusic.core.streaming.PcmFormat

private const val RECORD_BUFFER_FRAME_COUNT = 8

/**
 * Records everything the phone plays. The user granted [mediaProjection] and the RECORD_AUDIO permission.
 * Apps can opt out of being captured, in which case their audio arrives as silence.
 */
internal class PlaybackAudioCapture(private val mediaProjection: MediaProjection) {

    /** Emits one 20 ms frame of PCM audio at a time until the collector stops. */
    fun frames(): Flow<ByteArray> = flow {
        val audioRecord = createAudioRecord()
        audioRecord.startRecording()
        try {
            while (true) {
                currentCoroutineContext().ensureActive()
                emit(readOneFrame(audioRecord))
            }
        } finally {
            audioRecord.stop()
            audioRecord.release()
        }
    }.flowOn(Dispatchers.IO)

    @SuppressLint("MissingPermission") // The screen asks for RECORD_AUDIO before streaming starts.
    private fun createAudioRecord(): AudioRecord {
        val audioFormat = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(PcmFormat.SAMPLE_RATE_HZ)
            .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
            .build()
        return AudioRecord.Builder()
            .setAudioFormat(audioFormat)
            .setAudioPlaybackCaptureConfig(createCaptureConfiguration())
            .setBufferSizeInBytes(PcmFormat.BYTES_PER_FRAME * RECORD_BUFFER_FRAME_COUNT)
            .build()
    }

    private fun createCaptureConfiguration(): AudioPlaybackCaptureConfiguration {
        val builder = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
        builder.addMatchingUsage(AudioAttributes.USAGE_MEDIA)
        builder.addMatchingUsage(AudioAttributes.USAGE_GAME)
        builder.addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
        return builder.build()
    }

    private fun readOneFrame(audioRecord: AudioRecord): ByteArray {
        val frame = ByteArray(PcmFormat.BYTES_PER_FRAME)
        var bytesRead = 0
        while (bytesRead < frame.size) {
            val result = audioRecord.read(frame, bytesRead, frame.size - bytesRead)
            check(result >= 0) { "Audio capture failed with error code $result" }
            bytesRead += result
        }
        return frame
    }
}
