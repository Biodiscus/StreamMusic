package net.biodiscus.streammusic.core.streaming.aac

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.biodiscus.streammusic.core.streaming.PcmFormat

private const val INPUT_WAIT_MICROS = 10_000L
private const val MICROS_PER_MILLI = 1_000L

/** Turns raw PCM frames into a live AAC stream with an ADTS header in front of every frame. */
internal class AacEncoder(private val bitsPerSecond: Int) {

    fun encode(pcmFrames: Flow<ByteArray>): Flow<ByteArray> = flow {
        val codec = createStartedCodec()
        try {
            var frameIndex = 0L
            pcmFrames.collect { pcmFrame ->
                queueInput(codec, pcmFrame, frameIndex)
                frameIndex++
                takeEncodedFrames(codec).forEach { aacFrame -> emit(aacFrame) }
            }
        } finally {
            codec.stop()
            codec.release()
        }
    }

    private fun createStartedCodec(): MediaCodec {
        val format = MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_AAC,
            PcmFormat.SAMPLE_RATE_HZ,
            PcmFormat.CHANNEL_COUNT,
        )
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitsPerSecond)
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, PcmFormat.BYTES_PER_FRAME)

        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()
        return codec
    }

    /** If the encoder has no free input buffer the frame is dropped: live audio must not fall behind. */
    private fun queueInput(codec: MediaCodec, pcmFrame: ByteArray, frameIndex: Long) {
        val bufferIndex = codec.dequeueInputBuffer(INPUT_WAIT_MICROS)
        if (bufferIndex < 0) return
        val inputBuffer = codec.getInputBuffer(bufferIndex) ?: return
        inputBuffer.clear()
        inputBuffer.put(pcmFrame)
        val timestampMicros = frameIndex * PcmFormat.FRAME_DURATION_MILLIS * MICROS_PER_MILLI
        codec.queueInputBuffer(bufferIndex, 0, pcmFrame.size, timestampMicros, 0)
    }

    private fun takeEncodedFrames(codec: MediaCodec): List<ByteArray> {
        val frames = mutableListOf<ByteArray>()
        val info = MediaCodec.BufferInfo()
        while (true) {
            val bufferIndex = codec.dequeueOutputBuffer(info, 0)
            if (bufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) return frames
            if (bufferIndex < 0) continue
            if (!isCodecConfiguration(info)) frames.add(readEncodedFrame(codec, bufferIndex, info))
            codec.releaseOutputBuffer(bufferIndex, false)
        }
    }

    /** The config buffer holds decoder setup data, which ADTS headers already describe. */
    private fun isCodecConfiguration(info: MediaCodec.BufferInfo): Boolean =
        info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0

    private fun readEncodedFrame(codec: MediaCodec, bufferIndex: Int, info: MediaCodec.BufferInfo): ByteArray {
        val outputBuffer = checkNotNull(codec.getOutputBuffer(bufferIndex))
        val payload = ByteArray(info.size)
        outputBuffer.position(info.offset)
        outputBuffer.get(payload)
        return adtsHeaderFor(payload.size) + payload
    }
}
