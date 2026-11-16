package net.biodiscus.streammusic.core.streaming

import java.net.DatagramSocket
import java.net.InetAddress

/**
 * The address of this phone on the network that can reach [deviceHost].
 * Connecting a UDP socket only picks a route, it sends no packets.
 */
internal fun localAddressFacing(deviceHost: String): String {
    DatagramSocket().use { socket ->
        socket.connect(InetAddress.getByName(deviceHost), 9)
        return socket.localAddress.hostAddress.orEmpty()
    }
}
