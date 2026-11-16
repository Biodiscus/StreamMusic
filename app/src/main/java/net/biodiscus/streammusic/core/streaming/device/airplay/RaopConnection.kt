package net.biodiscus.streammusic.core.streaming.device.airplay

import java.io.Closeable

/** Everything the phone has open towards one AirPlay receiver: the RTSP conversation and the three UDP sockets. */
internal interface RaopConnection : Closeable {
    /** The address of the phone as the receiver sees it. */
    val phoneAddress: String

    /** The UDP ports the receiver is told to send its requests to. */
    val controlPort: Int
    val timingPort: Int

    /** Sends one request and waits for its answer. Throws when the connection fails. */
    suspend fun sendRtsp(request: RtspRequest): RtspResponse

    /** AirPlay 2: from now on everything on the RTSP connection is encrypted and decrypted with these keys. */
    fun enableRtspEncryption(sendKey: ByteArray, receiveKey: ByteArray)

    /** AirPlay 2: connects to the event channel the receiver opened. Its messages are only answered by [answerEventRequests]. */
    suspend fun openEventChannel(port: Int, sendKey: ByteArray, receiveKey: ByteArray)

    /** AirPlay 2: sends a positive answer to every message the receiver sends on the event channel, until cancelled. */
    suspend fun answerEventRequests(): Nothing

    /** Tells the UDP sockets where the receiver listens. Must be called before any packet is sent. */
    fun aimUdpSocketsAt(ports: RaopServerPorts)

    fun sendAudioPacket(packet: ByteArray)

    fun sendControlPacket(packet: ByteArray)

    /** Replies to every time request the receiver sends, until cancelled. A null answer means: ignore the request. */
    suspend fun answerTimingRequests(answerFor: (request: ByteArray) -> ByteArray?): Nothing

    /** Sends back every packet [answerFor] returns for each request on the control socket, until cancelled. */
    suspend fun answerControlRequests(answerFor: (request: ByteArray) -> List<ByteArray>): Nothing
}

internal fun interface RaopConnector {
    suspend fun open(host: String, rtspPort: Int): RaopConnection
}
