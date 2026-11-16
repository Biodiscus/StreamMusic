package net.biodiscus.streammusic.core.streaming.device.cast

import java.io.ByteArrayInputStream
import java.io.DataInputStream
import org.junit.Assert.assertEquals
import org.junit.Test

class CastMessageTest {

    private val connectMessage = CastMessage(
        sourceId = "sender-0",
        destinationId = "receiver-0",
        namespace = "urn:x-cast:com.google.cast.tp.connection",
        payload = """{"type":"CONNECT"}""",
    )

    @Test
    fun `encodes the fields in protobuf field order`() {
        val message = CastMessage("a", "b", "c", "d")

        val protobuf = encodeCastProtobuf(message)

        assertEquals("08 00 12 01 61 1A 01 62 22 01 63 28 00 32 01 64", protobuf.toHex())
    }

    @Test
    fun `decodes what was encoded`() {
        val decoded = decodeCastProtobuf(encodeCastProtobuf(connectMessage))

        assertEquals(connectMessage, decoded)
    }

    @Test
    fun `a payload longer than 127 bytes uses a two byte length`() {
        val longMessage = connectMessage.copy(payload = "x".repeat(300))

        val decoded = decodeCastProtobuf(encodeCastProtobuf(longMessage))

        assertEquals(longMessage, decoded)
    }

    @Test
    fun `non ASCII text survives the round trip`() {
        val message = connectMessage.copy(payload = """{"name":"Küche"}""")

        assertEquals(message, decodeCastProtobuf(encodeCastProtobuf(message)))
    }

    @Test
    fun `frame starts with the protobuf length as four big-endian bytes`() {
        val protobufSize = encodeCastProtobuf(connectMessage).size

        val frame = encodeCastFrame(connectMessage)

        assertEquals(protobufSize, frame.size - 4)
        assertEquals("00 00 00 %02X".format(protobufSize), frame.copyOf(4).toHex())
    }

    @Test
    fun `reads a framed message from a stream`() {
        val input = DataInputStream(ByteArrayInputStream(encodeCastFrame(connectMessage)))

        assertEquals(connectMessage, readCastMessage(input))
    }

    @Test
    fun `reads two framed messages in a row`() {
        val second = connectMessage.copy(payload = """{"type":"PING"}""")
        val bytes = encodeCastFrame(connectMessage) + encodeCastFrame(second)
        val input = DataInputStream(ByteArrayInputStream(bytes))

        assertEquals(listOf(connectMessage, second), listOf(readCastMessage(input), readCastMessage(input)))
    }

    private fun ByteArray.toHex() = joinToString(" ") { "%02X".format(it) }
}
