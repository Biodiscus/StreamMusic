package net.biodiscus.streammusic.core.streaming.device.cast

import android.annotation.SuppressLint
import java.io.DataInputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.inject.Inject
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509TrustManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val CONNECT_TIMEOUT_MILLIS = 5_000

internal class TlsCastChannelOpener @Inject constructor() : CastChannelOpener {

    override suspend fun open(host: String, port: Int): CastChannel =
        withContext(Dispatchers.IO) {
            val socket = createTrustingSocketFactory().createSocket() as SSLSocket
            socket.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MILLIS)
            socket.startHandshake()
            TlsCastChannel(socket)
        }

    private fun createTrustingSocketFactory() =
        SSLContext.getInstance("TLS")
            .apply { init(null, arrayOf(TrustEveryCertificate), SecureRandom()) }
            .socketFactory
}

private class TlsCastChannel(private val socket: SSLSocket) : CastChannel {

    private val input = DataInputStream(socket.inputStream.buffered())
    private val output: OutputStream = socket.outputStream.buffered()

    override suspend fun send(message: CastMessage) {
        withContext(Dispatchers.IO) {
            synchronized(output) {
                output.write(encodeCastFrame(message))
                output.flush()
            }
        }
    }

    override suspend fun receive(): CastMessage = withContext(Dispatchers.IO) { readCastMessage(input) }

    override fun close() = socket.close()
}

/**
 * Chromecasts present a certificate signed by Google's private device authority, which Android does not
 * trust, so the certificate cannot be checked. This is only used for the media controls on the local
 * network, and the audio itself is not sent over this connection.
 */
@SuppressLint("CustomX509TrustManager", "TrustAllX509TrustManager")
private object TrustEveryCertificate : X509TrustManager {
    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit
    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
}
