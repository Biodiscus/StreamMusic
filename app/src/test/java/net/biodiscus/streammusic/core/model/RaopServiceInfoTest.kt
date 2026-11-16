package net.biodiscus.streammusic.core.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RaopServiceInfoTest {

    @Test
    fun `accepts a receiver that takes unencrypted audio`() {
        assertTrue(RaopServiceInfo(setOf(0), needsPassword = false).acceptsAirPlay1Audio)
    }

    @Test
    fun `accepts a receiver that takes RSA encrypted audio`() {
        assertTrue(RaopServiceInfo(setOf(1), needsPassword = false).acceptsAirPlay1Audio)
    }

    @Test
    fun `rejects a receiver that only takes FairPlay or MFi encryption`() {
        assertFalse(RaopServiceInfo(setOf(3, 4), needsPassword = false).acceptsAirPlay1Audio)
    }

    @Test
    fun `rejects a receiver that asks for a password`() {
        assertFalse(RaopServiceInfo(setOf(0, 1), needsPassword = true).acceptsAirPlay1Audio)
    }

    @Test
    fun `encrypts when the receiver accepts RSA encryption`() {
        assertTrue(RaopServiceInfo(setOf(0, 1), needsPassword = false).shouldEncryptAudio)
    }

    @Test
    fun `does not encrypt when the receiver only takes clear audio`() {
        assertFalse(RaopServiceInfo(setOf(0), needsPassword = false).shouldEncryptAudio)
    }

    @Test
    fun `accepts AirPlay 2 audio when the receiver pairs without a code`() {
        val receiver = RaopServiceInfo(setOf(4), needsPassword = false, supportsTransientPairing = true)

        assertTrue(receiver.acceptsAirPlay2Audio)
    }

    @Test
    fun `does not accept AirPlay 2 audio when the receiver needs a code`() {
        assertFalse(RaopServiceInfo(setOf(0), needsPassword = false).acceptsAirPlay2Audio)
    }

    @Test
    fun `does not accept AirPlay 2 audio when the receiver asks for a password`() {
        assertFalse(RaopServiceInfo(setOf(0), needsPassword = true, supportsTransientPairing = true).acceptsAirPlay2Audio)
    }

    @Test
    fun `checks AirPlay 1 and AirPlay 2 separately`() {
        val receiver = RaopServiceInfo(setOf(3), needsPassword = false, supportsTransientPairing = true)

        assertFalse(receiver.canReceiveAudioWith(DeviceProtocol.AIRPLAY))
        assertTrue(receiver.canReceiveAudioWith(DeviceProtocol.AIRPLAY_2))
    }

    @Test
    fun `has nothing to say about other protocols`() {
        assertTrue(RaopServiceInfo(setOf(3), needsPassword = true).canReceiveAudioWith(DeviceProtocol.CHROMECAST))
    }
}
