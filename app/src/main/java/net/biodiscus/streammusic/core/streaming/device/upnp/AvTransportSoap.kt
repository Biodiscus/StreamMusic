package net.biodiscus.streammusic.core.streaming.device.upnp

private const val AV_TRANSPORT_SERVICE = "urn:schemas-upnp-org:service:AVTransport:1"

/** The value of the SOAPACTION header for an AVTransport action. The quotes are part of the value. */
internal fun soapActionHeader(action: String): String = "\"$AV_TRANSPORT_SERVICE#$action\""

internal fun buildSoapRequestBody(action: String, arguments: List<Pair<String, String>>): String {
    val argumentsXml = arguments.joinToString("") { (name, value) -> "<$name>${escapeXml(value)}</$name>" }
    return """<?xml version="1.0" encoding="utf-8"?>""" +
        """<s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" """ +
        """s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">""" +
        "<s:Body>" +
        """<u:$action xmlns:u="$AV_TRANSPORT_SERVICE">""" +
        "<InstanceID>0</InstanceID>" +
        argumentsXml +
        "</u:$action>" +
        "</s:Body>" +
        "</s:Envelope>"
}

private fun escapeXml(text: String): String =
    text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
