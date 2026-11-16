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
import net.biodiscus.streammusic.core.model.DeviceProtocol
import net.biodiscus.streammusic.core.model.NetworkDevice
import net.biodiscus.streammusic.core.model.StreamingDevice
import net.biodiscus.streammusic.core.model.StreamingState
import net.biodiscus.streammusic.core.model.groupIntoNetworkDevices
import net.biodiscus.streammusic.core.streaming.StreamingController
import net.biodiscus.streammusic.core.streaming.StreamingStateRepository

private const val STOP_TIMEOUT_MILLIS = 5_000L

@HiltViewModel
internal class DevicesViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val streamingStateRepository: StreamingStateRepository,
    private val streamingController: StreamingController,
) : ViewModel() {

    private val selectedDeviceIds = MutableStateFlow(deviceIdsInStream(streamingStateRepository.state.value))
    private val chosenProtocolByDeviceId = MutableStateFlow<Map<String, DeviceProtocol>>(emptyMap())
    private val searchNumber = MutableStateFlow(0)

    val uiState: StateFlow<DevicesUiState> =
        combine(
            foundDevices(),
            selectedDeviceIds,
            chosenProtocolByDeviceId,
            streamingStateRepository.state,
            ::toUiState,
        ).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = DevicesUiState.Searching(),
        )

    /** While a stream is running, ticking a device also starts it and unticking stops it. */
    fun onDeviceClicked(device: NetworkDevice) {
        if (!device.canReceiveStream) return
        selectedDeviceIds.update { selected -> toggleSelection(selected, device.id) }
        if (isStreaming()) updateRunningStream(device)
    }

    fun onProtocolChosen(device: NetworkDevice, protocol: DeviceProtocol) {
        chosenProtocolByDeviceId.update { chosen -> chosen + (device.id to protocol) }
    }

    fun onStopClicked() {
        streamingController.stop()
    }

    fun onRefreshClicked() {
        deviceRepository.forgetAllDevices()
        if (!isStreaming()) selectedDeviceIds.value = emptySet()
        searchNumber.update { number -> number + 1 }
    }

    private fun isStreaming(): Boolean = streamingStateRepository.state.value is StreamingState.Active

    private fun updateRunningStream(device: NetworkDevice) {
        if (device.id in selectedDeviceIds.value) {
            startStreamingTo(device)
        } else {
            stopStreamingTo(device)
        }
    }

    private fun startStreamingTo(device: NetworkDevice) {
        val option = device.streamingOptionFor(chosenProtocolByDeviceId.value[device.id]) ?: return
        streamingController.addDevice(option.id)
    }

    private fun stopStreamingTo(device: NetworkDevice) {
        if (selectedDeviceIds.value.isEmpty()) {
            streamingController.stop()
            return
        }
        val deviceInStream = findDeviceInStream(device) ?: return
        streamingController.removeDevice(deviceInStream.id)
    }

    private fun findDeviceInStream(device: NetworkDevice): StreamingDevice? {
        val streamingState = streamingStateRepository.state.value as? StreamingState.Active ?: return null
        return streamingState.devices.firstOrNull { streamingDevice -> streamingDevice.host == device.id }
    }

    /** Starting a new search number restarts the network search. */
    private fun foundDevices(): Flow<List<StreamingDevice>> =
        searchNumber.flatMapLatest { deviceRepository.observeDevices() }

    private fun toUiState(
        devices: List<StreamingDevice>,
        selectedIds: Set<String>,
        chosenProtocols: Map<String, DeviceProtocol>,
        streamingState: StreamingState,
    ): DevicesUiState {
        val isStreaming = streamingState is StreamingState.Active
        if (devices.isEmpty()) return DevicesUiState.Searching(isStreaming)
        return DevicesUiState.DevicesFound(
            devices = devices.groupIntoNetworkDevices(),
            selectedDeviceIds = selectedIds,
            chosenProtocolByDeviceId = chosenProtocols,
            streamStatusByDeviceId = streamStatusByDeviceId(streamingState),
            isStreaming = isStreaming,
        )
    }
}
