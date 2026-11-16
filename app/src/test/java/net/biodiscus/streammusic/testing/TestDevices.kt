package net.biodiscus.streammusic.testing

import net.biodiscus.streammusic.core.model.DeviceProtocol
import net.biodiscus.streammusic.core.model.StreamingDevice

fun aDevice(
    name: String = "Living Room",
    protocol: DeviceProtocol = DeviceProtocol.DLNA,
    host: String = "192.168.1.10",
) = StreamingDevice(
    name = name,
    protocol = protocol,
    host = host,
    port = 80,
    avTransportControlUrl = "http://$host/control",
)
