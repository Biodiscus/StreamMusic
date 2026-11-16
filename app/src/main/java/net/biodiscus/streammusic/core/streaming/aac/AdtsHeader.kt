package net.biodiscus.streammusic.core.streaming.aac

private const val ADTS_HEADER_SIZE = 7
private const val AAC_LC_PROFILE = 1 // ADTS stores the MPEG-4 audio object type minus one; AAC-LC is type 2.
private const val SAMPLE_RATE_INDEX_48_KHZ = 3
private const val STEREO_CHANNEL_CONFIGURATION = 2

/**
 * An ADTS header is the small prefix that lets a player find the start of every AAC frame in a live stream.
 * This one describes AAC-LC, 48 kHz, stereo, which is what [AacEncoder] produces.
 */
internal fun adtsHeaderFor(aacFrameSize: Int): ByteArray {
    val frameLengthWithHeader = aacFrameSize + ADTS_HEADER_SIZE
    return byteArrayOf(
        0xFF.toByte(),
        0xF1.toByte(),
        ((AAC_LC_PROFILE shl 6) or (SAMPLE_RATE_INDEX_48_KHZ shl 2) or (STEREO_CHANNEL_CONFIGURATION shr 2)).toByte(),
        (((STEREO_CHANNEL_CONFIGURATION and 3) shl 6) or (frameLengthWithHeader shr 11)).toByte(),
        ((frameLengthWithHeader shr 3) and 0xFF).toByte(),
        (((frameLengthWithHeader and 7) shl 5) or 0x1F).toByte(),
        0xFC.toByte(),
    )
}
