package net.biodiscus.streammusic.core.streaming.device.airplay2

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

/** An object of the file, with the numbers of the objects it contains (the items of a list, or keys and values). */
private class NumberedObject(val value: Any?, val childNumbers: List<Int>)

internal fun encodeBinaryPlist(root: Any?): ByteArray {
    val numberedObjects = numberObjects(root)
    return BinaryPlistWriter(numberedObjects).write()
}

/** The first object is the root. Children come after their parent, and each child is referred to by its number. */
private fun numberObjects(root: Any?): List<NumberedObject> {
    val numberedObjects = mutableListOf<NumberedObject?>()

    fun add(value: Any?): Int {
        val number = numberedObjects.size
        numberedObjects.add(null)
        val childNumbers = childrenOf(value).map { child -> add(child) }
        numberedObjects[number] = NumberedObject(value, childNumbers)
        return number
    }

    add(root)
    return numberedObjects.filterNotNull()
}

private fun childrenOf(value: Any?): List<Any?> =
    when (value) {
        is List<*> -> value
        is Map<*, *> -> value.keys.toList() + value.values.toList()
        else -> emptyList()
    }

private class BinaryPlistWriter(private val numberedObjects: List<NumberedObject>) {
    private val file = ByteArrayOutputStream()
    private val referenceSize = bytesNeededFor(numberedObjects.size.toLong())

    fun write(): ByteArray {
        file.write(BINARY_PLIST_HEADER)
        val objectOffsets = numberedObjects.map { numberedObject -> writeObject(numberedObject) }
        val offsetTableOffset = file.size()
        val offsetSize = bytesNeededFor(offsetTableOffset.toLong())
        objectOffsets.forEach { offset -> file.write(unsignedBytes(offset.toLong(), offsetSize)) }
        file.write(trailer(offsetSize, offsetTableOffset))
        return file.toByteArray()
    }

    private fun writeObject(numberedObject: NumberedObject): Int {
        val offset = file.size()
        when (val value = numberedObject.value) {
            null -> file.write(SPECIAL_NULL)
            is Boolean -> file.write(if (value) SPECIAL_TRUE else SPECIAL_FALSE)
            is Int -> writeInteger(value.toLong())
            is Long -> writeInteger(value)
            is Double -> writeReal(value)
            is String -> writeString(value)
            is ByteArray -> writeData(value)
            is List<*> -> writeReferences(TYPE_ARRAY, numberedObject.childNumbers)
            is Map<*, *> -> writeReferences(TYPE_DICTIONARY, numberedObject.childNumbers, count = value.size)
            else -> throw IllegalArgumentException("A property list cannot hold ${value::class.simpleName}")
        }
        return offset
    }

    private fun writeInteger(value: Long) {
        val size = if (value < 0) LONG_SIZE else bytesNeededFor(value)
        file.write((TYPE_INTEGER shl 4) or sizeExponent(size))
        file.write(unsignedBytes(value, size))
    }

    private fun writeReal(value: Double) {
        file.write((TYPE_REAL shl 4) or sizeExponent(LONG_SIZE))
        file.write(ByteBuffer.allocate(LONG_SIZE).putDouble(value).array())
    }

    private fun writeString(value: String) {
        val isAscii = value.all { character -> character.code < 0x80 }
        if (isAscii) {
            writeTypeAndCount(TYPE_ASCII_STRING, value.length)
            file.write(value.toByteArray(StandardCharsets.US_ASCII))
        } else {
            writeTypeAndCount(TYPE_UNICODE_STRING, value.length)
            file.write(value.toByteArray(StandardCharsets.UTF_16BE))
        }
    }

    private fun writeData(value: ByteArray) {
        writeTypeAndCount(TYPE_DATA, value.size)
        file.write(value)
    }

    /** A dictionary holds all its keys first and then all its values, so its count is half of the references. */
    private fun writeReferences(type: Int, childNumbers: List<Int>, count: Int = childNumbers.size) {
        writeTypeAndCount(type, count)
        childNumbers.forEach { number -> file.write(unsignedBytes(number.toLong(), referenceSize)) }
    }

    private fun writeTypeAndCount(type: Int, count: Int) {
        if (count < COUNT_IN_FOLLOWING_INTEGER) {
            file.write((type shl 4) or count)
            return
        }
        file.write((type shl 4) or COUNT_IN_FOLLOWING_INTEGER)
        writeInteger(count.toLong())
    }

    private fun trailer(offsetSize: Int, offsetTableOffset: Int): ByteArray =
        ByteBuffer.allocate(TRAILER_SIZE)
            .put(6, offsetSize.toByte())
            .put(7, referenceSize.toByte())
            .putLong(8, numberedObjects.size.toLong())
            .putLong(16, ROOT_OBJECT_NUMBER.toLong())
            .putLong(24, offsetTableOffset.toLong())
            .array()
}

private const val LONG_SIZE = 8
private const val ROOT_OBJECT_NUMBER = 0

/** The smallest of 1, 2, 4 and 8 bytes that can hold the value. */
private fun bytesNeededFor(value: Long): Int =
    when {
        value < 0x100 -> 1
        value < 0x10000 -> 2
        value < 0x100000000L -> 4
        else -> 8
    }

/** An integer object stores its size as a power of two in the low nibble. */
private fun sizeExponent(size: Int): Int =
    when (size) {
        1 -> 0
        2 -> 1
        4 -> 2
        else -> 3
    }

private fun unsignedBytes(value: Long, size: Int): ByteArray =
    ByteArray(size) { index -> (value shr (8 * (size - 1 - index))).toByte() }
