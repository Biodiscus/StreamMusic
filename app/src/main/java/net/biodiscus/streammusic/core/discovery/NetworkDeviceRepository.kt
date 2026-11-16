package net.biodiscus.streammusic.core.discovery

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import net.biodiscus.streammusic.core.model.StreamingDevice

/**
 * Merges every [DeviceDiscovery] into one device list.
 * Devices are remembered by id, so a device seen twice (for example over SSDP and mDNS) is listed once.
 */
@Singleton
internal class NetworkDeviceRepository @Inject constructor(
    private val discoveries: @JvmSuppressWildcards Set<DeviceDiscovery>,
) : DeviceRepository {

    private val knownDevices = MutableStateFlow<Map<String, StreamingDevice>>(emptyMap())

    override fun observeDevices(): Flow<List<StreamingDevice>> {
        val allFoundDevices = merge(*discoveries.map { it.discover() }.toTypedArray())
        return allFoundDevices
            .onEach { device -> rememberDevice(device) }
            .map { sortedKnownDevices() }
            .onStart { emit(sortedKnownDevices()) }
    }

    override fun findDevices(ids: Set<String>): List<StreamingDevice> =
        knownDevices.value.values.filter { device -> device.id in ids }

    override fun forgetAllDevices() {
        knownDevices.value = emptyMap()
    }

    private fun rememberDevice(device: StreamingDevice) {
        knownDevices.update { devices -> devices + (device.id to device) }
    }

    private fun sortedKnownDevices(): List<StreamingDevice> =
        knownDevices.value.values.sortedBy { device -> device.name.lowercase() }
}
