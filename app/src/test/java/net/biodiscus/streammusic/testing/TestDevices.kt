package net.biodiscus.streammusic.testing

import net.biodiscus.streammusic.core.model.DeviceGroup
import net.biodiscus.streammusic.core.model.DeviceGroupMember
import net.biodiscus.streammusic.core.model.DeviceProtocol
import net.biodiscus.streammusic.core.model.NetworkDevice
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

fun aNetworkDevice(vararg options: StreamingDevice) = NetworkDevice(host = options.first().host, options = options.toList())

fun aGroupMember(
    name: String = "Living Room",
    protocol: DeviceProtocol = DeviceProtocol.DLNA,
    host: String = "192.168.1.10",
) = DeviceGroupMember(host = host, name = name, protocol = protocol)

fun aGroup(name: String = "Downstairs", vararg members: DeviceGroupMember = arrayOf(aGroupMember())) =
    DeviceGroup(name = name, members = members.toList())
