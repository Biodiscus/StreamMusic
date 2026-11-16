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
    fun `disabling AirPlay leaves AirPlay 2 enabled`() {
        val settings = AppSettings().withFamilyEnabled(ProtocolFamily.AIRPLAY, isEnabled = false)

        assertEquals(setOf(DeviceProtocol.AIRPLAY), settings.disabledProtocols)
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
}
