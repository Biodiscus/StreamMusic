package net.biodiscus.streammusic.core.streaming.device.cast

import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.nio.ByteBuffer

private const val PROTOCOL_VERSION_FIELD = 1
private const val SOURCE_ID_FIELD = 2
private const val DESTINATION_ID_FIELD = 3
private const val NAMESPACE_FIELD = 4
private const val PAYLOAD_TYPE_FIELD = 5
private const val PAYLOAD_UTF8_FIELD = 6

private const val WIRE_TYPE_VARINT = 0
private const val WIRE_TYPE_64_BIT = 1
private const val WIRE_TYPE_LENGTH_DELIMITED = 2
private const val WIRE_TYPE_32_BIT = 5

private const val CASTV2_1_0 = 0
private const val PAYLOAD_TYPE_STRING = 0
private const val VARINT_CONTINUATION_BIT = 0x80

/**
 * One message of the Chromecast (CASTV2) protocol.
 * On the wire it is a protobuf message behind a 4 byte length. The protobuf is written by hand here
 * because only these four string fields are needed, which avoids a protobuf library.
 */
internal data class CastMessage(
    val sourceId: String,
    val destinationId: String,
    val namespace: String,
    val payload: String,
)

/** The message as protobuf bytes, preceded by its length as a 4 byte big-endian number. */
internal fun encodeCastFrame(message: CastMessage): ByteArray {
    val protobuf = encodeCastProtobuf(message)
    return ByteBuffer.allocate(Int.SIZE_BYTES + protobuf.size).putInt(protobuf.size).put(protobuf).array()
}

internal fun readCastMessage(input: DataInputStream): CastMessage {
    val length = input.readInt()
    val protobuf = ByteArray(length)
    input.readFully(protobuf)
    return decodeCastProtobuf(protobuf)
}

internal fun encodeCastProtobuf(message: CastMessage): ByteArray {
    val out = ByteArrayOutputStream()
    writeVarintField(out, PROTOCOL_VERSION_FIELD, CASTV2_1_0)
    writeStringField(out, SOURCE_ID_FIELD, message.sourceId)
    writeStringField(out, DESTINATION_ID_FIELD, message.destinationId)
    writeStringField(out, NAMESPACE_FIELD, message.namespace)
    writeVarintField(out, PAYLOAD_TYPE_FIELD, PAYLOAD_TYPE_STRING)
    writeStringField(out, PAYLOAD_UTF8_FIELD, message.payload)
    return out.toByteArray()
}

internal fun decodeCastProtobuf(bytes: ByteArray): CastMessage {
    val strings = mutableMapOf<Int, String>()
    val buffer = ByteBuffer.wrap(bytes)
    while (buffer.hasRemaining()) {
        val tag = readVarint(buffer)
        val fieldNumber = (tag shr 3).toInt()
        when ((tag and 7).toInt()) {
            WIRE_TYPE_VARINT -> readVarint(buffer)
            WIRE_TYPE_64_BIT -> buffer.position(buffer.position() + Long.SIZE_BYTES)
            WIRE_TYPE_32_BIT -> buffer.position(buffer.position() + Int.SIZE_BYTES)
            WIRE_TYPE_LENGTH_DELIMITED -> strings[fieldNumber] = readLengthDelimitedString(buffer)
            else -> error("Unsupported protobuf wire type in Cast message")
        }
    }
    return CastMessage(
        sourceId = strings[SOURCE_ID_FIELD].orEmpty(),
        destinationId = strings[DESTINATION_ID_FIELD].orEmpty(),
        namespace = strings[NAMESPACE_FIELD].orEmpty(),
        payload = strings[PAYLOAD_UTF8_FIELD].orEmpty(),
    )
}

private fun writeVarintField(out: ByteArrayOutputStream, fieldNumber: Int, value: Int) {
    writeVarint(out, (fieldNumber shl 3 or WIRE_TYPE_VARINT).toLong())
    writeVarint(out, value.toLong())
}

private fun writeStringField(out: ByteArrayOutputStream, fieldNumber: Int, value: String) {
    val bytes = value.toByteArray(Charsets.UTF_8)
    writeVarint(out, (fieldNumber shl 3 or WIRE_TYPE_LENGTH_DELIMITED).toLong())
    writeVarint(out, bytes.size.toLong())
    out.write(bytes)
}

private fun writeVarint(out: ByteArrayOutputStream, value: Long) {
    var remaining = value
    while (remaining >= VARINT_CONTINUATION_BIT) {
        out.write(((remaining and 0x7F).toInt()) or VARINT_CONTINUATION_BIT)
        remaining = remaining shr 7
    }
    out.write(remaining.toInt())
}

private fun readVarint(buffer: ByteBuffer): Long {
    var result = 0L
    var shift = 0
    while (true) {
        val byte = buffer.get().toInt() and 0xFF
        result = result or ((byte and 0x7F).toLong() shl shift)
        if (byte and VARINT_CONTINUATION_BIT == 0) return result
        shift += 7
    }
}

private fun readLengthDelimitedString(buffer: ByteBuffer): String {
    val length = readVarint(buffer).toInt()
    val bytes = ByteArray(length)
    buffer.get(bytes)
    return String(bytes, Charsets.UTF_8)
}
