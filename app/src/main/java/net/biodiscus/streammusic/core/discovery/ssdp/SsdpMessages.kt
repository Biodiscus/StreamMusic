package net.biodiscus.streammusic.core.discovery.ssdp

internal const val SSDP_MULTICAST_ADDRESS = "239.255.255.250"
internal const val SSDP_PORT = 1900

/** What we ask the network for. Each target answers for one family of devices. */
internal val SSDP_SEARCH_TARGETS = listOf(
    "urn:schemas-upnp-org:device:MediaRenderer:1",
    "urn:schemas-upnp-org:device:ZonePlayer:1",
    "urn:schemas-denon-com:device:ACT-Denon:1",
)

internal data class SsdpResponse(
    val location: String,
    val searchTarget: String,
)

internal fun buildSsdpSearchRequest(searchTarget: String): String =
    "M-SEARCH * HTTP/1.1\r\n" +
        "HOST: $SSDP_MULTICAST_ADDRESS:$SSDP_PORT\r\n" +
        "MAN: \"ssdp:discover\"\r\n" +
        "MX: 2\r\n" +
        "ST: $searchTarget\r\n" +
        "\r\n"

internal fun parseSsdpResponse(message: String): SsdpResponse? {
    val lines = message.lines()
    if (!isSuccessStatusLine(lines.first())) return null

    val headers = readHeaders(lines.drop(1))
    val location = headers["location"] ?: return null
    return SsdpResponse(
        location = location,
        searchTarget = headers["st"].orEmpty(),
    )
}

private fun isSuccessStatusLine(statusLine: String): Boolean =
    statusLine.startsWith("HTTP/1.1 200")

private fun readHeaders(headerLines: List<String>): Map<String, String> =
    headerLines
        .filter { line -> line.contains(":") }
        .associate { line ->
            val name = line.substringBefore(":").trim().lowercase()
            val value = line.substringAfter(":").trim()
            name to value
        }
