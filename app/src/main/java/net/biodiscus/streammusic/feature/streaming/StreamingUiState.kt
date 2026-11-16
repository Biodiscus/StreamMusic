package net.biodiscus.streammusic.feature.streaming

import net.biodiscus.streammusic.core.model.DeviceStreamStatus
import net.biodiscus.streammusic.core.model.StreamingDevice
import net.biodiscus.streammusic.core.model.StreamingState

internal data class DeviceStatusRow(
    val device: StreamingDevice,
    val status: DeviceStreamStatus,
)

internal sealed interface StreamingUiState {
    data object NotStreaming : StreamingUiState

    data class Streaming(val rows: List<DeviceStatusRow>) : StreamingUiState
}

internal fun toStreamingUiState(state: StreamingState): StreamingUiState =
    when (state) {
        StreamingState.Idle -> StreamingUiState.NotStreaming
        is StreamingState.Active -> StreamingUiState.Streaming(toRows(state))
    }

private fun toRows(state: StreamingState.Active): List<DeviceStatusRow> =
    state.devices.map { device ->
        val status = state.statusByDeviceId[device.id] ?: DeviceStreamStatus.Connecting
        DeviceStatusRow(device, status)
    }
