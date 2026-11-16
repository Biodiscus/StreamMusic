package net.biodiscus.streammusic.core.streaming

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import net.biodiscus.streammusic.core.model.DeviceStreamStatus
import net.biodiscus.streammusic.core.model.StreamingDevice
import net.biodiscus.streammusic.core.model.StreamingState

/** What the screens can observe about the streaming service, which runs outside of any screen. */
@Singleton
class StreamingStateRepository @Inject constructor() {

    private val mutableState = MutableStateFlow<StreamingState>(StreamingState.Idle)

    val state: StateFlow<StreamingState> = mutableState.asStateFlow()

    fun streamingStarted(devices: List<StreamingDevice>) {
        val allConnecting = devices.associate { device -> device.id to DeviceStreamStatus.Connecting }
        mutableState.value = StreamingState.Active(devices, allConnecting)
    }

    fun deviceAdded(device: StreamingDevice) {
        mutableState.update { current ->
            if (current is StreamingState.Active) {
                current.copy(
                    devices = current.devices + device,
                    statusByDeviceId = current.statusByDeviceId + (device.id to DeviceStreamStatus.Connecting),
                )
            } else {
                current
            }
        }
    }

    fun deviceRemoved(deviceId: String) {
        mutableState.update { current ->
            if (current is StreamingState.Active) {
                current.copy(
                    devices = current.devices.filterNot { device -> device.id == deviceId },
                    statusByDeviceId = current.statusByDeviceId - deviceId,
                )
            } else {
                current
            }
        }
    }

    /** A device that is not part of the session (anymore) is ignored, so a late report cannot bring it back. */
    fun deviceStatusChanged(deviceId: String, status: DeviceStreamStatus) {
        mutableState.update { current ->
            if (current is StreamingState.Active && deviceId in current.statusByDeviceId) {
                current.copy(statusByDeviceId = current.statusByDeviceId + (deviceId to status))
            } else {
                current
            }
        }
    }

    fun streamingEnded() {
        mutableState.value = StreamingState.Idle
    }
}
