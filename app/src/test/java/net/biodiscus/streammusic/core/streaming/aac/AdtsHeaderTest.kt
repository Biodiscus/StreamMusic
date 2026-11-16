package net.biodiscus.streammusic.core.streaming.aac

import org.junit.Assert.assertEquals
import org.junit.Test

class AdtsHeaderTest {

    @Test
    fun `header is seven bytes`() {
        assertEquals(7, adtsHeaderFor(aacFrameSize = 100).size)
    }

    @Test
    fun `header for a 100 byte frame matches the ADTS layout`() {
        val header = adtsHeaderFor(aacFrameSize = 100)

        assertEquals("FF F1 4C 80 0D 7F FC", header.toHex())
    }

    @Test
    fun `frame length counts the header and uses all 13 bits`() {
        val header = adtsHeaderFor(aacFrameSize = 2_000)

        val frameLength = ((header[3].toInt() and 0x03) shl 11) or
            ((header[4].toInt() and 0xFF) shl 3) or
            ((header[5].toInt() and 0xFF) shr 5)
        assertEquals(2_007, frameLength)
    }

    private fun ByteArray.toHex() = joinToString(" ") { "%02X".format(it) }
}
