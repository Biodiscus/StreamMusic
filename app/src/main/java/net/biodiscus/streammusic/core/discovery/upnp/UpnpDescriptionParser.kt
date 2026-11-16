package net.biodiscus.streammusic.core.discovery.upnp

import java.io.StringReader
import java.net.URI
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

private const val AV_TRANSPORT_SERVICE_MARKER = ":service:AVTransport:"

internal fun parseUpnpDescription(xml: String, locationUrl: String): UpnpDescription {
    val collector = DescriptionCollector()
    val parser = XmlPullParserFactory.newInstance().newPullParser()
    parser.setInput(StringReader(xml))

    var event = parser.eventType
    while (event != XmlPullParser.END_DOCUMENT) {
        when (event) {
            XmlPullParser.START_TAG -> collector.onTagStart(parser.name)
            XmlPullParser.TEXT -> collector.onText(parser.text.trim())
            XmlPullParser.END_TAG -> collector.onTagEnd(parser.name)
        }
        event = parser.next()
    }
    return collector.build(locationUrl)
}

/**
 * Collects the fields we need while the XML is read from top to bottom.
 * The root device always comes before its embedded devices, so the first value of a tag wins.
 */
private class DescriptionCollector {
    private var currentTag = ""
    private var friendlyName: String? = null
    private var roomName: String? = null
    private var manufacturer: String? = null
    private var modelName: String? = null

    private var serviceType: String? = null
    private var serviceControlUrl: String? = null
    private var avTransportControlUrl: String? = null

    fun onTagStart(tagName: String) {
        currentTag = tagName
    }

    fun onText(text: String) {
        if (text.isEmpty()) return
        when (currentTag) {
            "friendlyName" -> friendlyName = friendlyName ?: text
            "roomName" -> roomName = roomName ?: text
            "manufacturer" -> manufacturer = manufacturer ?: text
            "modelName" -> modelName = modelName ?: text
            "serviceType" -> serviceType = text
            "controlURL" -> serviceControlUrl = text
        }
    }

    fun onTagEnd(tagName: String) {
        currentTag = ""
        if (tagName == "service") finishService()
    }

    fun build(locationUrl: String) = UpnpDescription(
        friendlyName = friendlyName,
        roomName = roomName,
        manufacturer = manufacturer,
        modelName = modelName,
        avTransportControlUrl = avTransportControlUrl?.let { resolveAgainst(locationUrl, it) },
    )

    private fun finishService() {
        val isAvTransport = serviceType?.contains(AV_TRANSPORT_SERVICE_MARKER) == true
        if (isAvTransport && avTransportControlUrl == null) {
            avTransportControlUrl = serviceControlUrl
        }
        serviceType = null
        serviceControlUrl = null
    }

    private fun resolveAgainst(locationUrl: String, controlUrl: String): String =
        URI(locationUrl).resolve(controlUrl).toString()
}
