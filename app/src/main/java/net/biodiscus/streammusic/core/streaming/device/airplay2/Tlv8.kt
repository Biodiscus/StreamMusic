package net.biodiscus.streammusic.core.streaming.device.airplay2

import java.io.ByteArrayOutputStream

private const val LARGEST_TLV_VALUE = 255

/** The tags of the pairing messages. */
internal object TlvTag {
    const val METHOD = 0x00
    const val SALT = 0x02
    const val PUBLIC_KEY = 0x03
    const val PROOF = 0x04
    const val SEQUENCE_NUMBER = 0x06
    const val ERROR = 0x07
    const val FLAGS = 0x13
}

/** Writes tag, length and value for each entry. A value longer than 255 bytes is split over several entries. */
internal fun encodeTlv8(entries: List<Pair<Int, ByteArray>>): ByteArray {
    val output = ByteArrayOutputStream()
    for ((tag, value) in entries) {
        for (piece in piecesOf(value)) {
            output.write(tag)
            output.write(piece.size)
            output.write(piece)
        }
    }
    return output.toByteArray()
}

/** An empty value is still written, as one piece without content. */
private fun piecesOf(value: ByteArray): List<ByteArray> {
    if (value.isEmpty()) return listOf(value)
    return value.indices.step(LARGEST_TLV_VALUE).map { start -> value.copyOfRange(start, minOf(start + LARGEST_TLV_VALUE, value.size)) }
}

/** The reverse of [encodeTlv8]: values of the same tag that follow each other are joined again. */
internal fun decodeTlv8(bytes: ByteArray): Map<Int, ByteArray> {
    val valuesByTag = mutableMapOf<Int, ByteArray>()
    var position = 0
    while (position + 2 <= bytes.size) {
        val tag = bytes[position].toInt() and 0xFF
        val length = bytes[position + 1].toInt() and 0xFF
        val end = minOf(position + 2 + length, bytes.size)
        val value = bytes.copyOfRange(position + 2, end)
        valuesByTag[tag] = (valuesByTag[tag] ?: ByteArray(0)) + value
        position = end
    }
    return valuesByTag
}
