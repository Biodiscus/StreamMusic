package net.biodiscus.streammusic.core.settings

import net.biodiscus.streammusic.core.model.DeviceProtocol
import org.junit.Assert.assertEquals
import org.junit.Test

class DisabledProtocolsPreferenceTest {

    @Test
    fun `decodes what was encoded`() {
        val protocols = setOf(DeviceProtocol.AIRPLAY, DeviceProtocol.SONOS)

        assertEquals(protocols, decodeDisabledProtocols(encodeDisabledProtocols(protocols)))
    }

    @Test
    fun `drops protocol names that no longer exist`() {
        val decoded = decodeDisabledProtocols(setOf("SONOS", "REMOVED_PROTOCOL"))

        assertEquals(setOf(DeviceProtocol.SONOS), decoded)
    }
}
