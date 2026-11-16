package net.biodiscus.streammusic.core.streaming.device.airplay

import java.io.IOException
import java.util.Base64

private const val RTP_PAYLOAD_TYPE = 96
private const val BITS_PER_SAMPLE = 16
private const val CHANNEL_COUNT = 2

/** What the phone tells the receiver about the encryption of the audio. */
internal class RaopEncryptionOffer(val encryptedKey: ByteArray, val initializationVector: ByteArray)

/** The UDP ports of the receiver. The timing port is missing when the receiver sends its time requests unasked. */
internal data class RaopServerPorts(val audioPort: Int, val controlPort: Int, val timingPort: Int?)

internal fun buildSessionDescription(
    sessionId: String,
    phoneAddress: String,
    deviceAddress: String,
    encryption: RaopEncryptionOffer?,
): String {
    val lines = mutableListOf(
        "v=0",
        "o=iTunes $sessionId 0 IN IP4 $phoneAddress",
        "s=iTunes",
        "c=IN IP4 $deviceAddress",
        "t=0 0",
        "m=audio 0 RTP/AVP $RTP_PAYLOAD_TYPE",
        "a=rtpmap:$RTP_PAYLOAD_TYPE AppleLossless",
        "a=fmtp:$RTP_PAYLOAD_TYPE $RAOP_FRAMES_PER_PACKET 0 $BITS_PER_SAMPLE 40 10 14 $CHANNEL_COUNT 255 0 0 $RAOP_SAMPLE_RATE_HZ",
    )
    if (encryption != null) {
        lines += "a=rsaaeskey:${base64WithoutPadding(encryption.encryptedKey)}"
        lines += "a=aesiv:${base64WithoutPadding(encryption.initializationVector)}"
    }
    return lines.joinToString(separator = "\r\n", postfix = "\r\n")
}

internal fun buildTransportRequestHeader(controlPort: Int, timingPort: Int): String =
    "RTP/AVP/UDP;unicast;interleaved=0-1;mode=record;control_port=$controlPort;timing_port=$timingPort"

/** Reads the ports from a `Transport` header such as `RTP/AVP/UDP;unicast;server_port=6000;control_port=6001`. */
internal fun parseServerPorts(transportHeader: String): RaopServerPorts {
    val portsByName = transportHeader.split(";")
        .filter { part -> part.contains("=") }
        .associate { part -> part.substringBefore("=").trim() to part.substringAfter("=").trim().toIntOrNull() }
    val audioPort = portsByName["server_port"] ?: throw IOException("The device sent no audio port")
    val controlPort = portsByName["control_port"] ?: throw IOException("The device sent no control port")
    return RaopServerPorts(audioPort, controlPort, portsByName["timing_port"])
}

private fun base64WithoutPadding(bytes: ByteArray): String = Base64.getEncoder().withoutPadding().encodeToString(bytes)
