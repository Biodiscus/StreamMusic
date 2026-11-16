package net.biodiscus.streammusic.core.streaming.device.airplay

import java.io.IOException
import kotlin.random.Random
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.biodiscus.streammusic.core.model.StreamingDevice
import net.biodiscus.streammusic.core.streaming.device.DeviceStreamer
import net.biodiscus.streammusic.core.streaming.device.SessionAudio

private const val DEFAULT_LATENCY_FRAMES = 11_025
private const val KEEP_ALIVE_INTERVAL_MILLIS = 25_000L
private const val UNAUTHORIZED_STATUS_CODE = 401

/** Streams to an AirPlay 1 receiver (RAOP): RTSP to set the session up, then encrypted ALAC over RTP. */
internal class RaopDeviceStreamer(
    private val device: StreamingDevice,
    private val audio: SessionAudio,
    private val connector: RaopConnector,
    private val epochMillisNow: () -> Long = System::currentTimeMillis,
) : DeviceStreamer {

    override suspend fun stream(onStreaming: () -> Unit): Nothing {
        val connection = connector.open(device.host, device.port)
        val conversation = RaopConversation(connection, device.host, audioKeyForDevice())
        try {
            streamOver(connection, conversation, onStreaming)
        } finally {
            withContext(NonCancellable) {
                conversation.endIgnoringErrors()
                connection.close()
            }
        }
    }

    private suspend fun streamOver(
        connection: RaopConnection,
        conversation: RaopConversation,
        onStreaming: () -> Unit,
    ): Nothing = coroutineScope {
        launch { connection.answerTimingRequests { request -> answerTimingRequest(request, nowNtpTime()) } }

        val startTimestamp = sampleNumberAt(nowNtpTime(), RAOP_SAMPLE_RATE_HZ)
        val latencyFrames = conversation.start(startTimestamp)
        val packetEncoder = AlacAudioPacketEncoder(conversation.audioKey)
        val sender = RaopAudioSender(connection, packetEncoder, Random.nextInt(), latencyFrames, startTimestamp)
        streamAudioForever(connection, sender, audio, KEEP_ALIVE_INTERVAL_MILLIS, conversation::keepAlive, onStreaming)
    }

    private fun audioKeyForDevice(): RaopAudioKey? =
        if (device.raop?.shouldEncryptAudio == true) RaopAudioKey.random() else null

    private fun nowNtpTime(): Long = ntpTimeFromEpochMillis(epochMillisNow())
}

/** The RTSP requests of one session, in the order the receiver expects them. */
private class RaopConversation(
    private val connection: RaopConnection,
    private val deviceHost: String,
    val audioKey: RaopAudioKey?,
) {
    private val sessionId = Random.nextLong(1_000_000_000L, 10_000_000_000L).toString()
    private val sessionUrl = "rtsp://${connection.phoneAddress}/$sessionId"
    private var rtspSession: String? = null

    /** Returns how many frames the receiver buffers before it plays. */
    suspend fun start(startTimestamp: Long): Int {
        announce()
        val ports = setUp()
        connection.aimUdpSocketsAt(ports)
        return record(startTimestamp)
    }

    /** Some receivers answer an OPTIONS request with "not implemented". That is fine, only silence is a problem. */
    suspend fun keepAlive() {
        connection.sendRtsp(requestInSession(RtspRequest(method = "OPTIONS", url = "*")))
    }

    suspend fun endIgnoringErrors() {
        if (rtspSession == null) return
        try {
            send(RtspRequest(method = "TEARDOWN", url = sessionUrl))
        } catch (error: IOException) {
            // The receiver is unreachable, so it ends the session by itself.
        }
    }

    private suspend fun announce() {
        val body = buildSessionDescription(sessionId, connection.phoneAddress, deviceHost, encryptionOffer())
        send(RtspRequest("ANNOUNCE", sessionUrl, mapOf("Content-Type" to "application/sdp"), body.toByteArray()))
    }

    private fun encryptionOffer(): RaopEncryptionOffer? {
        val key = audioKey ?: return null
        return RaopEncryptionOffer(encryptKeyForReceiver(key), key.initializationVector)
    }

    private suspend fun setUp(): RaopServerPorts {
        val transport = buildTransportRequestHeader(connection.controlPort, connection.timingPort)
        val response = send(RtspRequest("SETUP", sessionUrl, mapOf("Transport" to transport)))
        rtspSession = response.header("Session")?.substringBefore(";")?.trim()
        val transportHeader = response.header("Transport") ?: throw IOException("The device sent no transport")
        return parseServerPorts(transportHeader)
    }

    private suspend fun record(startTimestamp: Long): Int {
        val startRtpTime = startTimestamp.toInt().toUInt()
        val headers = mapOf("Range" to "npt=0-", "RTP-Info" to "seq=$RAOP_FIRST_SEQUENCE_NUMBER;rtptime=$startRtpTime")
        val response = send(RtspRequest("RECORD", sessionUrl, headers))
        val announcedLatency = response.header("Audio-Latency")?.trim()?.toIntOrNull() ?: 0
        return maxOf(DEFAULT_LATENCY_FRAMES, announcedLatency)
    }

    private suspend fun send(request: RtspRequest): RtspResponse {
        val response = connection.sendRtsp(requestInSession(request))
        if (!response.isSuccess) throw IOException(describeRefusal(request.method, response))
        return response
    }

    private fun requestInSession(request: RtspRequest): RtspRequest {
        val session = rtspSession ?: return request
        return request.copy(headers = request.headers + ("Session" to session))
    }

    private fun describeRefusal(method: String, response: RtspResponse): String {
        if (response.statusCode == UNAUTHORIZED_STATUS_CODE) return "The device wants a password"
        return "The device refused $method: ${response.statusCode} ${response.reason}"
    }
}
