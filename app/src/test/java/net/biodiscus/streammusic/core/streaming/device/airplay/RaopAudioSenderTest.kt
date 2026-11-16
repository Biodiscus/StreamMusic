package net.biodiscus.streammusic.core.streaming.device.airplay

import java.nio.ByteBuffer
import net.biodiscus.streammusic.core.streaming.PcmFormat
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RaopAudioSenderTest {

    private val connection = FakeRaopConnection()
    private val firstTimestamp = sampleNumberAt(ntpTimeFromEpochMillis(1_763_000_000_000), RAOP_SAMPLE_RATE_HZ)

    @Test
    fun `sends nothing until audio is captured`() {
        senderWithoutEncryption().sendSyncPacket()

        assertTrue(connection.audioPackets.isEmpty())
        assertTrue(connection.controlPackets.isEmpty())
    }

    @Test
    fun `marks only the first packet as the first`() {
        senderWithoutEncryption().sendCapturedAudio(capturedFrame())

        assertEquals(listOf(0xE0, 0x60), connection.audioPackets.map { packet -> packet[1].toInt() and 0xFF })
    }

    @Test
    fun `numbers the packets one after the other`() {
        senderWithoutEncryption().sendCapturedAudio(capturedFrame())

        assertEquals(listOf(1, 2), connection.audioPackets.map { packet -> sequenceNumberOf(packet) })
    }

    @Test
    fun `starts the timeline at the current time and moves it by one packet each time`() {
        senderWithoutEncryption().sendCapturedAudio(capturedFrame())

        val timestamps = connection.audioPackets.map { packet -> ByteBuffer.wrap(packet).getInt(4) }
        assertEquals(listOf(firstTimestamp.toInt(), firstTimestamp.toInt() + RAOP_FRAMES_PER_PACKET), timestamps)
    }

    @Test
    fun `sends the first sync before the first audio packet`() {
        senderWithoutEncryption().sendCapturedAudio(capturedFrame())

        val firstSync = connection.controlPackets.single()
        assertEquals(0x90, firstSync[0].toInt() and 0xFF)
        assertEquals(firstTimestamp.toInt(), ByteBuffer.wrap(firstSync).getInt(16))
    }

    @Test
    fun `later syncs name the timestamp of the next packet`() {
        val sender = senderWithoutEncryption()
        sender.sendCapturedAudio(capturedFrame())

        sender.sendSyncPacket()

        val laterSync = connection.controlPackets.last()
        assertEquals(0x80, laterSync[0].toInt() and 0xFF)
        assertEquals(firstTimestamp.toInt() + 2 * RAOP_FRAMES_PER_PACKET, ByteBuffer.wrap(laterSync).getInt(16))
    }

    @Test
    fun `the sync tells the latency of the receiver`() {
        val sender = senderWithoutEncryption(latencyFrames = 20_000)
        sender.sendCapturedAudio(capturedFrame())

        val firstSync = ByteBuffer.wrap(connection.controlPackets.single())
        assertEquals(firstSync.getInt(16) - 20_000, firstSync.getInt(4))
    }

    @Test
    fun `sends a lost packet again behind a repeat header`() {
        val sender = senderWithoutEncryption()
        sender.sendCapturedAudio(capturedFrame())

        val retransmissions = sender.retransmissionsFor(lostPacketsRequest(firstSequenceNumber = 2, packetCount = 1))

        assertArrayEquals(buildRetransmission(connection.audioPackets[1]), retransmissions.single())
    }

    @Test
    fun `skips lost packets it no longer has`() {
        val sender = senderWithoutEncryption()
        sender.sendCapturedAudio(capturedFrame())

        val retransmissions = sender.retransmissionsFor(lostPacketsRequest(firstSequenceNumber = 1, packetCount = 5))

        assertEquals(2, retransmissions.size)
    }

    @Test
    fun `ignores a request that is not about lost packets`() {
        val sender = senderWithoutEncryption()
        sender.sendCapturedAudio(capturedFrame())

        assertTrue(sender.retransmissionsFor(ByteArray(3)).isEmpty())
    }

    @Test
    fun `lets the encoder build the packets`() {
        val encoder = AudioPacketEncoder { sequenceNumber, timestamp, sourceId, isFirstPacket, pcmChunk ->
            byteArrayOf(sequenceNumber.toByte(), sourceId.toByte(), if (isFirstPacket) 1 else 0, (pcmChunk.size / 256).toByte(), timestamp.toByte())
        }

        RaopAudioSender(connection, encoder, sourceId = 9, latencyFrames = 0, startTimestamp = 300).sendCapturedAudio(capturedFrame())

        val packets = connection.audioPackets.map { packet -> packet.map { byte -> byte.toInt() and 0xFF } }
        assertEquals(listOf(listOf(1, 9, 1, 5, 44), listOf(2, 9, 0, 5, 44 + 96)), packets)
    }

    @Test
    fun `marks the packets with the source id`() {
        RaopAudioSender(connection, AlacAudioPacketEncoder(null), sourceId = 0x01020304, latencyFrames = 0, startTimestamp = 0)
            .sendCapturedAudio(capturedFrame())

        assertEquals(0x01020304, ByteBuffer.wrap(connection.audioPackets.first()).getInt(8))
    }

    @Test
    fun `encrypts the audio when the encoder has a key`() {
        val plainConnection = FakeRaopConnection()
        RaopAudioSender(plainConnection, AlacAudioPacketEncoder(null), 1, 0, firstTimestamp).sendCapturedAudio(capturedFrame())
        val key = RaopAudioKey(ByteArray(16) { it.toByte() }, ByteArray(16))

        RaopAudioSender(connection, AlacAudioPacketEncoder(key), 1, 0, firstTimestamp).sendCapturedAudio(capturedFrame())

        val plainPayload = plainConnection.audioPackets.first().copyOfRange(12, 12 + 16)
        val encryptedPayload = connection.audioPackets.first().copyOfRange(12, 12 + 16)
        assertFalse(plainPayload.contentEquals(encryptedPayload))
    }

    @Test
    fun `remembers only the most recent packets`() {
        val backlog = RaopPacketBacklog(capacity = 4)
        (1..6).forEach { number -> backlog.remember(number, byteArrayOf(number.toByte())) }

        assertEquals(null, backlog.find(1))
        assertArrayEquals(byteArrayOf(6), backlog.find(6))
    }

    private fun senderWithoutEncryption(latencyFrames: Int = 11_025) =
        RaopAudioSender(connection, AlacAudioPacketEncoder(null), sourceId = 1, latencyFrames, firstTimestamp)

    private fun capturedFrame() = ByteArray(PcmFormat.BYTES_PER_FRAME)

    private fun sequenceNumberOf(packet: ByteArray): Int = ByteBuffer.wrap(packet).getShort(2).toInt() and 0xFFFF

    private fun lostPacketsRequest(firstSequenceNumber: Int, packetCount: Int): ByteArray =
        ByteBuffer.allocate(8)
            .put(0x80.toByte())
            .put(0xD5.toByte())
            .putShort(1)
            .putShort(firstSequenceNumber.toShort())
            .putShort(packetCount.toShort())
            .array()
}
