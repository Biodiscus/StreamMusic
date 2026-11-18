package net.biodiscus.streammusic.feature.devices

import net.biodiscus.streammusic.core.model.DeviceProtocol
import net.biodiscus.streammusic.core.model.DeviceStreamStatus
import net.biodiscus.streammusic.core.model.StreamingState
import net.biodiscus.streammusic.testing.aDevice
import net.biodiscus.streammusic.testing.aGroup
import net.biodiscus.streammusic.testing.aGroupMember
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

    @Test
    fun `two selected devices can be saved as a group`() {
        val devices = listOf(aNetworkDevice(aDevice(host = "10.0.0.2")), aNetworkDevice(aDevice(host = "10.0.0.3")))
        val uiState = DevicesUiState.DevicesFound(devices, setOf("10.0.0.2", "10.0.0.3"))

        assertEquals(true, uiState.canSaveGroup)
    }

    @Test
    fun `one selected device cannot be saved as a group`() {
        val devices = listOf(aNetworkDevice(aDevice(host = "10.0.0.2")), aNetworkDevice(aDevice(host = "10.0.0.3")))
        val uiState = DevicesUiState.DevicesFound(devices, setOf("10.0.0.2"))

        assertEquals(false, uiState.canSaveGroup)
    }

    @Test
    fun `a saved group member is the selected device with the protocol it streams with`() {
        val chromecast = aDevice(name = "TV", protocol = DeviceProtocol.CHROMECAST, host = "10.0.0.2")
        val uiState = DevicesUiState.DevicesFound(listOf(aNetworkDevice(chromecast)), setOf("10.0.0.2"))

        assertEquals(listOf(aGroupMember("TV", DeviceProtocol.CHROMECAST, "10.0.0.2")), uiState.selectedGroupMembers)
    }

    @Test
    fun `a member can stream with every protocol its device speaks`() {
        val chromecast = aDevice(protocol = DeviceProtocol.CHROMECAST, host = "10.0.0.2")
        val dlna = aDevice(protocol = DeviceProtocol.DLNA, host = "10.0.0.2")
        val uiState = DevicesUiState.DevicesFound(listOf(aNetworkDevice(chromecast, dlna)), emptySet())

        val choices = uiState.protocolChoicesFor(aGroupMember(protocol = DeviceProtocol.DLNA, host = "10.0.0.2"))

        assertEquals(listOf(DeviceProtocol.CHROMECAST, DeviceProtocol.DLNA), choices)
    }

    @Test
    fun `a member that is offline can only stream with its saved protocol`() {
        val uiState = DevicesUiState.DevicesFound(emptyList(), emptySet())

        val choices = uiState.protocolChoicesFor(aGroupMember(protocol = DeviceProtocol.DLNA, host = "10.0.0.99"))

        assertEquals(listOf(DeviceProtocol.DLNA), choices)
    }

    @Test
    fun `groups are shown while idle`() {
        val uiState = DevicesUiState.DevicesFound(emptyList(), emptySet(), groups = listOf(aGroup()))

        assertEquals(true, uiState.showsGroups)
    }

    @Test
    fun `groups are shown while streaming`() {
        val uiState = DevicesUiState.DevicesFound(emptyList(), emptySet(), groups = listOf(aGroup()), isStreaming = true)

        assertEquals(true, uiState.showsGroups)
    }

    @Test
    fun `groups are hidden when none is saved`() {
        val uiState = DevicesUiState.DevicesFound(emptyList(), emptySet())

        assertEquals(false, uiState.showsGroups)
    }

    @Test
    fun `there is a stream button while devices are selected`() {
        val uiState = DevicesUiState.DevicesFound(listOf(aNetworkDevice(aDevice())), setOf(aDevice().host))

        assertEquals(true, uiState.hasStreamingButton())
    }

    @Test
    fun `there is a stream button to stop while streaming`() {
        assertEquals(true, DevicesUiState.Searching(isStreaming = true).hasStreamingButton())
    }

    @Test
    fun `there is no stream button while nothing is selected and nothing streams`() {
        val uiState = DevicesUiState.DevicesFound(listOf(aNetworkDevice(aDevice())), emptySet())

        assertEquals(false, uiState.hasStreamingButton())
    }
}
