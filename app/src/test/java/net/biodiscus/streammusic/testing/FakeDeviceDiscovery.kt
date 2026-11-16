package net.biodiscus.streammusic.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import net.biodiscus.streammusic.core.discovery.DeviceDiscovery
import net.biodiscus.streammusic.core.model.StreamingDevice

class FakeDeviceDiscovery(private vararg val devices: StreamingDevice) : DeviceDiscovery {
    override fun discover(): Flow<StreamingDevice> = devices.asFlow()
}
