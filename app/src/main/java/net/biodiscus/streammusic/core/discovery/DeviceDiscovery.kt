package net.biodiscus.streammusic.core.discovery

import kotlinx.coroutines.flow.Flow
import net.biodiscus.streammusic.core.model.StreamingDevice

/** One way of finding devices. Searching runs while the flow is collected. */
interface DeviceDiscovery {
    fun discover(): Flow<StreamingDevice>
}
