package net.biodiscus.streammusic.feature.devices

import net.biodiscus.streammusic.core.model.DeviceProtocol
import net.biodiscus.streammusic.core.model.DeviceStreamStatus
import net.biodiscus.streammusic.core.model.StreamingState
import net.biodiscus.streammusic.testing.aDevice
import net.biodiscus.streammusic.testing.aNetworkDevice
import org.junit.Assert.assertEquals
import org.junit.Test

class DevicesUiStateTest {

    @Test
    fun `selecting an unselected device adds it`() {
        assertEquals(setOf("a", "b"), toggleSelection(setOf("a"), "b"))
    }

    @Test
    fun `selecting a selected device removes it`() {
        assertEquals(setOf("a"), toggleSelection(setOf("a", "b"), "b"))
    }

    @Test
    fun `a selected device is streamed to with its best protocol`() {
        val dlna = aDevice(protocol = DeviceProtocol.DLNA, host = "10.0.0.2")
        val chromecast = aDevice(protocol = DeviceProtocol.CHROMECAST, host = "10.0.0.2")
        val uiState = DevicesUiState.DevicesFound(listOf(aNetworkDevice(dlna, chromecast)), setOf("10.0.0.2"))

        assertEquals(setOf(chromecast.id), uiState.selectedStreamingDeviceIds)
    }

    @Test
    fun `a selected device is streamed to with the protocol the user chose`() {
        val dlna = aDevice(protocol = DeviceProtocol.DLNA, host = "10.0.0.2")
        val chromecast = aDevice(protocol = DeviceProtocol.CHROMECAST, host = "10.0.0.2")
        val uiState = DevicesUiState.DevicesFound(
            devices = listOf(aNetworkDevice(dlna, chromecast)),
            selectedDeviceIds = setOf("10.0.0.2"),
            chosenProtocolByDeviceId = mapOf("10.0.0.2" to DeviceProtocol.DLNA),
        )

        assertEquals(setOf(dlna.id), uiState.selectedStreamingDeviceIds)
    }

    @Test
    fun `a device that is not selected is not streamed to`() {
        val uiState = DevicesUiState.DevicesFound(listOf(aNetworkDevice(aDevice())), emptySet())

        assertEquals(emptySet<String>(), uiState.selectedStreamingDeviceIds)
    }

    @Test
    fun `nothing is in the stream while idle`() {
        assertEquals(emptySet<String>(), deviceIdsInStream(StreamingState.Idle))
    }

    @Test
    fun `the devices in the stream are found by their host`() {
        val kitchen = aDevice(host = "10.0.0.2")
        val study = aDevice(host = "10.0.0.3")
        val active = StreamingState.Active(listOf(kitchen, study), emptyMap())

        assertEquals(setOf("10.0.0.2", "10.0.0.3"), deviceIdsInStream(active))
    }

    @Test
    fun `there are no statuses while idle`() {
        assertEquals(emptyMap<String, DeviceStreamStatus>(), streamStatusByDeviceId(StreamingState.Idle))
    }

    @Test
    fun `a status belongs to the host of its streaming device`() {
        val kitchen = aDevice(host = "10.0.0.2")
        val active = StreamingState.Active(listOf(kitchen), mapOf(kitchen.id to DeviceStreamStatus.Streaming))

        assertEquals(mapOf("10.0.0.2" to DeviceStreamStatus.Streaming), streamStatusByDeviceId(active))
    }

    @Test
    fun `a streaming device without a status yet is connecting`() {
        val kitchen = aDevice(host = "10.0.0.2")
        val active = StreamingState.Active(listOf(kitchen), emptyMap())

        assertEquals(mapOf("10.0.0.2" to DeviceStreamStatus.Connecting), streamStatusByDeviceId(active))
    }

    @Test
    fun `the protocol can be chosen for an idle device that speaks several protocols`() {
        val device = aNetworkDevice(aDevice(protocol = DeviceProtocol.DLNA), aDevice(protocol = DeviceProtocol.CHROMECAST))

        assertEquals(true, canChooseProtocol(device, streamStatus = null))
    }

    @Test
    fun `the protocol cannot be chosen for a device that is playing`() {
        val device = aNetworkDevice(aDevice(protocol = DeviceProtocol.DLNA), aDevice(protocol = DeviceProtocol.CHROMECAST))

        assertEquals(false, canChooseProtocol(device, DeviceStreamStatus.Streaming))
    }

    @Test
    fun `there is no protocol to choose for a device that speaks only one`() {
        val device = aNetworkDevice(aDevice(protocol = DeviceProtocol.DLNA))

        assertEquals(false, canChooseProtocol(device, streamStatus = null))
    }
}
