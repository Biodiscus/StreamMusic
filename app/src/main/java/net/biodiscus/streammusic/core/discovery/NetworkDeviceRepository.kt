package net.biodiscus.streammusic.core.discovery

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import net.biodiscus.streammusic.core.logging.describeForLog
import net.biodiscus.streammusic.core.model.StreamingDevice
import timber.log.Timber

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
            .onStart {
                Timber.i("Started searching the network with %d methods", discoveries.size)
                emit(sortedKnownDevices())
            }
            .onCompletion { Timber.i("Stopped searching the network") }
    }

    override fun findDevices(ids: Set<String>): List<StreamingDevice> =
        knownDevices.value.values.filter { device -> device.id in ids }

    override fun forgetAllDevices() {
        Timber.d("Forgetting the %d known devices", knownDevices.value.size)
        knownDevices.value = emptyMap()
    }

    /** The same device is reported again and again while searching, so only news is logged. */
    private fun logDiscovery(device: StreamingDevice) {
        val knownDevice = knownDevices.value[device.id]
        if (knownDevice == null) {
            Timber.i("Found %s, can stream: %s", device.describeForLog(), device.canReceiveStream)
            if (device.raop != null) Timber.d("%s announces %s", device.name, device.raop)
        } else if (knownDevice != device) {
            Timber.d("%s changed to %s", knownDevice.describeForLog(), device.describeForLog())
        }
    }

    private fun rememberDevice(device: StreamingDevice) {
        logDiscovery(device)
        knownDevices.update { devices -> devices + (device.id to device) }
    }

    private fun sortedKnownDevices(): List<StreamingDevice> =
        knownDevices.value.values.sortedBy { device -> device.name.lowercase() }
}
