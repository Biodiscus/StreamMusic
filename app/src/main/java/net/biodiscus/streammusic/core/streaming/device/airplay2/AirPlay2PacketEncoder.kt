package net.biodiscus.streammusic.core.streaming.device.airplay2

import net.biodiscus.streammusic.core.streaming.device.airplay.AudioPacketEncoder
import net.biodiscus.streammusic.core.streaming.device.airplay.buildAudioPacket
import net.biodiscus.streammusic.core.streaming.device.airplay.nonceFromCounter
import net.biodiscus.streammusic.core.streaming.device.airplay.sealWithChaCha20Poly1305

private const val RTP_HEADER_SIZE = 12
private const val TIMESTAMP_AND_SOURCE_START = 4
private const val NONCE_COUNTER_START = 4
private const val BYTES_PER_SAMPLE = 2

/**
 * AirPlay 2: raw PCM, sent as big-endian samples and encrypted with the key from the stream setup.
 * The timestamp and source id of the header are protected too. The 8 byte nonce counter is added after the audio.
 */
internal class ChaChaPcmPacketEncoder(private val sharedKey: ByteArray) : AudioPacketEncoder {
    private var packetCounter = 0L

    override fun encode(
        sequenceNumber: Int,
        timestamp: Long,
        sourceId: Int,
        isFirstPacket: Boolean,
        pcmChunk: ByteArray,
    ): ByteArray {
        val header = buildAudioPacket(sequenceNumber, timestamp, sourceId, isFirstPacket, payload = ByteArray(0))
        val nonce = nonceFromCounter(packetCounter++)
        val protectedHeaderPart = header.copyOfRange(TIMESTAMP_AND_SOURCE_START, RTP_HEADER_SIZE)
        val sealedAudio = sealWithChaCha20Poly1305(sharedKey, nonce, toBigEndianSamples(pcmChunk), protectedHeaderPart)
        return header + sealedAudio + nonce.copyOfRange(NONCE_COUNTER_START, nonce.size)
    }
}

/** Swaps the two bytes of every 16-bit sample. */
internal fun toBigEndianSamples(littleEndianPcm: ByteArray): ByteArray {
    val swapped = ByteArray(littleEndianPcm.size)
    for (index in littleEndianPcm.indices step BYTES_PER_SAMPLE) {
        swapped[index] = littleEndianPcm[index + 1]
        swapped[index + 1] = littleEndianPcm[index]
    }
    return swapped
}
