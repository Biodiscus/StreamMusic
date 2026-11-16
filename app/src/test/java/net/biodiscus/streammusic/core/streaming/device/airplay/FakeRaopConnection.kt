package net.biodiscus.streammusic.core.streaming.device.airplay

import kotlinx.coroutines.awaitCancellation

/** A receiver that accepts everything and writes down what it was sent. */
internal class FakeRaopConnection : RaopConnection {
    override val phoneAddress = "10.0.0.5"
    override val controlPort = 50_001
    override val timingPort = 50_002

    val rtspRequests = mutableListOf<RtspRequest>()
    val audioPackets = mutableListOf<ByteArray>()
    val controlPackets = mutableListOf<ByteArray>()

    var answerToRtsp: (RtspRequest) -> RtspResponse = ::acceptingAnswer
    var aimedAt: RaopServerPorts? = null
        private set
    var timingAnswerer: ((ByteArray) -> ByteArray?)? = null
        private set
    var controlAnswerer: ((ByteArray) -> List<ByteArray>)? = null
        private set
    var closed = false
        private set

    /** What the phone did on the way to the AirPlay 2 connection, in order, such as "encrypted" or "events:51234". */
    val airPlay2Steps = mutableListOf<String>()
    var rtspSendKey: ByteArray? = null
        private set
    var rtspReceiveKey: ByteArray? = null
        private set
    var eventSendKey: ByteArray? = null
        private set
    var eventReceiveKey: ByteArray? = null
        private set
    var answeredEvents = false
        private set

    val rtspMethods: List<String>
        get() = rtspRequests.map { request -> request.method }

    override suspend fun sendRtsp(request: RtspRequest): RtspResponse {
        rtspRequests.add(request)
        return answerToRtsp(request)
    }

    override fun enableRtspEncryption(sendKey: ByteArray, receiveKey: ByteArray) {
        rtspSendKey = sendKey
        rtspReceiveKey = receiveKey
        airPlay2Steps.add("encrypted")
    }

    override suspend fun openEventChannel(port: Int, sendKey: ByteArray, receiveKey: ByteArray) {
        eventSendKey = sendKey
        eventReceiveKey = receiveKey
        airPlay2Steps.add("events:$port")
    }

    override suspend fun answerEventRequests(): Nothing {
        answeredEvents = true
        awaitCancellation()
    }

    override fun aimUdpSocketsAt(ports: RaopServerPorts) {
        aimedAt = ports
    }

    override fun sendAudioPacket(packet: ByteArray) {
        audioPackets.add(packet)
    }

    override fun sendControlPacket(packet: ByteArray) {
        controlPackets.add(packet)
    }

    override suspend fun answerTimingRequests(answerFor: (ByteArray) -> ByteArray?): Nothing {
        timingAnswerer = answerFor
        awaitCancellation()
    }

    override suspend fun answerControlRequests(answerFor: (ByteArray) -> List<ByteArray>): Nothing {
        controlAnswerer = answerFor
        awaitCancellation()
    }

    override fun close() {
        closed = true
    }
}

internal fun acceptingAnswer(request: RtspRequest): RtspResponse =
    when (request.method) {
        "SETUP" -> RtspResponse(
            200,
            "OK",
            mapOf("Session" to "ABC", "Transport" to "RTP/AVP/UDP;unicast;server_port=6000;control_port=6001;timing_port=6002"),
        )
        "RECORD" -> RtspResponse(200, "OK", mapOf("Audio-Latency" to "11025"))
        else -> RtspResponse(200, "OK", emptyMap())
    }
