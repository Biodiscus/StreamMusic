package net.biodiscus.streammusic.core.streaming.aac

import java.io.BufferedReader
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.util.Collections
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.launch

private const val STREAM_PATH = "/stream.aac"
private const val REQUEST_READ_TIMEOUT_MILLIS = 5_000
private const val AAC_FRAMES_KEPT_PER_LISTENER = 100

/** Encodes PCM to AAC once and serves it to every device that connects, like an internet radio station. */
internal class AacHttpStreamStarter @Inject constructor() : AacStreamStarter {
    override fun start(scope: CoroutineScope, pcmFrames: Flow<ByteArray>): RunningAacStream {
        val aacFrames = AacEncoder()
            .encode(pcmFrames)
            .flowOn(Dispatchers.Default)
            .buffer(AAC_FRAMES_KEPT_PER_LISTENER, BufferOverflow.DROP_OLDEST)
            .shareIn(scope, SharingStarted.Eagerly)
        val stream = AacHttpStream(scope, aacFrames)
        stream.startAcceptingListeners()
        return stream
    }
}

private class AacHttpStream(
    private val scope: CoroutineScope,
    private val aacFrames: Flow<ByteArray>,
) : RunningAacStream {

    private val serverSocket = ServerSocket(0)
    private val listenerSockets: MutableSet<Socket> = Collections.synchronizedSet(mutableSetOf())

    override val port: Int = serverSocket.localPort

    fun startAcceptingListeners() {
        scope.launch(Dispatchers.IO) {
            while (!serverSocket.isClosed) {
                val socket = acceptOrNull() ?: break
                launch { serveListener(socket) }
            }
        }
    }

    override fun stop() {
        serverSocket.close()
        synchronized(listenerSockets) { listenerSockets.forEach { it.close() } }
    }

    private fun acceptOrNull(): Socket? =
        try {
            serverSocket.accept()
        } catch (error: IOException) {
            null
        }

    private suspend fun serveListener(socket: Socket) {
        listenerSockets.add(socket)
        try {
            socket.use { sendStream(it) }
        } catch (error: IOException) {
            // The device disconnected or the stream was stopped.
        } finally {
            listenerSockets.remove(socket)
        }
    }

    private suspend fun sendStream(socket: Socket) {
        val requestLine = readRequestLine(socket)
        val output = socket.getOutputStream()
        output.write(buildStreamResponseHeaders().toByteArray())
        output.flush()
        if (isHeadRequest(requestLine)) return
        aacFrames.collect { frame ->
            output.write(frame)
            output.flush()
        }
    }

    private fun readRequestLine(socket: Socket): String {
        socket.soTimeout = REQUEST_READ_TIMEOUT_MILLIS
        val reader = socket.getInputStream().bufferedReader()
        val requestLine = reader.readLine().orEmpty()
        skipRemainingHeaders(reader)
        socket.soTimeout = 0
        return requestLine
    }

    private fun skipRemainingHeaders(reader: BufferedReader) {
        while (true) {
            val line = reader.readLine()
            if (line.isNullOrEmpty()) return
        }
    }
}

internal fun buildStreamResponseHeaders(): String =
    "HTTP/1.0 200 OK\r\n" +
        "Content-Type: audio/aac\r\n" +
        "Cache-Control: no-cache, no-store\r\n" +
        "Connection: close\r\n" +
        "icy-name: StreamMusic\r\n" +
        "\r\n"

internal fun isHeadRequest(requestLine: String): Boolean = requestLine.startsWith("HEAD ")

internal fun streamUrl(host: String, port: Int): String = "http://$host:$port$STREAM_PATH"
