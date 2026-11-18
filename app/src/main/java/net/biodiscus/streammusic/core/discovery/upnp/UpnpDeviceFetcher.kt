package net.biodiscus.streammusic.core.discovery.upnp

import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.biodiscus.streammusic.core.discovery.isIpv4Address
import net.biodiscus.streammusic.core.model.StreamingDevice
import timber.log.Timber

private const val TIMEOUT_MILLIS = 3_000

/** Downloads a UPnP device description and turns it into a [StreamingDevice] if it can play audio. */
internal class UpnpDeviceFetcher @Inject constructor() {

    /** Returns null when the device is not an IPv4 renderer (no AVTransport service) or cannot be read. */
    suspend fun fetchRenderer(locationUrl: String, searchTarget: String): StreamingDevice? =
        withContext(Dispatchers.IO) {
            val xml = downloadDescription(locationUrl) ?: return@withContext null
            val description = parseUpnpDescription(xml, locationUrl)
            toStreamingDevice(locationUrl, searchTarget, description)
        }

    private fun downloadDescription(locationUrl: String): String? =
        try {
            val connection = URL(locationUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = TIMEOUT_MILLIS
            connection.readTimeout = TIMEOUT_MILLIS
            connection.inputStream.bufferedReader().use { it.readText() }
        } catch (error: java.io.IOException) {
            Timber.d(error, "Could not download the device description from %s", locationUrl)
            null
        }

    private fun toStreamingDevice(
        locationUrl: String,
        searchTarget: String,
        description: UpnpDescription,
    ): StreamingDevice? {
        val controlUrl = description.avTransportControlUrl
        if (controlUrl == null) {
            Timber.d("Skipping %s: it has no AVTransport service, so it cannot play a stream", locationUrl)
            return null
        }
        val location = URL(locationUrl)
        if (!isIpv4Address(location.host)) {
            Timber.d("Skipping %s: its address is not IPv4", locationUrl)
            return null
        }
        val protocol = classifyUpnpDevice(searchTarget, description)
        Timber.d("%s is a %s (search target %s)", locationUrl, protocol, searchTarget)
        return StreamingDevice(
            name = description.displayName(fallback = location.host),
            protocol = protocol,
            host = location.host,
            port = location.port,
            avTransportControlUrl = controlUrl,
        )
    }
}
