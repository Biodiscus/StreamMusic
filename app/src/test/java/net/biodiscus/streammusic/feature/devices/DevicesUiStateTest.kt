package net.biodiscus.streammusic.feature.devices

import net.biodiscus.streammusic.core.model.DeviceProtocol
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
}
