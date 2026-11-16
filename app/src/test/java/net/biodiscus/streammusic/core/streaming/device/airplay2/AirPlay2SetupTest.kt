package net.biodiscus.streammusic.core.streaming.device.airplay2

import java.io.IOException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class AirPlay2SetupTest {

    @Test
    fun `the session setup names the time protocol and the timing port`() {
        val body = decodeBinaryPlist(buildSessionSetupBody("SESSION-UUID", timingPort = 50_000)) as Map<*, *>

        assertEquals("NTP", body["timingProtocol"])
        assertEquals(50_000L, body["timingPort"])
        assertEquals("SESSION-UUID", body["sessionUUID"])
    }

    @Test
    fun `the stream setup asks for one real-time stream of 44100 Hz PCM`() {
        val body = decodeBinaryPlist(buildStreamSetupBody(controlPort = 50_001, sharedKey = ByteArray(32), streamConnectionId = 4_000_000_000L))

        val stream = streamOf(body)
        assertEquals(1L, stream["ct"])
        assertEquals(0x800L, stream["audioFormat"])
        assertEquals(352L, stream["spf"])
        assertEquals(44_100L, stream["sr"])
        assertEquals(96L, stream["type"])
    }

    @Test
    fun `the stream setup tells the control port, the key and the connection id`() {
        val key = ByteArray(32) { it.toByte() }

        val stream = streamOf(decodeBinaryPlist(buildStreamSetupBody(50_001, key, 4_000_000_000L)))

        assertEquals(50_001L, stream["controlPort"])
        assertArrayEquals(key, stream["shk"] as ByteArray)
        assertEquals(4_000_000_000L, stream["streamConnectionID"])
    }

    @Test
    fun `the stream setup allows latencies between a quarter and two seconds`() {
        val stream = streamOf(decodeBinaryPlist(buildStreamSetupBody(1, ByteArray(32), 1)))

        assertEquals(11_025L, stream["latencyMin"])
        assertEquals(88_200L, stream["latencyMax"])
    }

    @Test
    fun `reads the event port from the answer to the session setup`() {
        val answer = encodeBinaryPlist(mapOf("eventPort" to 51_234, "timingPort" to 0))

        assertEquals(51_234, parseEventPort(answer))
    }

    @Test
    fun `reads the ports of the stream from the answer to the stream setup`() {
        val answer = encodeBinaryPlist(mapOf("streams" to listOf(mapOf("dataPort" to 6_000, "controlPort" to 6_001))))

        assertEquals(AirPlay2StreamPorts(dataPort = 6_000, controlPort = 6_001), parseStreamPorts(answer))
    }

    @Test
    fun `fails when the answer has no event port`() {
        assertFailsWith("The device sent no event port") { parseEventPort(encodeBinaryPlist(mapOf("other" to 1))) }
    }

    @Test
    fun `fails when the answer has no stream`() {
        assertFailsWith("The device did not set up a stream") { parseStreamPorts(encodeBinaryPlist(mapOf("streams" to emptyList<Any>()))) }
    }

    @Test
    fun `fails when the answer has no audio port`() {
        val answer = encodeBinaryPlist(mapOf("streams" to listOf(mapOf("controlPort" to 6_001))))

        assertFailsWith("The device sent no audio port") { parseStreamPorts(answer) }
    }

    @Test
    fun `fails when the answer is not a property list`() {
        assertFailsWith("Not a binary property list") { parseEventPort("<html>".toByteArray()) }
    }

    private fun streamOf(body: Any?): Map<*, *> = ((body as Map<*, *>)["streams"] as List<*>).single() as Map<*, *>

    private fun assertFailsWith(message: String, block: () -> Unit) {
        try {
            block()
            fail("Expected an IOException")
        } catch (expected: IOException) {
            assertEquals(message, expected.message)
        }
    }
}
