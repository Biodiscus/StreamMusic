package net.biodiscus.streammusic.core.streaming.device.upnp

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

private const val TIMEOUT_MILLIS = 5_000
private const val OK_STATUS_CODE = 200
private const val MAX_LOGGED_ANSWER_CHARACTERS = 500

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
                if (statusCode != OK_STATUS_CODE) {
                    Timber.w("%s at %s was rejected: HTTP %d, the device said: %s", action, controlUrl, statusCode, readErrorAnswer(connection))
                    throw IOException("$action was rejected: HTTP $statusCode")
                }
                Timber.d("%s answered HTTP %d", action, statusCode)
            } finally {
                connection.disconnect()
            }
        }
    }

    /** A UPnP device explains a refusal in the body of the answer, as a SOAP fault with an error code. */
    private fun readErrorAnswer(connection: HttpURLConnection): String {
        val errorStream = connection.errorStream ?: return ""
        return try {
            errorStream.bufferedReader().use { reader -> reader.readText() }.take(MAX_LOGGED_ANSWER_CHARACTERS)
        } catch (error: IOException) {
            ""
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
