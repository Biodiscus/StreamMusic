package net.biodiscus.streammusic.core.discovery.ssdp

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.withContext
import net.biodiscus.streammusic.core.discovery.DeviceDiscovery
import net.biodiscus.streammusic.core.discovery.upnp.UpnpDeviceFetcher
import net.biodiscus.streammusic.core.model.StreamingDevice

private const val LOG_TAG = "SsdpDeviceDiscovery"
private const val SEARCH_REPEAT_DELAY_MILLIS = 10_000L
private const val MAX_RESPONSE_BYTES = 2_048

/** Finds UPnP/DLNA renderers, Sonos and HEOS speakers with SSDP multicast searches. */
internal class SsdpDeviceDiscovery @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val deviceFetcher: UpnpDeviceFetcher,
) : DeviceDiscovery {

    override fun discover(): Flow<StreamingDevice> = channelFlow {
        val multicastLock = acquireMulticastLock()
        val socket = DatagramSocket()

        launch { sendSearchesRepeatedly(socket) }
        launch { receiveResponses(socket, devices = this@channelFlow) }

        // Closing the socket is what unblocks DatagramSocket.receive when the flow is cancelled.
        awaitClose {
            socket.close()
            multicastLock.release()
        }
    }

    private fun acquireMulticastLock(): WifiManager.MulticastLock {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val lock = wifiManager.createMulticastLock("streammusic-ssdp")
        lock.setReferenceCounted(false)
        lock.acquire()
        return lock
    }

    private suspend fun sendSearchesRepeatedly(socket: DatagramSocket) {
        withContext(Dispatchers.IO) {
            while (isActive && !socket.isClosed) {
                sendAllSearches(socket)
                delay(SEARCH_REPEAT_DELAY_MILLIS)
            }
        }
    }

    private fun sendAllSearches(socket: DatagramSocket) {
        try {
            SSDP_SEARCH_TARGETS.forEach { target -> sendSearch(socket, target) }
        } catch (error: IOException) {
            Log.w(LOG_TAG, "Could not send SSDP search", error)
        }
    }

    private fun sendSearch(socket: DatagramSocket, searchTarget: String) {
        val bytes = buildSsdpSearchRequest(searchTarget).toByteArray()
        val multicastAddress = InetAddress.getByName(SSDP_MULTICAST_ADDRESS)
        socket.send(DatagramPacket(bytes, bytes.size, multicastAddress, SSDP_PORT))
    }

    private suspend fun receiveResponses(socket: DatagramSocket, devices: SendChannel<StreamingDevice>) {
        withContext(Dispatchers.IO) {
            val alreadyFetchedLocations = mutableSetOf<String>()
            coroutineScope {
                while (isActive && !socket.isClosed) {
                    val response = receiveOneResponse(socket) ?: continue
                    val isNewLocation = alreadyFetchedLocations.add(response.location)
                    if (isNewLocation) launch { fetchAndSend(response, devices) }
                }
            }
        }
    }

    private fun receiveOneResponse(socket: DatagramSocket): SsdpResponse? {
        val buffer = ByteArray(MAX_RESPONSE_BYTES)
        val packet = DatagramPacket(buffer, buffer.size)
        try {
            socket.receive(packet)
        } catch (error: SocketException) {
            if (socket.isClosed) return null
            throw error
        }
        return parseSsdpResponse(String(packet.data, 0, packet.length))
    }

    private suspend fun fetchAndSend(response: SsdpResponse, devices: SendChannel<StreamingDevice>) {
        val device = deviceFetcher.fetchRenderer(response.location, response.searchTarget)
        if (device != null) devices.send(device)
    }
}
