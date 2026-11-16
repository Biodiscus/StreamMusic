package net.biodiscus.streammusic.feature.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import net.biodiscus.streammusic.core.discovery.DeviceRepository
import net.biodiscus.streammusic.core.model.NetworkDevice
import net.biodiscus.streammusic.core.model.StreamingDevice
import net.biodiscus.streammusic.core.model.groupIntoNetworkDevices

private const val STOP_TIMEOUT_MILLIS = 5_000L

@HiltViewModel
internal class DevicesViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
) : ViewModel() {

    private val selectedDeviceIds = MutableStateFlow<Set<String>>(emptySet())
    private val searchNumber = MutableStateFlow(0)

    val uiState: StateFlow<DevicesUiState> =
        combine(foundDevices(), selectedDeviceIds, ::toUiState)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = DevicesUiState.Searching,
            )

    fun onDeviceClicked(device: NetworkDevice) {
        if (!device.canReceiveStream) return
        selectedDeviceIds.update { selected -> toggleSelection(selected, device.id) }
    }

    fun onRefreshClicked() {
        deviceRepository.forgetAllDevices()
        selectedDeviceIds.value = emptySet()
        searchNumber.update { number -> number + 1 }
    }

    /** Starting a new search number restarts the network search. */
    private fun foundDevices(): Flow<List<StreamingDevice>> =
        searchNumber.flatMapLatest { deviceRepository.observeDevices() }

    private fun toUiState(devices: List<StreamingDevice>, selectedIds: Set<String>): DevicesUiState =
        if (devices.isEmpty()) {
            DevicesUiState.Searching
        } else {
            DevicesUiState.DevicesFound(devices.groupIntoNetworkDevices(), selectedIds)
        }
}
