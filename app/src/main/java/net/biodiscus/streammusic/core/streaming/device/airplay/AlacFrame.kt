package net.biodiscus.streammusic.core.streaming.device.airplay

private const val BYTES_PER_STEREO_FRAME = 4
private const val STEREO_ELEMENT_TAG = 1
private const val END_OF_FRAME_TAG = 7

/**
 * Packs 16-bit little-endian stereo PCM into an Apple Lossless frame without compressing it.
 * Every AirPlay receiver can decode ALAC, and the "not compressed" flag lets us skip writing an encoder.
 */
internal fun wrapPcmInAlacFrame(pcm: ByteArray): ByteArray {
    val frameCount = pcm.size / BYTES_PER_STEREO_FRAME
    val bits = BitWriter()
    writeUncompressedFrameHeader(bits, frameCount)
    writeSamples(bits, pcm, frameCount)
    bits.write(END_OF_FRAME_TAG, 3)
    return bits.toByteArray()
}

private fun writeUncompressedFrameHeader(bits: BitWriter, frameCount: Int) {
    bits.write(STEREO_ELEMENT_TAG, 3)
    bits.write(0, 4) // element instance tag
    bits.write(0, 12) // unused
    bits.write(1, 1) // the frame count follows
    bits.write(0, 2) // no extra bytes per sample
    bits.write(1, 1) // not compressed
    bits.write(frameCount, 32)
}

private fun writeSamples(bits: BitWriter, pcm: ByteArray, frameCount: Int) {
    for (frameIndex in 0 until frameCount) {
        val firstByteIndex = frameIndex * BYTES_PER_STEREO_FRAME
        bits.write(sampleAt(pcm, firstByteIndex), 16)
        bits.write(sampleAt(pcm, firstByteIndex + 2), 16)
    }
}

private fun sampleAt(pcm: ByteArray, byteIndex: Int): Int {
    val lowByte = pcm[byteIndex].toInt() and 0xFF
    val highByte = pcm[byteIndex + 1].toInt() and 0xFF
    return (highByte shl 8) or lowByte
}
