package net.biodiscus.streammusic.core.streaming.http

import java.nio.ByteBuffer
import java.nio.ByteOrder
import net.biodiscus.streammusic.core.model.StreamAudioFormat
import net.biodiscus.streammusic.core.streaming.PcmFormat

private const val WAV_HEADER_BYTES = 44
private const val WAV_FORMAT_CHUNK_BYTES = 16
private const val WAV_PCM_FORMAT_CODE: Short = 1
private const val UNKNOWN_LENGTH = -1
private const val BITS_PER_BYTE = 8

internal fun httpContentTypeOf(format: StreamAudioFormat): String =
    when (format) {
        StreamAudioFormat.AAC -> "audio/aac"
        StreamAudioFormat.LPCM -> "audio/L16;rate=${PcmFormat.SAMPLE_RATE_HZ};channels=${PcmFormat.CHANNEL_COUNT}"
        StreamAudioFormat.WAV -> "audio/wav"
    }

internal fun urlExtensionOf(format: StreamAudioFormat): String =
    when (format) {
        StreamAudioFormat.AAC -> "aac"
        StreamAudioFormat.LPCM -> "l16"
        StreamAudioFormat.WAV -> "wav"
    }

/** What every listener gets before the audio, because a listener can join while the stream is already running. */
internal fun streamPreambleOf(format: StreamAudioFormat): ByteArray =
    when (format) {
        StreamAudioFormat.WAV -> buildLiveWavHeader()
        StreamAudioFormat.AAC, StreamAudioFormat.LPCM -> ByteArray(0)
    }

/**
 * The header of a WAV file with PCM audio of a length nobody knows, because the stream never ends.
 * Players treat the maximum length as "keep playing".
 */
internal fun buildLiveWavHeader(): ByteArray {
    val bytesPerFrame = PcmFormat.CHANNEL_COUNT * PcmFormat.BYTES_PER_SAMPLE
    val header = ByteBuffer.allocate(WAV_HEADER_BYTES).order(ByteOrder.LITTLE_ENDIAN)
    header.put("RIFF".toByteArray())
    header.putInt(UNKNOWN_LENGTH)
    header.put("WAVE".toByteArray())
    header.put("fmt ".toByteArray())
    header.putInt(WAV_FORMAT_CHUNK_BYTES)
    header.putShort(WAV_PCM_FORMAT_CODE)
    header.putShort(PcmFormat.CHANNEL_COUNT.toShort())
    header.putInt(PcmFormat.SAMPLE_RATE_HZ)
    header.putInt(PcmFormat.SAMPLE_RATE_HZ * bytesPerFrame)
    header.putShort(bytesPerFrame.toShort())
    header.putShort((PcmFormat.BYTES_PER_SAMPLE * BITS_PER_BYTE).toShort())
    header.put("data".toByteArray())
    header.putInt(UNKNOWN_LENGTH)
    return header.array()
}

/** The phone records little-endian samples, but the L16 audio type is defined with the high byte first. */
internal fun toBigEndianSamples(littleEndianPcm: ByteArray): ByteArray {
    val bigEndianPcm = ByteArray(littleEndianPcm.size)
    for (index in 0 until littleEndianPcm.size - 1 step PcmFormat.BYTES_PER_SAMPLE) {
        bigEndianPcm[index] = littleEndianPcm[index + 1]
        bigEndianPcm[index + 1] = littleEndianPcm[index]
    }
    return bigEndianPcm
}
