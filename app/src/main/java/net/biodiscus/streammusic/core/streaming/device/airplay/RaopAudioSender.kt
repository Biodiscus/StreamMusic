package net.biodiscus.streammusic.core.streaming.device.airplay

private const val SEQUENCE_NUMBER_MASK = 0xFFFF
private const val PACKETS_KEPT_FOR_RETRANSMISSION = 512

internal const val RAOP_FIRST_SEQUENCE_NUMBER = 1

/**
 * Turns captured audio into numbered and timestamped packets and sends them.
 * The timeline starts at [startTimestamp] and every packet is [RAOP_FRAMES_PER_PACKET] later than the one before.
 */
internal class RaopAudioSender(
    private val connection: RaopConnection,
    private val packetEncoder: AudioPacketEncoder,
    private val sourceId: Int,
    private val latencyFrames: Int,
    startTimestamp: Long,
) {
    private val chunker = RaopAudioChunker()
    private val backlog = RaopPacketBacklog(PACKETS_KEPT_FOR_RETRANSMISSION)
    private var nextSequenceNumber = RAOP_FIRST_SEQUENCE_NUMBER
    @Volatile
    private var isFirstPacket = true

    @Volatile
    private var nextTimestamp = startTimestamp

    fun sendCapturedAudio(capturedPcm: ByteArray) {
        chunker.add(capturedPcm).forEach { chunk -> sendChunk(chunk) }
    }

    /** Receivers use this to keep their clock in step with ours. Nothing is sent before the first audio. */
    fun sendSyncPacket() {
        if (isFirstPacket) return
        connection.sendControlPacket(buildSyncPacketFor(nextTimestamp, isFirstSync = false))
    }

    fun retransmissionsFor(request: ByteArray): List<ByteArray> {
        val lostPackets = parseLostPacketsRequest(request) ?: return emptyList()
        return (0 until lostPackets.packetCount)
            .map { offset -> (lostPackets.firstSequenceNumber + offset) and SEQUENCE_NUMBER_MASK }
            .mapNotNull { sequenceNumber -> backlog.find(sequenceNumber) }
            .map { packet -> buildRetransmission(packet) }
    }

    private fun sendChunk(chunk: ByteArray) {
        val timestamp = nextTimestamp
        if (isFirstPacket) connection.sendControlPacket(buildSyncPacketFor(timestamp, isFirstSync = true))
        val packet = packetEncoder.encode(nextSequenceNumber, timestamp, sourceId, isFirstPacket, chunk)
        backlog.remember(nextSequenceNumber, packet)
        connection.sendAudioPacket(packet)
        isFirstPacket = false
        nextSequenceNumber = (nextSequenceNumber + 1) and SEQUENCE_NUMBER_MASK
        nextTimestamp = timestamp + RAOP_FRAMES_PER_PACKET
    }

    private fun buildSyncPacketFor(timestamp: Long, isFirstSync: Boolean): ByteArray =
        buildSyncPacket(isFirstSync, timestamp, latencyFrames, ntpTime = ntpTimeOfSample(timestamp, RAOP_SAMPLE_RATE_HZ))
}

/** The most recent packets, kept so the receiver can ask for the ones that got lost on the way. */
internal class RaopPacketBacklog(private val capacity: Int) {
    private class BackloggedPacket(val sequenceNumber: Int, val packet: ByteArray)

    private val slots = arrayOfNulls<BackloggedPacket>(capacity)

    @Synchronized
    fun remember(sequenceNumber: Int, packet: ByteArray) {
        slots[sequenceNumber % capacity] = BackloggedPacket(sequenceNumber, packet)
    }

    @Synchronized
    fun find(sequenceNumber: Int): ByteArray? {
        val backlogged = slots[sequenceNumber % capacity] ?: return null
        return if (backlogged.sequenceNumber == sequenceNumber) backlogged.packet else null
    }
}
