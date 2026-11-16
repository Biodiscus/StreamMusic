package net.biodiscus.streammusic.core.streaming.device.airplay

import java.nio.ByteBuffer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RtpPacketsTest {

    @Test
    fun `an audio packet has a header followed by the payload`() {
        val packet = buildAudioPacket(0x0102, 0x03040506, 0x0708090A, isFirstPacket = false, payload = byteArrayOf(11, 12))

        val expected = intArrayOf(0x80, 0x60, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 11, 12)
        assertArrayEquals(expected.map { it.toByte() }.toByteArray(), packet)
    }

    @Test
    fun `the first audio packet carries the marker bit`() {
        val packet = buildAudioPacket(1, 0, 0, isFirstPacket = true, payload = ByteArray(0))

        assertEquals(0xE0, packet[1].toInt() and 0xFF)
    }

    @Test
    fun `an audio packet keeps only the low 32 bits of the timestamp`() {
        val packet = buildAudioPacket(1, 0x1_0000_0005L, 0, isFirstPacket = false, payload = ByteArray(0))

        assertEquals(5, ByteBuffer.wrap(packet).getInt(4))
    }

    @Test
    fun `a sync packet names the playing sample and the sample the receiver plays later`() {
        val sync = buildSyncPacket(isFirstSync = false, playingTimestamp = 50_000, latencyFrames = 11_025, ntpTime = 0x1122334455667788L)

        val buffer = ByteBuffer.wrap(sync)
        assertEquals(20, sync.size)
        assertEquals(0x80D40007.toInt(), buffer.getInt(0))
        assertEquals(50_000 - 11_025, buffer.getInt(4))
        assertEquals(0x1122334455667788L, buffer.getLong(8))
        assertEquals(50_000, buffer.getInt(16))
    }

    @Test
    fun `the first sync packet carries the marker bit`() {
        val sync = buildSyncPacket(isFirstSync = true, playingTimestamp = 0, latencyFrames = 0, ntpTime = 0)

        assertEquals(0x90, sync[0].toInt() and 0xFF)
    }

    @Test
    fun `a timing answer repeats the send time of the request and adds the time of arrival`() {
        val request = timingRequest(sendTime = 0x0A0B0C0D0E0F1011L)

        val answer = ByteBuffer.wrap(answerTimingRequest(request, receivedAtNtpTime = 0x7000000000000001L)!!)

        assertEquals(32, answer.capacity())
        assertEquals(0x80D30007.toInt(), answer.getInt(0))
        assertEquals(0x0A0B0C0D0E0F1011L, answer.getLong(8))
        assertEquals(0x7000000000000001L, answer.getLong(16))
        assertEquals(0x7000000000000001L, answer.getLong(24))
    }

    @Test
    fun `a timing request of the wrong size is ignored`() {
        assertNull(answerTimingRequest(ByteArray(12), receivedAtNtpTime = 0))
    }

    @Test
    fun `reads which packets the receiver lost`() {
        val request = byteArrayOf(0x80.toByte(), 0xD5.toByte(), 0, 1, 0x01, 0x00, 0x00, 0x03)

        assertEquals(LostPacketsRequest(firstSequenceNumber = 256, packetCount = 3), parseLostPacketsRequest(request))
    }

    @Test
    fun `a request that is not about lost packets is ignored`() {
        val request = byteArrayOf(0x80.toByte(), 0xD2.toByte(), 0, 1, 0, 0, 0, 3)

        assertNull(parseLostPacketsRequest(request))
    }

    @Test
    fun `a retransmission puts a repeat header before the original packet`() {
        val original = byteArrayOf(1, 2, 3)

        val retransmission = buildRetransmission(original)

        assertArrayEquals(byteArrayOf(0x80.toByte(), 0xD6.toByte(), 0, 1, 1, 2, 3), retransmission)
    }

    private fun timingRequest(sendTime: Long): ByteArray =
        ByteBuffer.allocate(32)
            .putInt(0, 0x80D20007.toInt())
            .putLong(24, sendTime)
            .array()
}
