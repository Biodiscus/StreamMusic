package net.biodiscus.streammusic.core.streaming.device.airplay

import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class RaopSessionTest {

    @Test
    fun `describes a 44100 Hz Apple Lossless stream`() {
        val description = buildSessionDescription("1234567890", "10.0.0.5", "10.0.0.9", encryption = null)

        assertEquals(
            "v=0\r\n" +
                "o=iTunes 1234567890 0 IN IP4 10.0.0.5\r\n" +
                "s=iTunes\r\n" +
                "c=IN IP4 10.0.0.9\r\n" +
                "t=0 0\r\n" +
                "m=audio 0 RTP/AVP 96\r\n" +
                "a=rtpmap:96 AppleLossless\r\n" +
                "a=fmtp:96 352 0 16 40 10 14 2 255 0 0 44100\r\n",
            description,
        )
    }

    @Test
    fun `adds the encrypted key and the vector without padding when encrypting`() {
        val encryption = RaopEncryptionOffer(encryptedKey = ByteArray(16) { 0xFF.toByte() }, initializationVector = byteArrayOf(0, 0, 0, 0))

        val description = buildSessionDescription("1", "10.0.0.5", "10.0.0.9", encryption)

        assertTrue(description.contains("a=rsaaeskey:" + "/".repeat(21) + "w\r\n"))
        assertTrue(description.contains("a=aesiv:AAAAAA\r\n"))
    }

    @Test
    fun `leaves the key out when not encrypting`() {
        val description = buildSessionDescription("1", "10.0.0.5", "10.0.0.9", encryption = null)

        assertFalse(description.contains("rsaaeskey"))
    }

    @Test
    fun `tells the receiver where to send its control and timing requests`() {
        assertEquals(
            "RTP/AVP/UDP;unicast;interleaved=0-1;mode=record;control_port=50001;timing_port=50002",
            buildTransportRequestHeader(controlPort = 50001, timingPort = 50002),
        )
    }

    @Test
    fun `reads the ports of the receiver`() {
        val ports = parseServerPorts("RTP/AVP/UDP;unicast;mode=record;server_port=6000;control_port=6001;timing_port=6002")

        assertEquals(RaopServerPorts(audioPort = 6000, controlPort = 6001, timingPort = 6002), ports)
    }

    @Test
    fun `the timing port is optional`() {
        val ports = parseServerPorts("RTP/AVP/UDP;unicast;server_port=6000;control_port=6001")

        assertNull(ports.timingPort)
    }

    @Test
    fun `fails when the receiver sends no audio port`() {
        try {
            parseServerPorts("RTP/AVP/UDP;unicast;control_port=6001")
            fail("Expected an IOException")
        } catch (expected: IOException) {
            assertEquals("The device sent no audio port", expected.message)
        }
    }
}
