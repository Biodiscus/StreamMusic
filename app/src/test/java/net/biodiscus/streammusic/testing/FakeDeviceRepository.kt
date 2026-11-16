package net.biodiscus.streammusic.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import net.biodiscus.streammusic.core.discovery.DeviceRepository
import net.biodiscus.streammusic.core.model.StreamingDevice

class FakeDeviceRepository : DeviceRepository {

    private val devices = MutableStateFlow<List<StreamingDevice>>(emptyList())

    var observeCount = 0
        private set
    var forgetAllCount = 0
        private set

    fun setFoundDevices(vararg found: StreamingDevice) {
        devices.value = found.toList()
    }

    override fun observeDevices(): Flow<List<StreamingDevice>> {
        observeCount++
        return devices
    }

    override fun findDevices(ids: Set<String>): List<StreamingDevice> =
        devices.value.filter { device -> device.id in ids }

    override fun forgetAllDevices() {
        forgetAllCount++
        devices.value = emptyList()
    }
}
