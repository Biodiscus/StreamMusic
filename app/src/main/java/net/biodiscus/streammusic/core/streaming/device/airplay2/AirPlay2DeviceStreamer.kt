package net.biodiscus.streammusic.core.streaming.device.airplay2

import java.io.IOException
import java.util.UUID
import kotlin.random.Random
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.biodiscus.streammusic.core.model.StreamingDevice
import net.biodiscus.streammusic.core.streaming.device.DeviceStreamer
import net.biodiscus.streammusic.core.streaming.device.SessionAudio
import net.biodiscus.streammusic.core.streaming.device.airplay.RAOP_SAMPLE_RATE_HZ
import net.biodiscus.streammusic.core.streaming.device.airplay.RaopAudioSender
import net.biodiscus.streammusic.core.streaming.device.airplay.RaopConnection
import net.biodiscus.streammusic.core.streaming.device.airplay.RaopConnector
import net.biodiscus.streammusic.core.streaming.device.airplay.RaopServerPorts
import net.biodiscus.streammusic.core.streaming.device.airplay.RAOP_FIRST_SEQUENCE_NUMBER
import net.biodiscus.streammusic.core.streaming.device.airplay.RtspRequest
import net.biodiscus.streammusic.core.streaming.device.airplay.RtspResponse
import net.biodiscus.streammusic.core.streaming.device.airplay.answerTimingRequest
import net.biodiscus.streammusic.core.streaming.device.airplay.ntpTimeFromEpochMillis
import net.biodiscus.streammusic.core.streaming.device.airplay.sampleNumberAt
import net.biodiscus.streammusic.core.streaming.device.airplay.streamAudioForever

private const val FEEDBACK_INTERVAL_MILLIS = 2_000L
private const val UNAUTHORIZED_STATUS_CODE = 401
private const val BINARY_PLIST_CONTENT_TYPE = "application/x-apple-binary-plist"
private const val RTSP_USER_AGENT = "AirPlay/550.10"

private const val CONTROL_SALT = "Control-Salt"
private const val CONTROL_WRITE_INFO = "Control-Write-Encryption-Key"
private const val CONTROL_READ_INFO = "Control-Read-Encryption-Key"
private const val EVENTS_SALT = "Events-Salt"
private const val EVENTS_WRITE_INFO = "Events-Write-Encryption-Key"
private const val EVENTS_READ_INFO = "Events-Read-Encryption-Key"

/**
 * Streams to an AirPlay 2 receiver: pairs with the shared pin, sets up an encrypted real-time audio stream,
 * and sends it PCM packets protected with ChaCha20-Poly1305.
 */
internal class AirPlay2DeviceStreamer(
    private val device: StreamingDevice,
    private val audio: SessionAudio,
    private val connector: RaopConnector,
    private val epochMillisNow: () -> Long = System::currentTimeMillis,
) : DeviceStreamer {

    override suspend fun stream(onStreaming: () -> Unit): Nothing {
        val connection = connector.open(device.host, device.port)
        val conversation = AirPlay2Conversation(connection)
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
        conversation: AirPlay2Conversation,
        onStreaming: () -> Unit,
    ): Nothing = coroutineScope {
        launch { connection.answerTimingRequests { request -> answerTimingRequest(request, nowNtpTime()) } }

        val sharedKey = conversation.setUp()
        launch { connection.answerEventRequests() }

        val startTimestamp = sampleNumberAt(nowNtpTime(), RAOP_SAMPLE_RATE_HZ)
        conversation.startRecording(startTimestamp)
        val sender = RaopAudioSender(
            connection,
            ChaChaPcmPacketEncoder(sharedKey),
            conversation.sourceId,
            AIRPLAY2_LATENCY_FRAMES,
            startTimestamp,
        )
        streamAudioForever(connection, sender, audio, FEEDBACK_INTERVAL_MILLIS, conversation::sendFeedback, onStreaming)
    }

    private fun nowNtpTime(): Long = ntpTimeFromEpochMillis(epochMillisNow())
}

/** The requests of one AirPlay 2 session, in the order the receiver expects them. */
private class AirPlay2Conversation(private val connection: RaopConnection) {
    private val sessionId = Random.nextLong(0, 1L shl 32)
    private val sessionUrl = "rtsp://${connection.phoneAddress}/$sessionId"
    private val clientId = Random.nextLong().toULong().toString(16).uppercase()
    private val activeRemote = Random.nextLong(0, 1L shl 32).toString()
    private var isEncrypted = false

    /** The number that marks our audio packets and names our stream. */
    val sourceId: Int = sessionId.toInt()

    /** Pairs and sets up the audio stream. Returns the key that encrypts the audio. */
    suspend fun setUp(): ByteArray {
        askForInfo()
        val pairingSecret = TransientPairing(connection).pair()
        enableEncryption(pairingSecret)
        val eventPort = setUpSession()
        openEventChannel(pairingSecret, eventPort)
        val sharedKey = deriveKey(EVENTS_SALT, EVENTS_WRITE_INFO, pairingSecret)
        setUpAudioStream(sharedKey)
        return sharedKey
    }

    /** Asks the receiver to play what arrives, from [startTimestamp] on. Everything older is thrown away. */
    suspend fun startRecording(startTimestamp: Long) {
        send(RtspRequest("RECORD", sessionUrl, rtspHeaders()))
        val rtpInfo = "seq=$RAOP_FIRST_SEQUENCE_NUMBER;rtptime=${startTimestamp.toInt().toUInt()}"
        send(RtspRequest("FLUSH", sessionUrl, rtspHeaders("Range" to "npt=0-", "Session" to "0", "RTP-Info" to rtpInfo)))
    }

    /** The receiver closes the connection when it does not hear from the phone. */
    suspend fun sendFeedback() {
        connection.sendRtsp(RtspRequest("POST", "/feedback", rtspHeaders()))
    }

    suspend fun endIgnoringErrors() {
        if (!isEncrypted) return
        try {
            send(RtspRequest("TEARDOWN", sessionUrl, rtspHeaders("Session" to "0")))
        } catch (error: IOException) {
            // The receiver is unreachable, so it ends the session by itself.
        }
    }

    /** Receivers that do not know /info answer with an error, which is fine. */
    private suspend fun askForInfo() {
        connection.sendRtsp(RtspRequest("GET", "/info", rtspHeaders()))
    }

    private fun enableEncryption(pairingSecret: ByteArray) {
        connection.enableRtspEncryption(
            sendKey = deriveKey(CONTROL_SALT, CONTROL_WRITE_INFO, pairingSecret),
            receiveKey = deriveKey(CONTROL_SALT, CONTROL_READ_INFO, pairingSecret),
        )
        isEncrypted = true
    }

    private suspend fun setUpSession(): Int {
        val body = buildSessionSetupBody(UUID.randomUUID().toString().uppercase(), connection.timingPort)
        val response = send(RtspRequest("SETUP", sessionUrl, plistHeaders(), body))
        return parseEventPort(response.body)
    }

    /** The receiver reads what the phone sends with its "read" key, so the keys are the other way around. */
    private suspend fun openEventChannel(pairingSecret: ByteArray, eventPort: Int) {
        connection.openEventChannel(
            port = eventPort,
            sendKey = deriveKey(EVENTS_SALT, EVENTS_READ_INFO, pairingSecret),
            receiveKey = deriveKey(EVENTS_SALT, EVENTS_WRITE_INFO, pairingSecret),
        )
    }

    private suspend fun setUpAudioStream(sharedKey: ByteArray) {
        val body = buildStreamSetupBody(connection.controlPort, sharedKey, sessionId)
        val response = send(RtspRequest("SETUP", sessionUrl, plistHeaders(), body))
        val ports = parseStreamPorts(response.body)
        connection.aimUdpSocketsAt(RaopServerPorts(ports.dataPort, ports.controlPort, timingPort = null))
    }

    private suspend fun send(request: RtspRequest): RtspResponse {
        val response = connection.sendRtsp(request)
        if (!response.isSuccess) throw IOException(describeRefusal(request.method, response))
        return response
    }

    private fun describeRefusal(method: String, response: RtspResponse): String {
        if (response.statusCode == UNAUTHORIZED_STATUS_CODE) return "The device wants a password"
        return "The device refused $method: ${response.statusCode} ${response.reason}"
    }

    private fun plistHeaders(): Map<String, String> = rtspHeaders("Content-Type" to BINARY_PLIST_CONTENT_TYPE)

    private fun rtspHeaders(vararg extraHeaders: Pair<String, String>): Map<String, String> =
        mapOf(
            "User-Agent" to RTSP_USER_AGENT,
            "DACP-ID" to clientId,
            "Active-Remote" to activeRemote,
            "Client-Instance" to clientId,
        ) + extraHeaders
}
