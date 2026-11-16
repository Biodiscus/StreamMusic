package net.biodiscus.streammusic.core.discovery

private val IPV4_PATTERN = Regex("""^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})$""")
private const val HIGHEST_OCTET_VALUE = 255

internal fun isIpv4Address(host: String): Boolean {
    val match = IPV4_PATTERN.matchEntire(host) ?: return false
    val octets = match.groupValues.drop(1).map { it.toInt() }
    return octets.all { octet -> octet <= HIGHEST_OCTET_VALUE }
}
