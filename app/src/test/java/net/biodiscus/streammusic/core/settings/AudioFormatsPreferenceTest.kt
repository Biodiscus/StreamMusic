package net.biodiscus.streammusic.core.settings

import net.biodiscus.streammusic.core.model.ProtocolFamily
import net.biodiscus.streammusic.core.model.StreamAudioFormat
import org.junit.Assert.assertEquals
import org.junit.Test

class AudioFormatsPreferenceTest {

    @Test
    fun `decodes what was encoded`() {
        val formats = mapOf(ProtocolFamily.DLNA to StreamAudioFormat.LPCM, ProtocolFamily.SONOS to StreamAudioFormat.WAV)

        assertEquals(formats, decodeAudioFormats(encodeAudioFormats(formats)))
    }

    @Test
    fun `drops an entry with a family that no longer exists`() {
        val decoded = decodeAudioFormats(setOf("REMOVED_FAMILY:WAV", "SONOS:WAV"))

        assertEquals(mapOf(ProtocolFamily.SONOS to StreamAudioFormat.WAV), decoded)
    }

    @Test
    fun `drops an entry with a format that no longer exists`() {
        assertEquals(emptyMap<ProtocolFamily, StreamAudioFormat>(), decodeAudioFormats(setOf("SONOS:MP3")))
    }

    @Test
    fun `drops an entry without a format`() {
        assertEquals(emptyMap<ProtocolFamily, StreamAudioFormat>(), decodeAudioFormats(setOf("SONOS")))
    }
}
