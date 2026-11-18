package net.biodiscus.streammusic.core.model

import net.biodiscus.streammusic.testing.aDevice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsTest {

    @Test
    fun `every protocol family is enabled by default`() {
        val settings = AppSettings()

        assertTrue(ProtocolFamily.entries.all { family -> settings.isEnabled(family) })
    }

    @Test
    fun `disabling a family disables its protocol`() {
        val settings = AppSettings().withFamilyEnabled(ProtocolFamily.SONOS, isEnabled = false)

        assertEquals(setOf(DeviceProtocol.SONOS), settings.disabledProtocols)
    }

    @Test
    fun `disabling AirPlay disables both AirPlay generations`() {
        val settings = AppSettings().withFamilyEnabled(ProtocolFamily.AIRPLAY, isEnabled = false)

        assertEquals(setOf(DeviceProtocol.AIRPLAY, DeviceProtocol.AIRPLAY_2), settings.disabledProtocols)
    }

    @Test
    fun `AirPlay 2 is used where supported by default`() {
        assertTrue(AppSettings().usesAirPlay2WhereSupported)
    }

    @Test
    fun `drops AirPlay 1 of a receiver that also speaks AirPlay 2`() {
        val airPlay2 = aDevice(protocol = DeviceProtocol.AIRPLAY_2, host = "10.0.0.5")
        val airPlay1 = aDevice(protocol = DeviceProtocol.AIRPLAY, host = "10.0.0.5")

        val devices = listOf(airPlay2, airPlay1).withoutDisabledProtocols(AppSettings(usesAirPlay2WhereSupported = true))

        assertEquals(listOf(airPlay2), devices)
    }

    @Test
    fun `keeps a receiver that only speaks AirPlay 1`() {
        val airPlay1 = aDevice(protocol = DeviceProtocol.AIRPLAY, host = "10.0.0.6")
        val airPlay2OfOtherReceiver = aDevice(protocol = DeviceProtocol.AIRPLAY_2, host = "10.0.0.5")

        val devices = listOf(airPlay1, airPlay2OfOtherReceiver).withoutDisabledProtocols(AppSettings(usesAirPlay2WhereSupported = true))

        assertEquals(listOf(airPlay1, airPlay2OfOtherReceiver), devices)
    }

    @Test
    fun `keeps AirPlay 1 next to AirPlay 2 when AirPlay 2 is not required`() {
        val airPlay2 = aDevice(protocol = DeviceProtocol.AIRPLAY_2, host = "10.0.0.5")
        val airPlay1 = aDevice(protocol = DeviceProtocol.AIRPLAY, host = "10.0.0.5")

        val devices = listOf(airPlay2, airPlay1).withoutDisabledProtocols(AppSettings(usesAirPlay2WhereSupported = false))

        assertEquals(listOf(airPlay2, airPlay1), devices)
    }

    @Test
    fun `a disabled family is not enabled`() {
        val settings = AppSettings().withFamilyEnabled(ProtocolFamily.HEOS, isEnabled = false)

        assertFalse(settings.isEnabled(ProtocolFamily.HEOS))
    }

    @Test
    fun `enabling a family again removes its protocols from the disabled ones`() {
        val settings = AppSettings()
            .withFamilyEnabled(ProtocolFamily.AIRPLAY, isEnabled = false)
            .withFamilyEnabled(ProtocolFamily.AIRPLAY, isEnabled = true)

        assertEquals(emptySet<DeviceProtocol>(), settings.disabledProtocols)
    }

    @Test
    fun `disabling one family keeps the others enabled`() {
        val settings = AppSettings().withFamilyEnabled(ProtocolFamily.SONOS, isEnabled = false)

        assertTrue(settings.isEnabled(ProtocolFamily.DLNA))
    }

    @Test
    fun `leaves out the devices of disabled protocols`() {
        val sonos = aDevice(protocol = DeviceProtocol.SONOS, host = "10.0.0.2")
        val dlna = aDevice(protocol = DeviceProtocol.DLNA, host = "10.0.0.3")
        val settings = AppSettings().withFamilyEnabled(ProtocolFamily.SONOS, isEnabled = false)

        assertEquals(listOf(dlna), listOf(sonos, dlna).withoutDisabledProtocols(settings))
    }

    @Test
    fun `streams AAC when no format was chosen`() {
        assertEquals(StreamAudioFormat.AAC, AppSettings().audioFormatOf(ProtocolFamily.DLNA))
    }

    @Test
    fun `streams the format that was chosen for a family`() {
        val settings = AppSettings().withAudioFormat(ProtocolFamily.DLNA, StreamAudioFormat.LPCM)

        assertEquals(StreamAudioFormat.LPCM, settings.audioFormatOf(ProtocolFamily.DLNA))
    }

    @Test
    fun `a format chosen for one family does not change another`() {
        val settings = AppSettings().withAudioFormat(ProtocolFamily.DLNA, StreamAudioFormat.LPCM)

        assertEquals(StreamAudioFormat.AAC, settings.audioFormatOf(ProtocolFamily.SONOS))
    }

    @Test
    fun `streams AAC when the chosen format is not offered for the family`() {
        val settings = AppSettings().withAudioFormat(ProtocolFamily.SONOS, StreamAudioFormat.LPCM)

        assertEquals(StreamAudioFormat.AAC, settings.audioFormatOf(ProtocolFamily.SONOS))
    }

    @Test
    fun `finds the format for the protocol of a device`() {
        val settings = AppSettings().withAudioFormat(ProtocolFamily.GOOGLE_CAST, StreamAudioFormat.WAV)

        assertEquals(StreamAudioFormat.WAV, settings.audioFormatFor(DeviceProtocol.CHROMECAST))
    }

    @Test
    fun `a protocol without a format choice streams AAC`() {
        assertEquals(StreamAudioFormat.AAC, AppSettings().audioFormatFor(DeviceProtocol.HEOS))
    }

    @Test
    fun `only families whose devices pull a stream offer a format`() {
        val familiesWithChoice = ProtocolFamily.entries.filter { family -> family.selectableAudioFormats.isNotEmpty() }

        assertEquals(
            setOf(ProtocolFamily.DLNA, ProtocolFamily.GOOGLE_CAST, ProtocolFamily.SAMSUNG_MULTIROOM, ProtocolFamily.SONOS),
            familiesWithChoice.toSet(),
        )
    }

    @Test
    fun `every family that offers a format offers AAC too`() {
        val families = ProtocolFamily.entries.filter { family -> family.selectableAudioFormats.isNotEmpty() }

        assertTrue(families.all { family -> StreamAudioFormat.AAC in family.selectableAudioFormats })
    }

    @Test
    fun `the phone is muted while streaming by default`() {
        assertTrue(AppSettings().mutesPhoneWhileStreaming)
    }

    @Test
    fun `dropped devices are reconnected by default`() {
        assertTrue(AppSettings().reconnectsDroppedDevices)
    }

    @Test
    fun `streaming never stops by itself by default`() {
        assertFalse(AppSettings().stopsStreamingAfterSilence.isOn())
    }

    @Test
    fun `the AAC stream has the standard quality by default`() {
        assertEquals(AacQuality.STANDARD, AppSettings().aacQuality)
    }

    @Test
    fun `a timeout of five minutes is on`() {
        assertTrue(SilenceTimeout.FIVE_MINUTES.isOn())
    }

    @Test
    fun `the system theme follows the system`() {
        assertTrue(ThemeMode.SYSTEM.usesDarkTheme(isSystemDark = true))
        assertFalse(ThemeMode.SYSTEM.usesDarkTheme(isSystemDark = false))
    }

    @Test
    fun `the light theme is light even when the system is dark`() {
        assertFalse(ThemeMode.LIGHT.usesDarkTheme(isSystemDark = true))
    }

    @Test
    fun `the dark theme is dark even when the system is light`() {
        assertTrue(ThemeMode.DARK.usesDarkTheme(isSystemDark = false))
    }
}
