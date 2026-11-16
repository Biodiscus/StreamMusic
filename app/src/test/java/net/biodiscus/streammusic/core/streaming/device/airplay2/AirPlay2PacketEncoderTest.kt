package net.biodiscus.streammusic.core.streaming.device.airplay2

import java.nio.ByteBuffer
import net.biodiscus.streammusic.core.streaming.device.airplay.nonceFromCounter
import net.biodiscus.streammusic.core.streaming.device.airplay.openWithChaCha20Poly1305
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class AirPlay2PacketEncoderTest {

    private val key = ByteArray(32) { (it + 1).toByte() }
    private val pcm = ByteArray(1408) { (it % 200).toByte() }

    @Test
    fun `swaps the bytes of every sample`() {
        assertArrayEquals(byteArrayOf(2, 1, 4, 3), toBigEndianSamples(byteArrayOf(1, 2, 3, 4)))
    }

    @Test
    fun `a packet is the header, the sealed audio and the 8 byte nonce`() {
        val packet = ChaChaPcmPacketEncoder(key).encode(5, 1000, 77, isFirstPacket = false, pcmChunk = pcm)

        assertEquals(12 + 1408 + 16 + 8, packet.size)
    }

    @Test
    fun `starts with the RTP header`() {
        val packet = ChaChaPcmPacketEncoder(key).encode(5, 1000, 77, isFirstPacket = true, pcmChunk = pcm)

        val header = ByteBuffer.wrap(packet)
        assertEquals(0x80E0, header.getShort(0).toInt() and 0xFFFF)
        assertEquals(5, header.getShort(2).toInt())
        assertEquals(1000, header.getInt(4))
        assertEquals(77, header.getInt(8))
    }

    @Test
    fun `the receiver can open the audio with the key, the header and the nonce`() {
        val packet = ChaChaPcmPacketEncoder(key).encode(5, 1000, 77, isFirstPacket = false, pcmChunk = pcm)

        val sealedAudio = packet.copyOfRange(12, packet.size - 8)
        val nonce = ByteArray(4) + packet.copyOfRange(packet.size - 8, packet.size)
        val opened = openWithChaCha20Poly1305(key, nonce, sealedAudio, additionalData = packet.copyOfRange(4, 12))

        assertArrayEquals(toBigEndianSamples(pcm), opened)
    }

    @Test
    fun `uses the next nonce for every packet`() {
        val encoder = ChaChaPcmPacketEncoder(key)

        val first = encoder.encode(1, 0, 1, true, pcm)
        val second = encoder.encode(2, 352, 1, false, pcm)

        assertArrayEquals(nonceFromCounter(0).copyOfRange(4, 12), first.copyOfRange(first.size - 8, first.size))
        assertArrayEquals(nonceFromCounter(1).copyOfRange(4, 12), second.copyOfRange(second.size - 8, second.size))
    }

    @Test
    fun `the same audio sounds different in each packet`() {
        val encoder = ChaChaPcmPacketEncoder(key)

        val first = encoder.encode(1, 0, 1, true, pcm).copyOfRange(12, 40)
        val second = encoder.encode(1, 0, 1, true, pcm).copyOfRange(12, 40)

        assertEquals(false, first.contentEquals(second))
    }
}
