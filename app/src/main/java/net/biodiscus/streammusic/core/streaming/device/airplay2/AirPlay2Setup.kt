package net.biodiscus.streammusic.core.streaming.device.airplay2

import java.io.IOException
import net.biodiscus.streammusic.core.streaming.device.airplay.RAOP_FRAMES_PER_PACKET
import net.biodiscus.streammusic.core.streaming.device.airplay.RAOP_SAMPLE_RATE_HZ

/** How many frames the receiver buffers before it plays: one and a half seconds. */
internal const val AIRPLAY2_LATENCY_FRAMES = 66_150

private const val MIN_LATENCY_FRAMES = 11_025
private const val MAX_LATENCY_FRAMES = 88_200
private const val RAW_PCM_CODEC = 1
private const val PCM_44100_16_STEREO_FORMAT = 0x800
private const val REAL_TIME_AUDIO_STREAM_TYPE = 96

internal data class AirPlay2StreamPorts(val dataPort: Int, val controlPort: Int)

/** The first SETUP: who we are and where the receiver can ask us for the time. */
internal fun buildSessionSetupBody(sessionUuid: String, timingPort: Int): ByteArray =
    encodeBinaryPlist(
        linkedMapOf(
            "deviceID" to "AA:BB:CC:DD:EE:FF",
            "sessionUUID" to sessionUuid,
            "timingPort" to timingPort,
            "timingProtocol" to "NTP",
            "isMultiSelectAirPlay" to true,
            "groupContainsGroupLeader" to false,
            "macAddress" to "AA:BB:CC:DD:EE:FF",
            "model" to "iPhone14,3",
            "name" to "StreamMusic",
            "osBuildVersion" to "20F66",
            "osName" to "iPhone OS",
            "osVersion" to "16.5",
            "senderSupportsRelay" to false,
            "sourceVersion" to "690.7.1",
            "statsCollectionEnabled" to false,
        ),
    )

/** The second SETUP: one real-time audio stream of 44.1 kHz, 16-bit stereo PCM, encrypted with [sharedKey]. */
internal fun buildStreamSetupBody(controlPort: Int, sharedKey: ByteArray, streamConnectionId: Long): ByteArray =
    encodeBinaryPlist(
        linkedMapOf(
            "streams" to listOf(
                linkedMapOf(
                    "audioFormat" to PCM_44100_16_STEREO_FORMAT,
                    "audioMode" to "default",
                    "controlPort" to controlPort,
                    "ct" to RAW_PCM_CODEC,
                    "isMedia" to true,
                    "latencyMax" to MAX_LATENCY_FRAMES,
                    "latencyMin" to MIN_LATENCY_FRAMES,
                    "shk" to sharedKey,
                    "spf" to RAOP_FRAMES_PER_PACKET,
                    "sr" to RAOP_SAMPLE_RATE_HZ,
                    "type" to REAL_TIME_AUDIO_STREAM_TYPE,
                    "supportsDynamicStreamID" to false,
                    "streamConnectionID" to streamConnectionId,
                ),
            ),
        ),
    )

/** The port on which the receiver listens for events from the phone's side of the session. */
internal fun parseEventPort(sessionSetupResponse: ByteArray): Int {
    val response = decodeResponseDictionary(sessionSetupResponse)
    return (response["eventPort"] as? Long)?.toInt() ?: throw IOException("The device sent no event port")
}

internal fun parseStreamPorts(streamSetupResponse: ByteArray): AirPlay2StreamPorts {
    val response = decodeResponseDictionary(streamSetupResponse)
    val streams = response["streams"] as? List<*> ?: throw IOException("The device did not set up a stream")
    val stream = streams.firstOrNull() as? Map<*, *> ?: throw IOException("The device did not set up a stream")
    val dataPort = (stream["dataPort"] as? Long)?.toInt() ?: throw IOException("The device sent no audio port")
    val controlPort = (stream["controlPort"] as? Long)?.toInt() ?: throw IOException("The device sent no control port")
    return AirPlay2StreamPorts(dataPort, controlPort)
}

private fun decodeResponseDictionary(body: ByteArray): Map<*, *> =
    decodeBinaryPlist(body) as? Map<*, *> ?: throw IOException("The device sent an unexpected answer")
