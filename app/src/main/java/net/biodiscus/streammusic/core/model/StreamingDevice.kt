package net.biodiscus.streammusic.core.model

/**
 * A speaker, TV or other receiver found on the local network.
 *
 * [host] is always an IPv4 address, because stream URLs and control URLs are built from it.
 */
data class StreamingDevice(
    val name: String,
    val protocol: DeviceProtocol,
    val host: String,
    val port: Int,
    val avTransportControlUrl: String? = null,
) {
    val id: String = "${protocol.name}@$host"

    val canReceiveStream: Boolean = protocol.transport != StreamTransport.NOT_SUPPORTED
}
