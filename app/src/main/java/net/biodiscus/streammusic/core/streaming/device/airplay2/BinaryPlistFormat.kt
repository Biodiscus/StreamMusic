package net.biodiscus.streammusic.core.streaming.device.airplay2

/*
 * Apple's binary property list is the format of the AirPlay 2 SETUP messages. Values are
 * Map<String, Any?>, List<Any?>, String, Long, Int, Double, Boolean and ByteArray.
 *
 * File layout: the header, every object, a table with the position of each object, and a 32 byte trailer.
 * Each object starts with one byte: the type in the high nibble and a small number (usually the length) in the low.
 */

internal val BINARY_PLIST_HEADER = "bplist00".toByteArray()
internal const val TRAILER_SIZE = 32

internal const val TYPE_SPECIAL = 0x0
internal const val TYPE_INTEGER = 0x1
internal const val TYPE_REAL = 0x2
internal const val TYPE_DATE = 0x3
internal const val TYPE_DATA = 0x4
internal const val TYPE_ASCII_STRING = 0x5
internal const val TYPE_UNICODE_STRING = 0x6
internal const val TYPE_UID = 0x8
internal const val TYPE_ARRAY = 0xA
internal const val TYPE_SET = 0xC
internal const val TYPE_DICTIONARY = 0xD

internal const val SPECIAL_NULL = 0x0
internal const val SPECIAL_FALSE = 0x8
internal const val SPECIAL_TRUE = 0x9

/** A count of 15 or more does not fit in the low nibble. It follows the first byte as an integer object. */
internal const val COUNT_IN_FOLLOWING_INTEGER = 0xF
