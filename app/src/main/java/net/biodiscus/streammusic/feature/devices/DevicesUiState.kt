package net.biodiscus.streammusic.feature.devices

import net.biodiscus.streammusic.core.model.DeviceProtocol
import net.biodiscus.streammusic.core.model.NetworkDevice
import net.biodiscus.streammusic.core.model.StreamingDevice

internal sealed interface DevicesUiState {

    /** The search is running and has not found anything yet. */
    data object Searching : DevicesUiState

    data class DevicesFound(
        val devices: List<NetworkDevice>,
        val selectedDeviceIds: Set<String>,
        /** The protocol the user chose per device id. A device without an entry streams with its best protocol. */
        val chosenProtocolByDeviceId: Map<String, DeviceProtocol> = emptyMap(),
    ) : DevicesUiState {
        val selectedCount: Int = selectedDeviceIds.size

        /** The ids of the streaming options to start: one per selected device, using the protocol it streams with. */
        val selectedStreamingDeviceIds: Set<String> =
            devices
                .filter { device -> device.id in selectedDeviceIds }
                .mapNotNull { device -> streamingOptionOf(device)?.id }
                .toSet()

        fun streamingOptionOf(device: NetworkDevice): StreamingDevice? =
            device.streamingOptionFor(chosenProtocolByDeviceId[device.id])
    }
}

internal fun toggleSelection(selectedDeviceIds: Set<String>, deviceId: String): Set<String> =
    if (deviceId in selectedDeviceIds) selectedDeviceIds - deviceId else selectedDeviceIds + deviceId
