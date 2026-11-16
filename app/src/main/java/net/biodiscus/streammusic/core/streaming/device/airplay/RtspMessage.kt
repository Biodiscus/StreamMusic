package net.biodiscus.streammusic.core.streaming.device.airplay

import java.io.IOException
import java.io.InputStream
import java.util.TreeMap

private const val LINE_BREAK = "\r\n"
private const val SUCCESS_STATUS_CODE = 200
private const val RTSP_PROTOCOL = "RTSP/1.0"

/** AirPlay 2 pairs over plain HTTP/1.1 and then continues with RTSP on the same connection. */
internal const val HTTP_PROTOCOL = "HTTP/1.1"

internal data class RtspRequest(
    val method: String,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val body: ByteArray? = null,
    val protocol: String = RTSP_PROTOCOL,
)

internal class RtspResponse(
    val statusCode: Int,
    val reason: String,
    headers: Map<String, String>,
    val body: ByteArray = ByteArray(0),
) {
    /** Header names are not case sensitive in RTSP. */
    private val headersByName = caseInsensitiveMapOf(headers)

    val isSuccess: Boolean = statusCode == SUCCESS_STATUS_CODE

    fun header(name: String): String? = headersByName[name]
}

/** [userAgent] is only used when the request does not bring its own. HTTP requests have no sequence number. */
internal fun encodeRtspRequest(request: RtspRequest, sequenceNumber: Int, userAgent: String): ByteArray {
    val headers = caseInsensitiveMapOf(request.headers)
    val body = request.body ?: ByteArray(0)
    val head = buildString {
        append("${request.method} ${request.url} ${request.protocol}$LINE_BREAK")
        headers.forEach { (name, value) -> append("$name: $value$LINE_BREAK") }
        if (request.body != null) append("Content-Length: ${body.size}$LINE_BREAK")
        if (request.protocol == RTSP_PROTOCOL) append("CSeq: $sequenceNumber$LINE_BREAK")
        if ("User-Agent" !in headers) append("User-Agent: $userAgent$LINE_BREAK")
        append(LINE_BREAK)
    }
    return head.toByteArray() + body
}

/** Reads exactly one response and leaves the stream at the start of the next one. */
internal fun readRtspResponse(input: InputStream): RtspResponse {
    val statusLine = readLine(input)
    val statusParts = statusLine.split(" ", limit = 3)
    val statusCode = statusParts.getOrNull(1)?.toIntOrNull() ?: throw IOException("Not an RTSP response: $statusLine")
    val headers = readHeaders(input)
    val body = readBody(input, headers)
    return RtspResponse(statusCode, statusParts.getOrElse(2) { "" }, headers, body)
}

/** Reads exactly one request, as the receiver sends them on the AirPlay 2 event channel. */
internal fun readRtspRequest(input: InputStream): RtspRequest {
    val requestLine = readLine(input)
    val parts = requestLine.split(" ")
    if (parts.size != 3) throw IOException("Not an RTSP request: $requestLine")
    val headers = readHeaders(input)
    val body = readBody(input, headers)
    return RtspRequest(method = parts[0], url = parts[1], headers = headers, body = body, protocol = parts[2])
}

private fun readHeaders(input: InputStream): Map<String, String> {
    val headers = mutableMapOf<String, String>()
    while (true) {
        val line = readLine(input)
        if (line.isEmpty()) return headers
        val name = line.substringBefore(":")
        headers[name.trim()] = line.substringAfter(":", missingDelimiterValue = "").trim()
    }
}

private fun readBody(input: InputStream, headers: Map<String, String>): ByteArray {
    val contentLength = caseInsensitiveMapOf(headers)["Content-Length"]?.trim()?.toIntOrNull() ?: 0
    return ByteArray(contentLength) { readByte(input).toByte() }
}

private fun readLine(input: InputStream): String {
    val line = StringBuilder()
    while (true) {
        val character = readByte(input).toChar()
        if (character == '\n') return line.toString().trimEnd('\r')
        line.append(character)
    }
}

private fun readByte(input: InputStream): Int {
    val value = input.read()
    if (value == -1) throw IOException("The device closed the connection")
    return value
}

private fun caseInsensitiveMapOf(headers: Map<String, String>): MutableMap<String, String> =
    TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER).apply { putAll(headers) }

/** The receiver sends requests on the event channel and only needs to hear that they arrived. */
internal fun encodeEventResponse(request: RtspRequest): ByteArray {
    val headers = caseInsensitiveMapOf(request.headers)
    val response = buildString {
        append("${request.protocol} 200 OK$LINE_BREAK")
        append("Content-Length: 0$LINE_BREAK")
        append("Audio-Latency: 0$LINE_BREAK")
        headers["Server"]?.let { server -> append("Server: $server$LINE_BREAK") }
        headers["CSeq"]?.let { sequenceNumber -> append("CSeq: $sequenceNumber$LINE_BREAK") }
        append(LINE_BREAK)
    }
    return response.toByteArray()
}
