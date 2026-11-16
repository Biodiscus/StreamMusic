package net.biodiscus.streammusic.feature.streaming

import net.biodiscus.streammusic.core.model.DeviceStreamStatus
import net.biodiscus.streammusic.core.model.StreamingState
import net.biodiscus.streammusic.testing.aDevice
import org.junit.Assert.assertEquals
import org.junit.Test

class StreamingUiStateTest {

    private val kitchen = aDevice(name = "Kitchen", host = "10.0.0.2")
    private val study = aDevice(name = "Study", host = "10.0.0.3")

    @Test
    fun `idle means not streaming`() {
        assertEquals(StreamingUiState.NotStreaming, toStreamingUiState(StreamingState.Idle))
    }

    @Test
    fun `every device gets a row with its status`() {
        val state = StreamingState.Active(
            devices = listOf(kitchen, study),
            statusByDeviceId = mapOf(
                kitchen.id to DeviceStreamStatus.Streaming,
                study.id to DeviceStreamStatus.Failed("Refused"),
            ),
        )

        val uiState = toStreamingUiState(state)

        val expectedRows = listOf(
            DeviceStatusRow(kitchen, DeviceStreamStatus.Streaming),
            DeviceStatusRow(study, DeviceStreamStatus.Failed("Refused")),
        )
        assertEquals(StreamingUiState.Streaming(expectedRows), uiState)
    }

    @Test
    fun `a device without a status yet is connecting`() {
        val state = StreamingState.Active(devices = listOf(kitchen), statusByDeviceId = emptyMap())

        val uiState = toStreamingUiState(state) as StreamingUiState.Streaming

        assertEquals(DeviceStreamStatus.Connecting, uiState.rows.single().status)
    }
}
