package net.biodiscus.streammusic.core.streaming.device.airplay2

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Tlv8Test {

    @Test
    fun `writes tag, length and value for each entry`() {
        val encoded = encodeTlv8(listOf(TlvTag.SEQUENCE_NUMBER to byteArrayOf(1), TlvTag.FLAGS to byteArrayOf(0x10)))

        assertArrayEquals(byteArrayOf(0x06, 1, 1, 0x13, 1, 0x10), encoded)
    }

    @Test
    fun `splits a value of more than 255 bytes over two entries of the same tag`() {
        val encoded = encodeTlv8(listOf(TlvTag.PUBLIC_KEY to ByteArray(300) { 7 }))

        assertEquals(2 + 255 + 2 + 45, encoded.size)
        assertEquals(255, encoded[1].toInt() and 0xFF)
        assertEquals(0x03, encoded[2 + 255].toInt())
        assertEquals(45, encoded[2 + 255 + 1].toInt())
    }

    @Test
    fun `writes an empty value as a tag with length zero`() {
        assertArrayEquals(byteArrayOf(0x01, 0), encodeTlv8(listOf(1 to ByteArray(0))))
    }

    @Test
    fun `reads the entries by tag`() {
        val decoded = decodeTlv8(byteArrayOf(0x06, 1, 2, 0x03, 2, 9, 8))

        assertArrayEquals(byteArrayOf(2), decoded.getValue(TlvTag.SEQUENCE_NUMBER))
        assertArrayEquals(byteArrayOf(9, 8), decoded.getValue(TlvTag.PUBLIC_KEY))
    }

    @Test
    fun `joins the pieces of a long value`() {
        val original = ByteArray(600) { index -> index.toByte() }

        val decoded = decodeTlv8(encodeTlv8(listOf(TlvTag.PUBLIC_KEY to original)))

        assertArrayEquals(original, decoded.getValue(TlvTag.PUBLIC_KEY))
    }

    @Test
    fun `ignores a message that stops in the middle of an entry`() {
        val decoded = decodeTlv8(byteArrayOf(0x06, 1, 2, 0x03, 5, 9))

        assertTrue(decoded.getValue(TlvTag.PUBLIC_KEY).size == 1)
    }

    @Test
    fun `reads nothing from an empty message`() {
        assertTrue(decodeTlv8(ByteArray(0)).isEmpty())
    }
}
