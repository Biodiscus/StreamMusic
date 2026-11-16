package net.biodiscus.streammusic.feature.devices

import net.biodiscus.streammusic.core.model.NetworkDevice

internal sealed interface DevicesUiState {

    /** The search is running and has not found anything yet. */
    data object Searching : DevicesUiState

    data class DevicesFound(
        val devices: List<NetworkDevice>,
        val selectedDeviceIds: Set<String>,
    ) : DevicesUiState {
        val selectedCount: Int = selectedDeviceIds.size

        /** The ids of the streaming options to start: one per selected device, using its best protocol. */
        val selectedStreamingDeviceIds: Set<String> =
            devices
                .filter { device -> device.id in selectedDeviceIds }
                .mapNotNull { device -> device.bestStreamingOption?.id }
                .toSet()
    }
}

internal fun toggleSelection(selectedDeviceIds: Set<String>, deviceId: String): Set<String> =
    if (deviceId in selectedDeviceIds) selectedDeviceIds - deviceId else selectedDeviceIds + deviceId
