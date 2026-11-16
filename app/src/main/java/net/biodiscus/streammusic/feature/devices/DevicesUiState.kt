package net.biodiscus.streammusic.feature.devices

import net.biodiscus.streammusic.core.model.DeviceGroup
import net.biodiscus.streammusic.core.model.DeviceGroupMember
import net.biodiscus.streammusic.core.model.DeviceProtocol
import net.biodiscus.streammusic.core.model.DeviceStreamStatus
import net.biodiscus.streammusic.core.model.MIN_DEVICES_IN_GROUP
import net.biodiscus.streammusic.core.model.NetworkDevice
import net.biodiscus.streammusic.core.model.StreamingDevice
import net.biodiscus.streammusic.core.model.StreamingState
import net.biodiscus.streammusic.core.model.streamingOptionsIn

internal sealed interface DevicesUiState {

    val isStreaming: Boolean

    /** The search is running and has not found anything yet. */
    data class Searching(override val isStreaming: Boolean = false) : DevicesUiState

    data class DevicesFound(
        val devices: List<NetworkDevice>,
        val selectedDeviceIds: Set<String>,
        /** The protocol the user chose per device id. A device without an entry streams with its best protocol. */
        val chosenProtocolByDeviceId: Map<String, DeviceProtocol> = emptyMap(),
        /** The status of every device that is part of the running stream, per device id. */
        val streamStatusByDeviceId: Map<String, DeviceStreamStatus> = emptyMap(),
        val groups: List<DeviceGroup> = emptyList(),
        override val isStreaming: Boolean = false,
    ) : DevicesUiState {
        val selectedCount: Int = selectedDeviceIds.size

        /** Starting a group only makes sense while nothing is streaming, because it replaces the selection. */
        val showsGroups: Boolean = groups.isNotEmpty() && !isStreaming

        private val selectedStreamingOptions: List<StreamingDevice> =
            devices
                .filter { device -> device.id in selectedDeviceIds }
                .mapNotNull { device -> streamingOptionOf(device) }

        /** The ids of the streaming options to start: one per selected device, using the protocol it streams with. */
        val selectedStreamingDeviceIds: Set<String> = selectedStreamingOptions.map { option -> option.id }.toSet()

        /** The selected devices as they would be saved in a group. */
        val selectedGroupMembers: List<DeviceGroupMember> = selectedStreamingOptions.map { option -> option.toGroupMember() }

        val canSaveGroup: Boolean = selectedGroupMembers.size >= MIN_DEVICES_IN_GROUP

        /** The protocols a group member can stream with: the ones its device speaks now, or just the saved one while it is offline. */
        fun protocolChoicesFor(member: DeviceGroupMember): List<DeviceProtocol> {
            val device = devices.firstOrNull { networkDevice -> networkDevice.host == member.host }
            if (device == null) return listOf(member.protocol)
            return device.streamingOptions.map { option -> option.protocol }
        }

        fun streamingOptionsOf(group: DeviceGroup): List<StreamingDevice> = group.streamingOptionsIn(devices)

        fun streamingOptionOf(device: NetworkDevice): StreamingDevice? =
            device.streamingOptionFor(chosenProtocolByDeviceId[device.id])
    }
}

private fun StreamingDevice.toGroupMember() = DeviceGroupMember(host = host, name = name, protocol = protocol)

/** The stream button is there to stop a running stream, or to start one for the selected devices. */
internal fun DevicesUiState.hasStreamingButton(): Boolean =
    isStreaming || (this is DevicesUiState.DevicesFound && selectedCount > 0)

internal fun toggleSelection(selectedDeviceIds: Set<String>, deviceId: String): Set<String> =
    if (deviceId in selectedDeviceIds) selectedDeviceIds - deviceId else selectedDeviceIds + deviceId

/** A device that is playing keeps its protocol, because switching it while it plays is not supported. */
internal fun canChooseProtocol(device: NetworkDevice, streamStatus: DeviceStreamStatus?): Boolean =
    device.streamingOptions.size > 1 && streamStatus == null

/** The ids of the network devices that the running stream plays on. They are the host of the streaming device. */
internal fun deviceIdsInStream(streamingState: StreamingState): Set<String> =
    when (streamingState) {
        StreamingState.Idle -> emptySet()
        is StreamingState.Active -> streamingState.devices.map { device -> device.host }.toSet()
    }

internal fun streamStatusByDeviceId(streamingState: StreamingState): Map<String, DeviceStreamStatus> =
    when (streamingState) {
        StreamingState.Idle -> emptyMap()
        is StreamingState.Active -> streamingState.devices.associate { device ->
            device.host to (streamingState.statusByDeviceId[device.id] ?: DeviceStreamStatus.Connecting)
        }
    }
