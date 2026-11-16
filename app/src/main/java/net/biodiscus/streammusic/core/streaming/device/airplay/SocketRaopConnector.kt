package net.biodiscus.streammusic.core.streaming.device.airplay

import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.ConnectException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val USER_AGENT = "iTunes/11.0.4 (Windows; N)"
private const val CONNECT_TIMEOUT_MILLIS = 5_000
private const val RTSP_ANSWER_TIMEOUT_MILLIS = 10_000

/** A blocked read cannot be cancelled, so reads give up regularly to check whether the streaming was cancelled. */
private const val UDP_READ_TIMEOUT_MILLIS = 500
private const val LARGEST_UDP_PACKET = 2048

/** An AirPlay 2 receiver opens the event port a moment after it names it, so the first attempts can be refused. */
private const val EVENT_CHANNEL_ATTEMPTS = 5
private const val EVENT_CHANNEL_RETRY_DELAY_MILLIS = 1_000L

internal class SocketRaopConnector @Inject constructor() : RaopConnector {

    override suspend fun open(host: String, rtspPort: Int): RaopConnection = withContext(Dispatchers.IO) {
        val udpSockets = RaopUdpSockets()
        try {
            val rtspSocket = Socket()
            rtspSocket.connect(InetSocketAddress(host, rtspPort), CONNECT_TIMEOUT_MILLIS)
            rtspSocket.soTimeout = RTSP_ANSWER_TIMEOUT_MILLIS
            rtspSocket.tcpNoDelay = true
            SocketRaopConnection(InetAddress.getByName(host), rtspSocket, udpSockets)
        } catch (error: Exception) {
            udpSockets.close()
            throw error
        }
    }
}

private class RaopUdpSockets : AutoCloseable {
    val audio = DatagramSocket()
    val control = DatagramSocket().also { socket -> socket.soTimeout = UDP_READ_TIMEOUT_MILLIS }
    val timing = DatagramSocket().also { socket -> socket.soTimeout = UDP_READ_TIMEOUT_MILLIS }

    override fun close() {
        audio.close()
        control.close()
        timing.close()
    }
}

/** The event channel of AirPlay 2: a second TCP connection, encrypted like the first one once pairing is done. */
private class EventChannel(val socket: Socket, val input: InputStream, val output: OutputStream)

private class SocketRaopConnection(
    private val deviceAddress: InetAddress,
    private val rtspSocket: Socket,
    private val udpSockets: RaopUdpSockets,
) : RaopConnection {

    private var rtspInput: InputStream = BufferedInputStream(rtspSocket.getInputStream())
    private var rtspOutput: OutputStream = rtspSocket.getOutputStream()
    private var rtspSequenceNumber = 0

    @Volatile
    private var eventChannel: EventChannel? = null

    @Volatile
    private var deviceAudioAddress: InetSocketAddress? = null

    @Volatile
    private var deviceControlAddress: InetSocketAddress? = null

    override val phoneAddress: String = rtspSocket.localAddress.hostAddress.orEmpty()
    override val controlPort: Int = udpSockets.control.localPort
    override val timingPort: Int = udpSockets.timing.localPort

    override suspend fun sendRtsp(request: RtspRequest): RtspResponse = withContext(Dispatchers.IO) {
        rtspSequenceNumber++
        rtspOutput.write(encodeRtspRequest(request, rtspSequenceNumber, USER_AGENT))
        rtspOutput.flush()
        readRtspResponse(rtspInput)
    }

    override fun enableRtspEncryption(sendKey: ByteArray, receiveKey: ByteArray) {
        rtspInput = BufferedInputStream(HapInputStream(rtspSocket.getInputStream(), receiveKey))
        rtspOutput = HapOutputStream(rtspSocket.getOutputStream(), sendKey)
    }

    override suspend fun openEventChannel(port: Int, sendKey: ByteArray, receiveKey: ByteArray) {
        val socket = withContext(Dispatchers.IO) { connectWithRetries(port) }
        val input = BufferedInputStream(HapInputStream(socket.getInputStream(), receiveKey))
        eventChannel = EventChannel(socket, input, HapOutputStream(socket.getOutputStream(), sendKey))
    }

    override suspend fun answerEventRequests(): Nothing {
        val channel = checkNotNull(eventChannel) { "The event channel is not open" }
        coroutineScope {
            launch(start = CoroutineStart.UNDISPATCHED) { closeWhenCancelled(channel.socket) }
            withContext(Dispatchers.IO) { answerUntilClosed(channel) }
        }
        awaitCancellation()
    }

    override fun aimUdpSocketsAt(ports: RaopServerPorts) {
        deviceAudioAddress = InetSocketAddress(deviceAddress, ports.audioPort)
        deviceControlAddress = InetSocketAddress(deviceAddress, ports.controlPort)
    }

    override fun sendAudioPacket(packet: ByteArray) {
        val address = checkNotNull(deviceAudioAddress) { "The audio port of the device is not known yet" }
        udpSockets.audio.send(DatagramPacket(packet, packet.size, address))
    }

    override fun sendControlPacket(packet: ByteArray) {
        val address = checkNotNull(deviceControlAddress) { "The control port of the device is not known yet" }
        udpSockets.control.send(DatagramPacket(packet, packet.size, address))
    }

    override suspend fun answerTimingRequests(answerFor: (ByteArray) -> ByteArray?): Nothing =
        handleEachPacket(udpSockets.timing) { request ->
            val answer = answerFor(request.contents())
            if (answer != null) udpSockets.timing.send(DatagramPacket(answer, answer.size, request.socketAddress))
        }

    override suspend fun answerControlRequests(answerFor: (ByteArray) -> List<ByteArray>): Nothing =
        handleEachPacket(udpSockets.control) { request ->
            answerFor(request.contents()).forEach { answer -> sendControlPacket(answer) }
        }

    override fun close() {
        udpSockets.close()
        eventChannel?.socket?.close()
        rtspSocket.close()
    }

    private suspend fun connectWithRetries(port: Int): Socket {
        repeat(EVENT_CHANNEL_ATTEMPTS - 1) {
            try {
                return connectTo(port)
            } catch (refused: ConnectException) {
                delay(EVENT_CHANNEL_RETRY_DELAY_MILLIS)
            }
        }
        return connectTo(port)
    }

    private fun connectTo(port: Int): Socket {
        val socket = Socket()
        socket.connect(InetSocketAddress(deviceAddress, port), CONNECT_TIMEOUT_MILLIS)
        socket.tcpNoDelay = true
        return socket
    }

    /** When the coroutine was cancelled, the exception is only the result of closing the socket, so it is not an error. */
    private fun CoroutineScope.answerUntilClosed(channel: EventChannel) {
        try {
            while (true) {
                val request = readRtspRequest(channel.input)
                channel.output.write(encodeEventResponse(request))
                channel.output.flush()
            }
        } catch (error: IOException) {
            ensureActive()
            throw error
        }
    }

    /** A blocked read ends with an exception when its socket is closed, which is the only way to stop it. */
    private suspend fun closeWhenCancelled(socket: Socket): Nothing {
        try {
            awaitCancellation()
        } finally {
            socket.close()
        }
    }

    private suspend fun handleEachPacket(socket: DatagramSocket, handle: (DatagramPacket) -> Unit): Nothing {
        withContext(Dispatchers.IO) {
            while (true) {
                ensureActive()
                val packet = receiveOrNull(socket) ?: continue
                handle(packet)
            }
        }
        awaitCancellation()
    }

    private fun receiveOrNull(socket: DatagramSocket): DatagramPacket? {
        val packet = DatagramPacket(ByteArray(LARGEST_UDP_PACKET), LARGEST_UDP_PACKET)
        return try {
            socket.receive(packet)
            packet
        } catch (noPacketYet: SocketTimeoutException) {
            null
        }
    }

    private fun DatagramPacket.contents(): ByteArray = data.copyOf(length)
}
