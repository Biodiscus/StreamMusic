package net.biodiscus.streammusic.core.streaming

import net.biodiscus.streammusic.core.model.DeviceStreamStatus
import net.biodiscus.streammusic.core.model.StreamingState
import net.biodiscus.streammusic.testing.aDevice
import org.junit.Assert.assertEquals
import org.junit.Test

class StreamingStateRepositoryTest {

    private val repository = StreamingStateRepository()
    private val kitchen = aDevice(name = "Kitchen", host = "10.0.0.2")
    private val garage = aDevice(name = "Garage", host = "10.0.0.3")

    @Test
    fun `is idle at first`() {
        assertEquals(StreamingState.Idle, repository.state.value)
    }

    @Test
    fun `starting marks every device as connecting`() {
        repository.streamingStarted(listOf(kitchen, garage))

        val expected = StreamingState.Active(
            devices = listOf(kitchen, garage),
            statusByDeviceId = mapOf(
                kitchen.id to DeviceStreamStatus.Connecting,
                garage.id to DeviceStreamStatus.Connecting,
            ),
        )
        assertEquals(expected, repository.state.value)
    }

    @Test
    fun `a status change only affects its own device`() {
        repository.streamingStarted(listOf(kitchen, garage))

        repository.deviceStatusChanged(kitchen.id, DeviceStreamStatus.Streaming)

        val active = repository.state.value as StreamingState.Active
        assertEquals(DeviceStreamStatus.Streaming, active.statusByDeviceId[kitchen.id])
        assertEquals(DeviceStreamStatus.Connecting, active.statusByDeviceId[garage.id])
    }

    @Test
    fun `a status change while idle is ignored`() {
        repository.deviceStatusChanged(kitchen.id, DeviceStreamStatus.Streaming)

        assertEquals(StreamingState.Idle, repository.state.value)
    }

    @Test
    fun `ending returns to idle`() {
        repository.streamingStarted(listOf(kitchen))

        repository.streamingEnded()

        assertEquals(StreamingState.Idle, repository.state.value)
    }

    @Test
    fun `an added device is part of the stream and connecting`() {
        repository.streamingStarted(listOf(kitchen))

        repository.deviceAdded(garage)

        val active = repository.state.value as StreamingState.Active
        assertEquals(listOf(kitchen, garage), active.devices)
        assertEquals(DeviceStreamStatus.Connecting, active.statusByDeviceId[garage.id])
    }

    @Test
    fun `adding a device while idle is ignored`() {
        repository.deviceAdded(kitchen)

        assertEquals(StreamingState.Idle, repository.state.value)
    }

    @Test
    fun `a removed device is no longer part of the stream`() {
        repository.streamingStarted(listOf(kitchen, garage))

        repository.deviceRemoved(garage.id)

        val expected = StreamingState.Active(
            devices = listOf(kitchen),
            statusByDeviceId = mapOf(kitchen.id to DeviceStreamStatus.Connecting),
        )
        assertEquals(expected, repository.state.value)
    }

    @Test
    fun `a late status of a removed device does not bring it back`() {
        repository.streamingStarted(listOf(kitchen, garage))
        repository.deviceRemoved(garage.id)

        repository.deviceStatusChanged(garage.id, DeviceStreamStatus.Streaming)

        val active = repository.state.value as StreamingState.Active
        assertEquals(null, active.statusByDeviceId[garage.id])
    }
}
