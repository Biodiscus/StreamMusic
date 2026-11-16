package net.biodiscus.streammusic.core.streaming.device.airplay2

import net.biodiscus.streammusic.core.streaming.device.airplay.RtspRequest
import net.biodiscus.streammusic.core.streaming.device.airplay.RtspResponse
import net.biodiscus.streammusic.core.streaming.device.airplay.bytes

/** The salt and public key of a real SRP server, so that the phone can answer its challenge. */
private val SERVER_SALT = bytes("1254ab587b061d8d")
private val SERVER_PUBLIC_KEY = bytes(
        "dd544725ca76dce53256aba77a57d56ac71134355b3a2515aa80358a7bd6bebfe7688ccdfdb446a01056d5b285440ad80195" +
        "0b9362f9cb22758993f54eec1fdecb9b8d6a49e50095885ca7fd8b49dd3951299d4f7e40d654bd387a2630597389191a7e82" +
        "7e08cf2664859e3848bef379a4a6aa22747abf624c5317565159634e3b4ba7d769ddc26ce5a5897afba154a01ab10da2d1c7" +
        "2d9a222cf1cc457236787b8e227bb68a3dde4ee18dc194ea907642d036652061314223b1f49e1c5963941a3ae99d2680429f" +
        "ea417873a203b80360ff6c423bdfc1e2d0573ec9a81f8cdbbc8a997833ed387001bafce19267910d37b44d3d86c336a8f30b" +
        "6eca7caef14045373369b03aa1b381934b720daae73bd22425e8dad699db6ffbc8c6ccbea912b2f33bf88a97d0c0ac28113b" +
        "bc6e6af13a2be90193c1edaf067e4d30e290edd0d2e50200c56034835399f9e2f03a239343338811ef37fffb5acbb844c6e6" +
        "cfed871a29c0a7ae34e862ef0493ca6ffeedfd6a8346d734c709c88ba06bedb5d080",
)

internal const val EVENT_PORT = 51_234
internal const val AUDIO_PORT = 6_000
internal const val CONTROL_PORT = 6_001

/** A receiver that accepts pairing with any answer and sets up a stream. It remembers what it was sent. */
internal class FakeAirPlay2Receiver {
    val requests = mutableListOf<RtspRequest>()
    private var pairSetupMessages = 0

    val methodsAndUrls: List<String>
        get() = requests.map { request -> "${request.method} ${request.url.substringBefore("rtsp://")}".trim() }

    fun answer(request: RtspRequest): RtspResponse {
        requests.add(request)
        return when {
            request.url == "/pair-setup" -> answerPairSetup()
            request.method == "SETUP" && isSessionSetup(request) -> plistAnswer(linkedMapOf("eventPort" to EVENT_PORT, "timingPort" to 0))
            request.method == "SETUP" -> plistAnswer(linkedMapOf("streams" to listOf(linkedMapOf("dataPort" to AUDIO_PORT, "controlPort" to CONTROL_PORT))))
            else -> RtspResponse(200, "OK", emptyMap())
        }
    }

    private fun answerPairSetup(): RtspResponse {
        pairSetupMessages++
        val body = if (pairSetupMessages == 1) firstStepAnswer() else thirdStepAnswer()
        return RtspResponse(200, "OK", emptyMap(), body)
    }

    private fun firstStepAnswer(): ByteArray =
        encodeTlv8(listOf(TlvTag.SEQUENCE_NUMBER to byteArrayOf(2), TlvTag.PUBLIC_KEY to SERVER_PUBLIC_KEY, TlvTag.SALT to SERVER_SALT))

    private fun thirdStepAnswer(): ByteArray =
        encodeTlv8(listOf(TlvTag.SEQUENCE_NUMBER to byteArrayOf(4), TlvTag.PROOF to ByteArray(64)))

    private fun isSessionSetup(request: RtspRequest): Boolean =
        (decodeBinaryPlist(request.body ?: ByteArray(0)) as Map<*, *>).containsKey("timingProtocol")

    private fun plistAnswer(content: Map<String, Any>) = RtspResponse(200, "OK", emptyMap(), encodeBinaryPlist(content))
}
