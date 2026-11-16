package net.biodiscus.streammusic.core.streaming.device.airplay

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class AlacFrameTest {

    @Test
    fun `wraps one stereo frame as an uncompressed frame`() {
        val leftThenRight = byteArrayOf(0x34, 0x12, 0x78, 0x56)

        val frame = wrapPcmInAlacFrame(leftThenRight)

        val expected = intArrayOf(0x20, 0x00, 0x12, 0x00, 0x00, 0x00, 0x02, 0x24, 0x68, 0xAC, 0xF1, 0xC0)
        assertArrayEquals(expected.map { it.toByte() }.toByteArray(), frame)
    }

    @Test
    fun `starts a full packet of silence with the header receivers expect`() {
        val silence = ByteArray(RAOP_FRAMES_PER_PACKET * 4)

        val frame = wrapPcmInAlacFrame(silence)

        val expectedStart = intArrayOf(0x20, 0x00, 0x12, 0x00, 0x00, 0x02, 0xC0)
        assertArrayEquals(expectedStart.map { it.toByte() }.toByteArray(), frame.copyOf(expectedStart.size))
    }

    @Test
    fun `a full packet takes 1416 bytes`() {
        val silence = ByteArray(RAOP_FRAMES_PER_PACKET * 4)

        assertEquals(1416, wrapPcmInAlacFrame(silence).size)
    }
}
