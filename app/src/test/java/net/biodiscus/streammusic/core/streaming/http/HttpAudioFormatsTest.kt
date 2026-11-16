package net.biodiscus.streammusic.core.streaming.http

import java.nio.ByteBuffer
import java.nio.ByteOrder
import net.biodiscus.streammusic.core.model.StreamAudioFormat
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HttpAudioFormatsTest {

    private val wavHeader = ByteBuffer.wrap(buildLiveWavHeader()).order(ByteOrder.LITTLE_ENDIAN)

    @Test
    fun `a WAV header is 44 bytes long`() {
        assertEquals(44, buildLiveWavHeader().size)
    }

    @Test
    fun `a WAV header starts with RIFF and WAVE`() {
        val header = buildLiveWavHeader()

        assertEquals("RIFF", String(header, 0, 4))
        assertEquals("WAVE", String(header, 8, 4))
    }

    @Test
    fun `a WAV header describes 16-bit stereo PCM at 48 kHz`() {
        assertEquals(1, wavHeader.getShort(20).toInt())
        assertEquals(2, wavHeader.getShort(22).toInt())
        assertEquals(48_000, wavHeader.getInt(24))
        assertEquals(16, wavHeader.getShort(34).toInt())
    }

    @Test
    fun `a WAV header announces the bytes per second and per frame`() {
        assertEquals(192_000, wavHeader.getInt(28))
        assertEquals(4, wavHeader.getShort(32).toInt())
    }

    @Test
    fun `a WAV header marks the length as unknown`() {
        assertEquals(-1, wavHeader.getInt(4))
        assertEquals(-1, wavHeader.getInt(40))
    }

    @Test
    fun `only a WAV stream starts with a header`() {
        assertTrue(streamPreambleOf(StreamAudioFormat.WAV).isNotEmpty())
        assertTrue(streamPreambleOf(StreamAudioFormat.AAC).isEmpty())
        assertTrue(streamPreambleOf(StreamAudioFormat.LPCM).isEmpty())
    }

    @Test
    fun `swaps the two bytes of every sample`() {
        val littleEndian = byteArrayOf(0x01, 0x02, 0x03, 0x04)

        assertArrayEquals(byteArrayOf(0x02, 0x01, 0x04, 0x03), toBigEndianSamples(littleEndian))
    }

    @Test
    fun `swapping the bytes of silence changes nothing`() {
        assertArrayEquals(ByteArray(8), toBigEndianSamples(ByteArray(8)))
    }
}
