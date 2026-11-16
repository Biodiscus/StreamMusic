package net.biodiscus.streammusic.core.streaming.device.ariacast

import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString.Companion.toByteString

private const val NORMAL_CLOSE_CODE = 1000

internal interface AriaCastConnection {
    /** Returns false when the frame could not be queued, which means the connection is no longer usable. */
    fun sendAudioFrame(frame: ByteArray): Boolean

    /** Suspends until the connection ends, and returns the error if it ended because of one. */
    suspend fun awaitClosed(): Throwable?

    fun close()
}

internal fun interface AriaCastConnector {
    suspend fun connect(host: String, port: Int): AriaCastConnection
}

internal fun audioWebSocketUrl(host: String, port: Int): String = "ws://$host:$port/audio"

internal class WebSocketAriaCastConnector @Inject constructor(
    private val httpClient: OkHttpClient,
) : AriaCastConnector {

    override suspend fun connect(host: String, port: Int): AriaCastConnection =
        suspendCancellableCoroutine { continuation ->
            val closed = CompletableDeferred<Throwable?>()
            val request = Request.Builder().url(audioWebSocketUrl(host, port)).build()
            val webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    continuation.resume(WebSocketAriaCastConnection(webSocket, closed))
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    webSocket.close(NORMAL_CLOSE_CODE, null)
                    closed.complete(null)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    closed.complete(null)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    closed.complete(t)
                    if (continuation.isActive) continuation.resumeWithException(t)
                }
            })
            continuation.invokeOnCancellation { webSocket.cancel() }
        }
}

private class WebSocketAriaCastConnection(
    private val webSocket: WebSocket,
    private val closed: CompletableDeferred<Throwable?>,
) : AriaCastConnection {

    override fun sendAudioFrame(frame: ByteArray): Boolean = webSocket.send(frame.toByteString())

    override suspend fun awaitClosed(): Throwable? = closed.await()

    override fun close() {
        webSocket.close(NORMAL_CLOSE_CODE, null)
    }
}
