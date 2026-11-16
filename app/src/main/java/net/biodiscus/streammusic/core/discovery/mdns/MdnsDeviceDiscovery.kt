package net.biodiscus.streammusic.core.discovery.mdns

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.Inet4Address
import javax.inject.Inject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import net.biodiscus.streammusic.core.discovery.DeviceDiscovery
import net.biodiscus.streammusic.core.model.DeviceProtocol
import net.biodiscus.streammusic.core.model.StreamingDevice

private const val LOG_TAG = "MdnsDeviceDiscovery"
private const val RESOLVE_TIMEOUT_MILLIS = 5_000L

private class FoundService(
    val info: NsdServiceInfo,
    val protocol: DeviceProtocol,
)

/** Finds Chromecast, AirPlay, AriaCast, Fire TV and similar devices with mDNS (Bonjour). */
internal class MdnsDeviceDiscovery @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : DeviceDiscovery {

    override fun discover(): Flow<StreamingDevice> = channelFlow {
        val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        val foundServices = Channel<FoundService>(Channel.UNLIMITED)
        val listeners = MDNS_SERVICE_KINDS.map { kind -> startBrowsing(nsdManager, kind, foundServices) }

        launch { resolveOneAtATime(nsdManager, foundServices, devices = this@channelFlow) }

        awaitClose {
            listeners.forEach { listener -> nsdManager.stopServiceDiscovery(listener) }
            foundServices.close()
        }
    }

    private fun startBrowsing(
        nsdManager: NsdManager,
        kind: MdnsServiceKind,
        foundServices: SendChannel<FoundService>,
    ): NsdManager.DiscoveryListener {
        val listener = ServiceBrowseListener(kind) { info ->
            foundServices.trySend(FoundService(info, kind.protocol))
        }
        nsdManager.discoverServices(kind.serviceType, NsdManager.PROTOCOL_DNS_SD, listener)
        return listener
    }

    /** NsdManager can only resolve one service at a time, so found services wait in a queue. */
    private suspend fun resolveOneAtATime(
        nsdManager: NsdManager,
        foundServices: Channel<FoundService>,
        devices: SendChannel<StreamingDevice>,
    ) {
        for (service in foundServices) {
            val resolved = resolveWithTimeout(nsdManager, service.info) ?: continue
            val device = toStreamingDevice(resolved, service.protocol)
            if (device != null) devices.send(device)
        }
    }

    private suspend fun resolveWithTimeout(nsdManager: NsdManager, info: NsdServiceInfo): NsdServiceInfo? =
        withTimeoutOrNull(RESOLVE_TIMEOUT_MILLIS) { resolve(nsdManager, info) }

    private suspend fun resolve(nsdManager: NsdManager, info: NsdServiceInfo): NsdServiceInfo? {
        val callback = FirstResolveCallback()
        nsdManager.registerServiceInfoCallback(info, context.mainExecutor, callback)
        try {
            return callback.resolved.await()
        } finally {
            if (callback.isRegistered) nsdManager.unregisterServiceInfoCallback(callback)
        }
    }

    private fun toStreamingDevice(resolved: NsdServiceInfo, protocol: DeviceProtocol): StreamingDevice? {
        val address = resolved.host as? Inet4Address ?: return null
        val textRecords = resolved.attributes.mapValues { (_, bytes) -> String(bytes ?: ByteArray(0)) }
        return StreamingDevice(
            name = friendlyMdnsName(protocol, resolved.serviceName, textRecords),
            protocol = protocol,
            host = address.hostAddress.orEmpty(),
            port = resolved.port,
        )
    }
}

/** Keeps only the first answer: the callback is called again whenever the service changes. */
private class FirstResolveCallback : NsdManager.ServiceInfoCallback {
    val resolved = CompletableDeferred<NsdServiceInfo?>()
    var isRegistered = true
        private set

    override fun onServiceUpdated(serviceInfo: NsdServiceInfo) {
        resolved.complete(serviceInfo)
    }

    override fun onServiceInfoCallbackRegistrationFailed(errorCode: Int) {
        Log.w(LOG_TAG, "Could not resolve a service: error $errorCode")
        isRegistered = false
        resolved.complete(null)
    }

    override fun onServiceLost() {
        resolved.complete(null)
    }

    override fun onServiceInfoCallbackUnregistered() = Unit
}

private class ServiceBrowseListener(
    private val kind: MdnsServiceKind,
    private val onServiceFound: (NsdServiceInfo) -> Unit,
) : NsdManager.DiscoveryListener {

    override fun onServiceFound(serviceInfo: NsdServiceInfo) = onServiceFound.invoke(serviceInfo)

    override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
        Log.w(LOG_TAG, "Could not browse ${kind.serviceType}: error $errorCode")
    }

    override fun onServiceLost(serviceInfo: NsdServiceInfo) = Unit
    override fun onDiscoveryStarted(serviceType: String) = Unit
    override fun onDiscoveryStopped(serviceType: String) = Unit
    override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
}
