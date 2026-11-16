package net.biodiscus.streammusic.core.streaming.device.airplay

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class BitWriterTest {

    @Test
    fun `writes the most significant bit first`() {
        val bits = BitWriter()

        bits.write(0b101, 3)
        bits.write(0b11111, 5)

        assertArrayEquals(byteArrayOf(0b10111111.toByte()), bits.toByteArray())
    }

    @Test
    fun `pads the last byte with zeros`() {
        val bits = BitWriter()

        bits.write(0b11, 2)

        assertArrayEquals(byteArrayOf(0b11000000.toByte()), bits.toByteArray())
    }

    @Test
    fun `keeps only the requested number of low bits`() {
        val bits = BitWriter()

        bits.write(0xFF, 4)

        assertArrayEquals(byteArrayOf(0xF0.toByte()), bits.toByteArray())
    }

    @Test
    fun `writes a value that crosses a byte boundary`() {
        val bits = BitWriter()

        bits.write(0, 4)
        bits.write(0xABC, 12)

        assertArrayEquals(byteArrayOf(0x0A, 0xBC.toByte()), bits.toByteArray())
    }
}
