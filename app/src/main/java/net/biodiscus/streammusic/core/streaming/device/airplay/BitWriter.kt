package net.biodiscus.streammusic.core.streaming.device.airplay

import java.io.ByteArrayOutputStream

/** Collects values of any width in bits, most significant bit first. The last byte is padded with zeros. */
internal class BitWriter {
    private val finishedBytes = ByteArrayOutputStream()
    private var unfinishedByte = 0
    private var bitsInUnfinishedByte = 0

    fun write(value: Int, bitCount: Int) {
        for (bitIndex in bitCount - 1 downTo 0) {
            writeBit((value ushr bitIndex) and 1)
        }
    }

    fun toByteArray(): ByteArray {
        if (bitsInUnfinishedByte == 0) return finishedBytes.toByteArray()
        val paddingBits = BITS_PER_BYTE - bitsInUnfinishedByte
        write(0, paddingBits)
        return finishedBytes.toByteArray()
    }

    private fun writeBit(bit: Int) {
        unfinishedByte = (unfinishedByte shl 1) or bit
        bitsInUnfinishedByte++
        if (bitsInUnfinishedByte == BITS_PER_BYTE) finishUnfinishedByte()
    }

    private fun finishUnfinishedByte() {
        finishedBytes.write(unfinishedByte)
        unfinishedByte = 0
        bitsInUnfinishedByte = 0
    }
}

private const val BITS_PER_BYTE = 8
