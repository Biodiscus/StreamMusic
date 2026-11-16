package net.biodiscus.streammusic.core.streaming.device.airplay

import java.nio.ByteBuffer

private const val RTP_VERSION_2 = 0x80
private const val FIRST_PACKET_FLAG = 0x80
private const val FIRST_SYNC_FLAG = 0x10

private const val AUDIO_PACKET_TYPE = 0x60
private const val SYNC_PACKET_TYPE = 0xD4
private const val TIMING_RESPONSE_TYPE = 0xD3
private const val RETRANSMISSION_TYPE = 0xD6
private const val LOST_PACKETS_REQUEST_TYPE = 0x55

/** The sync packet always carries sequence number 7. */
private const val SYNC_SEQUENCE_NUMBER = 7
private const val RETRANSMISSION_SEQUENCE_NUMBER = 1

private const val AUDIO_HEADER_SIZE = 12
private const val SYNC_PACKET_SIZE = 20
private const val TIMING_PACKET_SIZE = 32
private const val LOST_PACKETS_REQUEST_SIZE = 8
private const val TIMING_SEND_TIME_OFFSET = 24

internal data class LostPacketsRequest(val firstSequenceNumber: Int, val packetCount: Int)

internal fun buildAudioPacket(
    sequenceNumber: Int,
    timestamp: Long,
    sourceId: Int,
    isFirstPacket: Boolean,
    payload: ByteArray,
): ByteArray {
    val packetType = if (isFirstPacket) AUDIO_PACKET_TYPE or FIRST_PACKET_FLAG else AUDIO_PACKET_TYPE
    return ByteBuffer.allocate(AUDIO_HEADER_SIZE + payload.size)
        .put(RTP_VERSION_2.toByte())
        .put(packetType.toByte())
        .putShort(sequenceNumber.toShort())
        .putInt(timestamp.toInt())
        .putInt(sourceId)
        .put(payload)
        .array()
}

/**
 * Tells the receiver which audio sample plays at which moment, so several receivers can stay in step.
 * [playingTimestamp] is the sample that plays at [ntpTime], and the receiver plays [latencyFrames] later.
 */
internal fun buildSyncPacket(isFirstSync: Boolean, playingTimestamp: Long, latencyFrames: Int, ntpTime: Long): ByteArray {
    val firstByte = if (isFirstSync) RTP_VERSION_2 or FIRST_SYNC_FLAG else RTP_VERSION_2
    return ByteBuffer.allocate(SYNC_PACKET_SIZE)
        .put(firstByte.toByte())
        .put(SYNC_PACKET_TYPE.toByte())
        .putShort(SYNC_SEQUENCE_NUMBER.toShort())
        .putInt((playingTimestamp - latencyFrames).toInt())
        .putLong(ntpTime)
        .putInt(playingTimestamp.toInt())
        .array()
}

/** The receiver asks for the time to measure the delay to the phone. The answer repeats the question's send time. */
internal fun answerTimingRequest(request: ByteArray, receivedAtNtpTime: Long): ByteArray? {
    if (request.size != TIMING_PACKET_SIZE) return null
    val requestSendTime = ByteBuffer.wrap(request).getLong(TIMING_SEND_TIME_OFFSET)
    return ByteBuffer.allocate(TIMING_PACKET_SIZE)
        .put(request[0])
        .put(TIMING_RESPONSE_TYPE.toByte())
        .put(request[2])
        .put(request[3])
        .putInt(0)
        .putLong(requestSendTime)
        .putLong(receivedAtNtpTime)
        .putLong(receivedAtNtpTime)
        .array()
}

internal fun parseLostPacketsRequest(request: ByteArray): LostPacketsRequest? {
    if (request.size != LOST_PACKETS_REQUEST_SIZE) return null
    val isLostPacketsRequest = (request[1].toInt() and 0x7F) == LOST_PACKETS_REQUEST_TYPE
    if (!isLostPacketsRequest) return null
    val buffer = ByteBuffer.wrap(request)
    return LostPacketsRequest(
        firstSequenceNumber = buffer.getShort(4).toInt() and 0xFFFF,
        packetCount = buffer.getShort(6).toInt() and 0xFFFF,
    )
}

/** A packet the receiver missed, sent again behind a short header that says it is a repeat. */
internal fun buildRetransmission(originalPacket: ByteArray): ByteArray =
    ByteBuffer.allocate(4 + originalPacket.size)
        .put(RTP_VERSION_2.toByte())
        .put(RETRANSMISSION_TYPE.toByte())
        .putShort(RETRANSMISSION_SEQUENCE_NUMBER.toShort())
        .put(originalPacket)
        .array()
