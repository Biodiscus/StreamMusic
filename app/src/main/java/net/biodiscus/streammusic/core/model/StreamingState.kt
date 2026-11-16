package net.biodiscus.streammusic.core.model

sealed interface DeviceStreamStatus {
    data object Connecting : DeviceStreamStatus
    data object Streaming : DeviceStreamStatus
    data class Failed(val reason: String) : DeviceStreamStatus
}

sealed interface StreamingState {
    data object Idle : StreamingState

    data class Active(
        val devices: List<StreamingDevice>,
        val statusByDeviceId: Map<String, DeviceStreamStatus>,
    ) : StreamingState
}
