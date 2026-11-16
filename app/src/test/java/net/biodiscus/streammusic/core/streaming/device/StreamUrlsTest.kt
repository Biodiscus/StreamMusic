package net.biodiscus.streammusic.core.streaming.device

import net.biodiscus.streammusic.core.model.DeviceProtocol
import org.junit.Assert.assertEquals
import org.junit.Test

class StreamUrlsTest {

    private val httpUrl = "http://192.168.1.5:40000/stream.aac"

    @Test
    fun `Sonos gets the radio scheme`() {
        assertEquals(
            "x-rincon-mp3radio://192.168.1.5:40000/stream.aac",
            playableUrlFor(DeviceProtocol.SONOS, httpUrl),
        )
    }

    @Test
    fun `other protocols keep the http address`() {
        assertEquals(httpUrl, playableUrlFor(DeviceProtocol.DLNA, httpUrl))
    }
}
