package net.biodiscus.streammusic.core.streaming.device.cast

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CastPayloadsTest {

    private val receiverStatusWithMediaPlayer = """
        {"type":"RECEIVER_STATUS","status":{"applications":[
          {"appId":"CC1AD845","transportId":"web-5","sessionId":"abc-123"}]}}
    """.trimIndent()

    @Test
    fun `launch payload names the app and the request`() {
        val json = JSONObject(launchPayload("CC1AD845", requestId = 7))

        assertEquals("LAUNCH", json.getString("type"))
        assertEquals("CC1AD845", json.getString("appId"))
        assertEquals(7, json.getInt("requestId"))
    }

    @Test
    fun `load payload describes a live audio stream that starts automatically`() {
        val json = JSONObject(loadLiveAudioPayload("http://10.0.0.5:4000/stream.aac", "audio/aac", requestId = 2))

        assertEquals("LOAD", json.getString("type"))
        assertTrue(json.getBoolean("autoplay"))
        assertEquals("LIVE", json.getJSONObject("media").getString("streamType"))
        assertEquals("http://10.0.0.5:4000/stream.aac", json.getJSONObject("media").getString("contentId"))
        assertEquals("audio/aac", json.getJSONObject("media").getString("contentType"))
    }

    @Test
    fun `stop payload names the session`() {
        val json = JSONObject(stopPayload("abc-123", requestId = 3))

        assertEquals("STOP", json.getString("type"))
        assertEquals("abc-123", json.getString("sessionId"))
    }

    @Test
    fun `reads the message type`() {
        assertEquals("PING", payloadType(pingPayload()))
    }

    @Test
    fun `finds the launched app in a receiver status`() {
        val app = findLaunchedApp(receiverStatusWithMediaPlayer, "CC1AD845")

        assertEquals(LaunchedCastApp(transportId = "web-5", sessionId = "abc-123"), app)
    }

    @Test
    fun `finds no app when another app is running`() {
        assertNull(findLaunchedApp(receiverStatusWithMediaPlayer, "OTHER000"))
    }

    @Test
    fun `finds no app when the receiver status has no applications`() {
        assertNull(findLaunchedApp("""{"type":"RECEIVER_STATUS","status":{"volume":{}}}""", "CC1AD845"))
    }

    @Test
    fun `an empty media status is not a player status`() {
        assertFalse(isPlayerStatus("""{"type":"MEDIA_STATUS","status":[]}"""))
    }

    @Test
    fun `a media status with a player is a player status`() {
        assertTrue(isPlayerStatus("""{"type":"MEDIA_STATUS","status":[{"playerState":"BUFFERING"}]}"""))
    }

    @Test
    fun `an idle player reports its idle reason`() {
        val payload = """{"type":"MEDIA_STATUS","status":[{"playerState":"IDLE","idleReason":"ERROR"}]}"""

        assertEquals("ERROR", findIdleReason(payload))
    }

    @Test
    fun `a playing player has no idle reason`() {
        val payload = """{"type":"MEDIA_STATUS","status":[{"playerState":"PLAYING"}]}"""

        assertNull(findIdleReason(payload))
    }

    @Test
    fun `reads that the Chromecast is muted`() {
        val payload = """{"type":"RECEIVER_STATUS","status":{"volume":{"level":0.4,"muted":true}}}"""

        assertEquals(true, findMuted(payload))
    }

    @Test
    fun `reads that the Chromecast is not muted`() {
        val payload = """{"type":"RECEIVER_STATUS","status":{"volume":{"level":0.4,"muted":false}}}"""

        assertEquals(false, findMuted(payload))
    }

    @Test
    fun `a receiver status without a volume says nothing about muting`() {
        val payload = """{"type":"RECEIVER_STATUS","status":{"applications":[]}}"""

        assertNull(findMuted(payload))
    }

    @Test
    fun `a message that is not a receiver status says nothing about muting`() {
        val payload = """{"type":"MEDIA_STATUS","status":[{"playerState":"PLAYING"}]}"""

        assertNull(findMuted(payload))
    }
}
