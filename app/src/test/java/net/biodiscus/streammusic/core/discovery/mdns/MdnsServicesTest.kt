package net.biodiscus.streammusic.core.discovery.mdns

import net.biodiscus.streammusic.core.model.DeviceProtocol
import org.junit.Assert.assertEquals
import org.junit.Test

class MdnsServicesTest {

    @Test
    fun `Chromecast uses the friendly name text record`() {
        val name = friendlyMdnsName(
            protocol = DeviceProtocol.CHROMECAST,
            serviceName = "Google-Home-Mini-8f3a",
            textRecords = mapOf("fn" to "Kitchen speaker"),
        )

        assertEquals("Kitchen speaker", name)
    }

    @Test
    fun `Chromecast without friendly name text record keeps the service name`() {
        val name = friendlyMdnsName(DeviceProtocol.CHROMECAST, "Google-Home-Mini-8f3a", emptyMap())

        assertEquals("Google-Home-Mini-8f3a", name)
    }

    @Test
    fun `AirPlay audio service name drops the MAC address prefix`() {
        val name = friendlyMdnsName(DeviceProtocol.AIRPLAY, "5855CA1AE288@Living Room", emptyMap())

        assertEquals("Living Room", name)
    }

    @Test
    fun `AirPlay name without prefix is unchanged`() {
        val name = friendlyMdnsName(DeviceProtocol.AIRPLAY, "Living Room", emptyMap())

        assertEquals("Living Room", name)
    }

    @Test
    fun `other protocols keep the service name`() {
        val name = friendlyMdnsName(DeviceProtocol.ARIACAST, "Study Speaker", mapOf("fn" to "ignored"))

        assertEquals("Study Speaker", name)
    }
}
