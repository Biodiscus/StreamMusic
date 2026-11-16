package net.biodiscus.streammusic.core.discovery

import kotlinx.coroutines.flow.Flow
import net.biodiscus.streammusic.core.model.StreamingDevice

interface DeviceRepository {
    /** Searches the network while collected. Emits the full list of devices found so far, sorted by name. */
    fun observeDevices(): Flow<List<StreamingDevice>>

    fun findDevices(ids: Set<String>): List<StreamingDevice>

    fun forgetAllDevices()
}
