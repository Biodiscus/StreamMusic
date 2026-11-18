package net.biodiscus.streammusic.core.protocolchoices

import net.biodiscus.streammusic.core.model.DeviceProtocol
import org.junit.Assert.assertEquals
import org.junit.Test

class ProtocolChoicesJsonTest {

    @Test
    fun `saved choices are read back unchanged`() {
        val choices = mapOf("10.0.0.2" to DeviceProtocol.SONOS, "10.0.0.3" to DeviceProtocol.CHROMECAST)

        assertEquals(choices, decodeProtocolChoices(encodeProtocolChoices(choices)))
    }

    @Test
    fun `nothing saved means no choices`() {
        assertEquals(emptyMap<String, DeviceProtocol>(), decodeProtocolChoices(null))
    }

    @Test
    fun `text that cannot be read means no choices`() {
        assertEquals(emptyMap<String, DeviceProtocol>(), decodeProtocolChoices("""{"10.0.0.2":"REMOVED"}"""))
    }
}
