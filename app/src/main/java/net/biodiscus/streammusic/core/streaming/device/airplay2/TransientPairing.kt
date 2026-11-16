package net.biodiscus.streammusic.core.streaming.device.airplay2

import java.io.IOException
import net.biodiscus.streammusic.core.streaming.device.airplay.HTTP_PROTOCOL
import net.biodiscus.streammusic.core.streaming.device.airplay.RaopConnection
import net.biodiscus.streammusic.core.streaming.device.airplay.RtspRequest

/** Receivers that do not ask the user for a code accept this pin. It is the same for every receiver. */
private const val TRANSIENT_PIN = "3939"
private const val FIRST_STEP = 1
private const val THIRD_STEP = 3
private const val PAIR_SETUP_METHOD = 0
private const val TRANSIENT_PAIRING_FLAG = 0x10

private val PAIRING_HEADERS = mapOf(
    "User-Agent" to "AirPlay/320.20",
    "Connection" to "keep-alive",
    "X-Apple-HKP" to "4",
    "Content-Type" to "application/octet-stream",
)

/**
 * The first four steps of HomeKit pairing. It leaves nothing stored: the receiver and the phone only share a secret
 * for this session, from which the keys of the encrypted connection and of the audio are made.
 */
internal class TransientPairing(private val connection: RaopConnection) {

    /** Returns the secret that both sides now share. */
    suspend fun pair(): ByteArray {
        askReceiverToStartPairing()
        val challenge = decodeTlv8(post("/pair-setup", firstStepMessage()))
        val srp = HomeKitSrpClient(TRANSIENT_PIN)
        val answer = srp.answerChallenge(
            serverPublicKey = challenge.requireValue(TlvTag.PUBLIC_KEY, "public key"),
            salt = challenge.requireValue(TlvTag.SALT, "salt"),
        )
        val confirmation = decodeTlv8(post("/pair-setup", thirdStepMessage(srp.publicKey, answer.proof)))
        failOnPairingError(confirmation)
        return answer.sessionKey
    }

    /** Receivers that show a pin on their screen show it now. For transient pairing it does not matter. */
    private suspend fun askReceiverToStartPairing() {
        connection.sendRtsp(RtspRequest("POST", "/pair-pin-start", PAIRING_HEADERS, protocol = HTTP_PROTOCOL))
    }

    private suspend fun post(path: String, body: ByteArray): ByteArray {
        val response = connection.sendRtsp(RtspRequest("POST", path, PAIRING_HEADERS, body, HTTP_PROTOCOL))
        if (!response.isSuccess) throw IOException("The device refused to pair: ${response.statusCode} ${response.reason}")
        return response.body
    }

    private fun firstStepMessage(): ByteArray =
        encodeTlv8(
            listOf(
                TlvTag.METHOD to byteArrayOf(PAIR_SETUP_METHOD.toByte()),
                TlvTag.SEQUENCE_NUMBER to byteArrayOf(FIRST_STEP.toByte()),
                TlvTag.FLAGS to byteArrayOf(TRANSIENT_PAIRING_FLAG.toByte()),
            ),
        )

    private fun thirdStepMessage(publicKey: ByteArray, proof: ByteArray): ByteArray =
        encodeTlv8(
            listOf(
                TlvTag.SEQUENCE_NUMBER to byteArrayOf(THIRD_STEP.toByte()),
                TlvTag.PUBLIC_KEY to publicKey,
                TlvTag.PROOF to proof,
            ),
        )

    private fun Map<Int, ByteArray>.requireValue(tag: Int, name: String): ByteArray {
        failOnPairingError(this)
        return this[tag] ?: throw IOException("The device sent no $name while pairing")
    }

    private fun failOnPairingError(message: Map<Int, ByteArray>) {
        val errorCode = message[TlvTag.ERROR]?.firstOrNull()?.toInt() ?: return
        throw IOException("The device refused to pair (error $errorCode)")
    }
}
