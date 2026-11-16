package net.biodiscus.streammusic.feature.devices

import net.biodiscus.streammusic.core.model.StreamingDevice

internal sealed interface DevicesUiState {

    /** The search is running and has not found anything yet. */
    data object Searching : DevicesUiState

    data class DevicesFound(
        val devices: List<StreamingDevice>,
        val selectedDeviceIds: Set<String>,
    ) : DevicesUiState {
        val selectedCount: Int = selectedDeviceIds.size
    }
}

internal fun toggleSelection(selectedDeviceIds: Set<String>, deviceId: String): Set<String> =
    if (deviceId in selectedDeviceIds) selectedDeviceIds - deviceId else selectedDeviceIds + deviceId
