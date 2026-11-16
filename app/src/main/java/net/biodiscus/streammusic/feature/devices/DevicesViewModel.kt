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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import net.biodiscus.streammusic.core.discovery.DeviceRepository
import net.biodiscus.streammusic.core.groups.DeviceGroupRepository
import net.biodiscus.streammusic.core.model.AppSettings
import net.biodiscus.streammusic.core.model.DeviceGroup
import net.biodiscus.streammusic.core.model.DeviceGroupColor
import net.biodiscus.streammusic.core.model.DeviceGroupIcon
import net.biodiscus.streammusic.core.model.DeviceGroupMember
import net.biodiscus.streammusic.core.model.DeviceProtocol
import net.biodiscus.streammusic.core.model.NetworkDevice
import net.biodiscus.streammusic.core.model.StreamingDevice
import net.biodiscus.streammusic.core.model.StreamingState
import net.biodiscus.streammusic.core.model.groupIntoNetworkDevices
import net.biodiscus.streammusic.core.model.isSavableGroup
import net.biodiscus.streammusic.core.model.isValidGroupName
import net.biodiscus.streammusic.core.model.withoutDisabledProtocols
import net.biodiscus.streammusic.core.streaming.StreamingController
import net.biodiscus.streammusic.core.settings.SettingsRepository
import net.biodiscus.streammusic.core.streaming.StreamingStateRepository

private const val STOP_TIMEOUT_MILLIS = 5_000L

@HiltViewModel
internal class DevicesViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val streamingStateRepository: StreamingStateRepository,
    private val streamingController: StreamingController,
    private val deviceGroupRepository: DeviceGroupRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val selectedDeviceIds = MutableStateFlow(deviceIdsInStream(streamingStateRepository.state.value))
    private val chosenProtocolByDeviceId = MutableStateFlow<Map<String, DeviceProtocol>>(emptyMap())
    private val searchNumber = MutableStateFlow(0)

    val uiState: StateFlow<DevicesUiState> =
        combine(
            foundNetworkDevices(),
            selectedDeviceIds,
            chosenProtocolByDeviceId,
            streamingStateRepository.state,
            deviceGroupRepository.observeGroups(),
            ::toUiState,
        ).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = DevicesUiState.Searching(),
        )

    val hidesSingleAppCapture: StateFlow<Boolean> =
        settingsRepository.observeSettings()
            .map { settings -> settings.hidesSingleAppCapture }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = AppSettings().hidesSingleAppCapture,
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

    fun onSaveGroupClicked(name: String, icon: DeviceGroupIcon, color: DeviceGroupColor) {
        val foundDevices = uiState.value as? DevicesUiState.DevicesFound ?: return
        val trimmedName = name.trim()
        if (!isValidGroupName(trimmedName)) return
        deviceGroupRepository.saveGroup(DeviceGroup(trimmedName, foundDevices.selectedGroupMembers, icon, color))
    }

    fun onGroupEdited(
        group: DeviceGroup,
        name: String,
        icon: DeviceGroupIcon,
        members: List<DeviceGroupMember>,
    ) {
        val trimmedName = name.trim()
        if (!isSavableGroup(trimmedName, members.size)) return
        deviceGroupRepository.updateGroup(group.name, DeviceGroup(trimmedName, members, icon, group.color))
    }

    fun onDeleteGroupClicked(group: DeviceGroup) {
        deviceGroupRepository.deleteGroup(group.name)
    }

    /** Selects exactly the devices of the group that are on the network, each with the protocol saved in the group. */
    fun onGroupClicked(group: DeviceGroup) {
        val foundDevices = uiState.value as? DevicesUiState.DevicesFound ?: return
        val groupOptions = foundDevices.streamingOptionsOf(group)
        selectedDeviceIds.value = groupOptions.map { option -> option.host }.toSet()
        chosenProtocolByDeviceId.update { chosen ->
            chosen + groupOptions.associate { option -> option.host to option.protocol }
        }
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

    private fun foundNetworkDevices(): Flow<List<NetworkDevice>> =
        combine(searchedDevices(), settingsRepository.observeSettings(), ::toNetworkDevices)

    /** Starting a new search number restarts the network search. */
    private fun searchedDevices(): Flow<List<StreamingDevice>> =
        searchNumber.flatMapLatest { deviceRepository.observeDevices() }

    private fun toNetworkDevices(devices: List<StreamingDevice>, settings: AppSettings): List<NetworkDevice> =
        devices.withoutDisabledProtocols(settings).groupIntoNetworkDevices()

    private fun toUiState(
        devices: List<NetworkDevice>,
        selectedIds: Set<String>,
        chosenProtocols: Map<String, DeviceProtocol>,
        streamingState: StreamingState,
        groups: List<DeviceGroup>,
    ): DevicesUiState {
        val isStreaming = streamingState is StreamingState.Active
        if (devices.isEmpty()) return DevicesUiState.Searching(isStreaming)
        return DevicesUiState.DevicesFound(
            devices = devices,
            selectedDeviceIds = selectedIds,
            chosenProtocolByDeviceId = chosenProtocols,
            streamStatusByDeviceId = streamStatusByDeviceId(streamingState),
            groups = groups,
            isStreaming = isStreaming,
        )
    }
}
