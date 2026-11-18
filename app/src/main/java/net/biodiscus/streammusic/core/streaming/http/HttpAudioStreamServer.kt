package net.biodiscus.streammusic.core.streaming.http

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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.launch
import net.biodiscus.streammusic.core.model.StreamAudioFormat
import net.biodiscus.streammusic.core.streaming.aac.AacEncoder
import timber.log.Timber

private const val REQUEST_READ_TIMEOUT_MILLIS = 5_000
private const val AUDIO_FRAMES_KEPT_PER_LISTENER = 100

/** Encodes PCM to the format once and serves it to every device that connects, like an internet radio station. */
internal class HttpAudioStreamServerStarter @Inject constructor() : HttpAudioStreamStarter {
    override fun start(scope: CoroutineScope, pcmFrames: Flow<ByteArray>, format: StreamAudioFormat): RunningHttpAudioStream {
        val audioFrames = encode(pcmFrames, format)
            .buffer(AUDIO_FRAMES_KEPT_PER_LISTENER, BufferOverflow.DROP_OLDEST)
            .shareIn(scope, SharingStarted.Eagerly)
        val stream = HttpAudioStream(scope, audioFrames, format)
        stream.startAcceptingListeners()
        Timber.i("Serving the %s stream on port %d", format, stream.port)
        return stream
    }

    private fun encode(pcmFrames: Flow<ByteArray>, format: StreamAudioFormat): Flow<ByteArray> =
        when (format) {
            StreamAudioFormat.AAC -> AacEncoder().encode(pcmFrames).flowOn(Dispatchers.Default)
            StreamAudioFormat.LPCM -> pcmFrames.map { pcmFrame -> toBigEndianSamples(pcmFrame) }
            StreamAudioFormat.WAV -> pcmFrames
        }
}

private class HttpAudioStream(
    private val scope: CoroutineScope,
    private val audioFrames: Flow<ByteArray>,
    override val format: StreamAudioFormat,
) : RunningHttpAudioStream {

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
            Timber.d(error, "A listener stopped listening")
            // The device disconnected or the stream was stopped.
        } finally {
            listenerSockets.remove(socket)
        }
    }

    private suspend fun sendStream(socket: Socket) {
        val requestLine = readRequestLine(socket)
        Timber.i("%s asked for the stream: %s", socket.inetAddress.hostAddress, requestLine)
        val output = socket.getOutputStream()
        output.write(buildStreamResponseHeaders(format).toByteArray())
        output.flush()
        if (isHeadRequest(requestLine)) return
        output.write(streamPreambleOf(format))
        audioFrames.collect { frame ->
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

internal fun buildStreamResponseHeaders(format: StreamAudioFormat): String =
    "HTTP/1.0 200 OK\r\n" +
        "Content-Type: ${httpContentTypeOf(format)}\r\n" +
        "Cache-Control: no-cache, no-store\r\n" +
        "Connection: close\r\n" +
        "icy-name: StreamMusic\r\n" +
        "\r\n"

internal fun isHeadRequest(requestLine: String): Boolean = requestLine.startsWith("HEAD ")

internal fun streamUrl(host: String, port: Int, format: StreamAudioFormat): String =
    "http://$host:$port/stream.${urlExtensionOf(format)}"
