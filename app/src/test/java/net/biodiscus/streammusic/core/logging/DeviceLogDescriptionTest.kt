package net.biodiscus.streammusic.core.logging

import net.biodiscus.streammusic.core.model.DeviceProtocol
import net.biodiscus.streammusic.testing.aDevice
import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceLogDescriptionTest {

    @Test
    fun `describes a device by name, protocol and address`() {
        val device = aDevice(name = "Kitchen", protocol = DeviceProtocol.SONOS, host = "10.0.0.2")

        assertEquals("Kitchen (SONOS at 10.0.0.2:80)", device.describeForLog())
    }
}
