package net.biodiscus.streammusic.core.discovery.mdns

import net.biodiscus.streammusic.core.model.DeviceProtocol
import net.biodiscus.streammusic.core.model.RaopServiceInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun `reads the encryption types of an AirPlay receiver`() {
        val info = parseRaopServiceInfo(mapOf("et" to "0,1,3"))

        assertEquals(setOf(0, 1, 3), info.encryptionTypes)
    }

    @Test
    fun `an AirPlay receiver that names no encryption accepts none`() {
        assertEquals(setOf(0), parseRaopServiceInfo(emptyMap()).encryptionTypes)
    }

    @Test
    fun `ignores encryption types that are not numbers`() {
        assertEquals(setOf(1), parseRaopServiceInfo(mapOf("et" to "x, 1")).encryptionTypes)
    }

    @Test
    fun `knows that an AirPlay receiver asks for a password`() {
        assertEquals(RaopServiceInfo(setOf(0), needsPassword = true), parseRaopServiceInfo(mapOf("pw" to "true")))
    }

    @Test
    fun `an AirPlay receiver does not ask for a password by default`() {
        assertFalse(parseRaopServiceInfo(emptyMap()).needsPassword)
    }

    @Test
    fun `browses AirPlay through the audio service only`() {
        val airPlayServices = MDNS_SERVICE_KINDS.filter { kind -> kind.protocol == DeviceProtocol.AIRPLAY }

        assertEquals(listOf("_raop._tcp"), airPlayServices.map { kind -> kind.serviceType })
    }

    @Test
    fun `reads features written as two numbers`() {
        assertEquals(0xBC354BD0_4A7FCA00UL.toLong(), parseAirPlayFeatures("0x4A7FCA00,0xBC354BD0"))
    }

    @Test
    fun `reads features written as one number`() {
        assertEquals(0x1234L, parseAirPlayFeatures("0x1234"))
    }

    @Test
    fun `no features are known when the record is missing or broken`() {
        assertEquals(0L, parseAirPlayFeatures(null))
        assertEquals(0L, parseAirPlayFeatures("nonsense"))
    }

    @Test
    fun `a receiver with system pairing pairs without a code`() {
        val bit43 = "0x0,0x800"

        assertTrue(parseRaopServiceInfo(mapOf("ft" to bit43)).supportsTransientPairing)
    }

    @Test
    fun `a receiver with core utils pairing pairs without a code`() {
        val bit48 = "0x0,0x10000"

        assertTrue(parseRaopServiceInfo(mapOf("ft" to bit48)).supportsTransientPairing)
    }

    @Test
    fun `a receiver with other features does not pair without a code`() {
        assertFalse(parseRaopServiceInfo(mapOf("ft" to "0x4A7FCA00")).supportsTransientPairing)
    }

    @Test
    fun `an AirPlay 2 receiver is listed as AirPlay 2 and as AirPlay`() {
        val protocols = protocolsAnnouncedBy(DeviceProtocol.AIRPLAY, mapOf("ft" to "0x0,0x800"))

        assertEquals(listOf(DeviceProtocol.AIRPLAY_2, DeviceProtocol.AIRPLAY), protocols)
    }

    @Test
    fun `an AirPlay 1 receiver is only listed as AirPlay`() {
        assertEquals(listOf(DeviceProtocol.AIRPLAY), protocolsAnnouncedBy(DeviceProtocol.AIRPLAY, mapOf("ft" to "0x4A7FCA00")))
    }

    @Test
    fun `other protocols are listed as themselves`() {
        assertEquals(listOf(DeviceProtocol.CHROMECAST), protocolsAnnouncedBy(DeviceProtocol.CHROMECAST, emptyMap()))
    }

    @Test
    fun `AirPlay 2 names drop the MAC address prefix too`() {
        assertEquals("Kitchen", friendlyMdnsName(DeviceProtocol.AIRPLAY_2, "5855CA1AE288@Kitchen", emptyMap()))
    }
}
