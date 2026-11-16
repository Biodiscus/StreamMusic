package net.biodiscus.streammusic.core.streaming.device.upnp

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TIMEOUT_MILLIS = 5_000
private const val OK_STATUS_CODE = 200

/** The three UPnP AVTransport actions needed to play a stream on a renderer. */
internal interface AvTransportClient {
    suspend fun setAvTransportUri(controlUrl: String, mediaUrl: String)
    suspend fun play(controlUrl: String)
    suspend fun stop(controlUrl: String)
}

internal class SoapAvTransportClient @Inject constructor() : AvTransportClient {

    override suspend fun setAvTransportUri(controlUrl: String, mediaUrl: String) {
        val arguments = listOf("CurrentURI" to mediaUrl, "CurrentURIMetaData" to "")
        callAction(controlUrl, "SetAVTransportURI", arguments)
    }

    override suspend fun play(controlUrl: String) {
        callAction(controlUrl, "Play", listOf("Speed" to "1"))
    }

    override suspend fun stop(controlUrl: String) {
        callAction(controlUrl, "Stop", emptyList())
    }

    private suspend fun callAction(controlUrl: String, action: String, arguments: List<Pair<String, String>>) {
        withContext(Dispatchers.IO) {
            val connection = URL(controlUrl).openConnection() as HttpURLConnection
            try {
                postSoapRequest(connection, action, arguments)
                val statusCode = connection.responseCode
                if (statusCode != OK_STATUS_CODE) throw IOException("$action was rejected: HTTP $statusCode")
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun postSoapRequest(connection: HttpURLConnection, action: String, arguments: List<Pair<String, String>>) {
        connection.requestMethod = "POST"
        connection.connectTimeout = TIMEOUT_MILLIS
        connection.readTimeout = TIMEOUT_MILLIS
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"")
        connection.setRequestProperty("SOAPACTION", soapActionHeader(action))
        connection.outputStream.use { it.write(buildSoapRequestBody(action, arguments).toByteArray()) }
    }
}
