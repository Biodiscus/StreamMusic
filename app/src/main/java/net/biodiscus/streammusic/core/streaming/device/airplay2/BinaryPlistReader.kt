package net.biodiscus.streammusic.core.streaming.device.airplay2

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

/** Dictionaries come back as Map<String, Any?> and lists as List<Any?>. Integers are always Long. */
internal fun decodeBinaryPlist(bytes: ByteArray): Any? {
    val hasHeader = bytes.size >= BINARY_PLIST_HEADER.size && bytes.copyOf(BINARY_PLIST_HEADER.size).contentEquals(BINARY_PLIST_HEADER)
    if (!hasHeader || bytes.size < BINARY_PLIST_HEADER.size + TRAILER_SIZE) throw IOException("Not a binary property list")
    return BinaryPlistReader(bytes).readRoot()
}

private class BinaryPlistReader(private val bytes: ByteArray) {
    private val buffer = ByteBuffer.wrap(bytes)
    private val offsetSize: Int
    private val referenceSize: Int
    private val objectCount: Int
    private val rootObjectNumber: Int
    private val offsetTableOffset: Int

    init {
        val trailerStart = bytes.size - TRAILER_SIZE
        offsetSize = bytes[trailerStart + 6].toInt()
        referenceSize = bytes[trailerStart + 7].toInt()
        objectCount = buffer.getLong(trailerStart + 8).toInt()
        rootObjectNumber = buffer.getLong(trailerStart + 16).toInt()
        offsetTableOffset = buffer.getLong(trailerStart + 24).toInt()
    }

    fun readRoot(): Any? = readObject(rootObjectNumber)

    private fun readObject(objectNumber: Int): Any? {
        if (objectNumber !in 0 until objectCount) throw IOException("The property list refers to a missing object")
        val offset = readUnsigned(offsetTableOffset + objectNumber * offsetSize, offsetSize).toInt()
        val firstByte = bytes[offset].toInt() and 0xFF
        val type = firstByte shr 4
        val lowNibble = firstByte and 0x0F
        return when (type) {
            TYPE_SPECIAL -> readSpecial(lowNibble)
            TYPE_INTEGER -> readInteger(offset + 1, size = 1 shl lowNibble)
            TYPE_REAL -> readReal(offset + 1, size = 1 shl lowNibble)
            TYPE_DATE -> buffer.getDouble(offset + 1)
            TYPE_UID -> readUnsigned(offset + 1, size = lowNibble + 1)
            else -> readObjectWithCount(type, offset, lowNibble)
        }
    }

    private fun readObjectWithCount(type: Int, offset: Int, lowNibble: Int): Any {
        val (count, contentStart) = readCount(offset, lowNibble)
        return when (type) {
            TYPE_DATA -> bytes.copyOfRange(contentStart, contentStart + count)
            TYPE_ASCII_STRING -> String(bytes, contentStart, count, StandardCharsets.US_ASCII)
            TYPE_UNICODE_STRING -> String(bytes, contentStart, count * 2, StandardCharsets.UTF_16BE)
            TYPE_ARRAY, TYPE_SET -> readList(contentStart, count)
            TYPE_DICTIONARY -> readDictionary(contentStart, count)
            else -> throw IOException("Unknown property list type $type")
        }
    }

    private fun readSpecial(code: Int): Any? =
        when (code) {
            SPECIAL_NULL -> null
            SPECIAL_FALSE -> false
            SPECIAL_TRUE -> true
            else -> throw IOException("Unknown special value $code")
        }

    private fun readInteger(start: Int, size: Int): Long = if (size == 8) buffer.getLong(start) else readUnsigned(start, size)

    private fun readReal(start: Int, size: Int): Double = if (size == 4) buffer.getFloat(start).toDouble() else buffer.getDouble(start)

    /** Returns the count and where the content starts. */
    private fun readCount(offset: Int, lowNibble: Int): Pair<Int, Int> {
        if (lowNibble != COUNT_IN_FOLLOWING_INTEGER) return lowNibble to offset + 1
        val integerSize = 1 shl (bytes[offset + 1].toInt() and 0x0F)
        val count = readUnsigned(offset + 2, integerSize).toInt()
        return count to offset + 2 + integerSize
    }

    private fun readList(start: Int, count: Int): List<Any?> =
        List(count) { index -> readObject(readReference(start, index)) }

    /** The references to all keys come first, followed by the references to all values. */
    private fun readDictionary(start: Int, count: Int): Map<String, Any?> {
        val entries = LinkedHashMap<String, Any?>()
        for (index in 0 until count) {
            val key = readObject(readReference(start, index)).toString()
            entries[key] = readObject(readReference(start, count + index))
        }
        return entries
    }

    private fun readReference(start: Int, index: Int): Int = readUnsigned(start + index * referenceSize, referenceSize).toInt()

    private fun readUnsigned(start: Int, size: Int): Long {
        var value = 0L
        for (index in 0 until size) value = (value shl 8) or (bytes[start + index].toLong() and 0xFF)
        return value
    }
}
