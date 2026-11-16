package net.biodiscus.streammusic.core.streaming.device.airplay

/** Turns one chunk of PCM audio (352 frames, 44.1 kHz, 16-bit little-endian stereo) into the RTP packet to send. */
internal fun interface AudioPacketEncoder {
    fun encode(sequenceNumber: Int, timestamp: Long, sourceId: Int, isFirstPacket: Boolean, pcmChunk: ByteArray): ByteArray
}

/** AirPlay 1: Apple Lossless, optionally encrypted with AES. */
internal class AlacAudioPacketEncoder(private val audioKey: RaopAudioKey?) : AudioPacketEncoder {

    override fun encode(
        sequenceNumber: Int,
        timestamp: Long,
        sourceId: Int,
        isFirstPacket: Boolean,
        pcmChunk: ByteArray,
    ): ByteArray {
        val alacFrame = wrapPcmInAlacFrame(pcmChunk)
        val payload = if (audioKey == null) alacFrame else encryptAudioPayload(alacFrame, audioKey)
        return buildAudioPacket(sequenceNumber, timestamp, sourceId, isFirstPacket, payload)
    }
}
