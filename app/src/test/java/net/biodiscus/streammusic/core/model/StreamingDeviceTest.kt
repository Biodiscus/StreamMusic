package net.biodiscus.streammusic.core.model

import net.biodiscus.streammusic.testing.aDevice
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamingDeviceTest {

    private val airPlay = aDevice(protocol = DeviceProtocol.AIRPLAY)

    @Test
    fun `an AirPlay receiver that accepts unencrypted audio can receive a stream`() {
        val receiver = airPlay.copy(raop = RaopServiceInfo(setOf(0), needsPassword = false))

        assertTrue(receiver.canReceiveStream)
    }

    @Test
    fun `an AirPlay receiver that only accepts FairPlay cannot receive a stream`() {
        val receiver = airPlay.copy(raop = RaopServiceInfo(setOf(3), needsPassword = false))

        assertFalse(receiver.canReceiveStream)
    }

    @Test
    fun `an AirPlay receiver with a password cannot receive a stream`() {
        val receiver = airPlay.copy(raop = RaopServiceInfo(setOf(0, 1), needsPassword = true))

        assertFalse(receiver.canReceiveStream)
    }

    @Test
    fun `an AirPlay 2 receiver that pairs without a code can receive a stream`() {
        val receiver = aDevice(protocol = DeviceProtocol.AIRPLAY_2)
            .copy(raop = RaopServiceInfo(setOf(4), needsPassword = false, supportsTransientPairing = true))

        assertTrue(receiver.canReceiveStream)
    }

    @Test
    fun `the AirPlay find of a receiver that only does AirPlay 2 cannot receive a stream`() {
        val receiver = airPlay.copy(raop = RaopServiceInfo(setOf(4), needsPassword = false, supportsTransientPairing = true))

        assertFalse(receiver.canReceiveStream)
    }

    @Test
    fun `an AirPlay 2 find without transient pairing cannot receive a stream`() {
        val receiver = aDevice(protocol = DeviceProtocol.AIRPLAY_2).copy(raop = RaopServiceInfo(setOf(0), needsPassword = false))

        assertFalse(receiver.canReceiveStream)
    }

    @Test
    fun `a protocol that is only listed cannot receive a stream`() {
        assertFalse(aDevice(protocol = DeviceProtocol.FIRE_TV).canReceiveStream)
    }

    @Test
    fun `a Chromecast can receive a stream`() {
        assertTrue(aDevice(protocol = DeviceProtocol.CHROMECAST).canReceiveStream)
    }
}
