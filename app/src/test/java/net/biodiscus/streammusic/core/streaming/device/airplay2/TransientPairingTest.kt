package net.biodiscus.streammusic.core.streaming.device.airplay2

import java.io.IOException
import kotlinx.coroutines.test.runTest
import net.biodiscus.streammusic.core.streaming.device.airplay.FakeRaopConnection
import net.biodiscus.streammusic.core.streaming.device.airplay.RtspResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class TransientPairingTest {

    private val connection = FakeRaopConnection()
    private val receiver = FakeAirPlay2Receiver()

    init {
        connection.answerToRtsp = receiver::answer
    }

    @Test
    fun `asks the receiver to start pairing before the first step`() = runTest {
        TransientPairing(connection).pair()

        assertEquals(listOf("POST /pair-pin-start", "POST /pair-setup", "POST /pair-setup"), receiver.methodsAndUrls)
    }

    @Test
    fun `speaks HTTP while pairing`() = runTest {
        TransientPairing(connection).pair()

        assertTrue(receiver.requests.all { request -> request.protocol == "HTTP/1.1" })
    }

    @Test
    fun `the first step asks for transient pairing`() = runTest {
        TransientPairing(connection).pair()

        val firstStep = decodeTlv8(receiver.requests[1].body ?: ByteArray(0))
        assertEquals(listOf<Byte>(0), firstStep.getValue(TlvTag.METHOD).toList())
        assertEquals(listOf<Byte>(1), firstStep.getValue(TlvTag.SEQUENCE_NUMBER).toList())
        assertEquals(listOf<Byte>(0x10), firstStep.getValue(TlvTag.FLAGS).toList())
    }

    @Test
    fun `the third step sends the public key and a proof of 64 bytes`() = runTest {
        TransientPairing(connection).pair()

        val thirdStep = decodeTlv8(receiver.requests[2].body ?: ByteArray(0))
        assertEquals(listOf<Byte>(3), thirdStep.getValue(TlvTag.SEQUENCE_NUMBER).toList())
        assertTrue(thirdStep.getValue(TlvTag.PUBLIC_KEY).size in 380..384)
        assertEquals(64, thirdStep.getValue(TlvTag.PROOF).size)
    }

    @Test
    fun `returns a secret of 64 bytes`() = runTest {
        val secret = TransientPairing(connection).pair()

        assertEquals(64, secret.size)
    }

    @Test
    fun `fails when the receiver answers with an error`() = runTest {
        connection.answerToRtsp = { request ->
            if (request.url == "/pair-setup") {
                RtspResponse(200, "OK", emptyMap(), encodeTlv8(listOf(TlvTag.ERROR to byteArrayOf(2))))
            } else {
                RtspResponse(200, "OK", emptyMap())
            }
        }

        assertFailsWith("The device refused to pair (error 2)")
    }

    @Test
    fun `fails when the receiver refuses the request`() = runTest {
        connection.answerToRtsp = { request ->
            if (request.url == "/pair-setup") RtspResponse(470, "Connection Authorization Required", emptyMap()) else RtspResponse(200, "OK", emptyMap())
        }

        assertFailsWith("The device refused to pair: 470 Connection Authorization Required")
    }

    @Test
    fun `fails when the receiver sends no public key`() = runTest {
        connection.answerToRtsp = { RtspResponse(200, "OK", emptyMap(), encodeTlv8(listOf(TlvTag.SEQUENCE_NUMBER to byteArrayOf(2)))) }

        assertFailsWith("The device sent no public key while pairing")
    }

    @Test
    fun `fails when the receiver refuses the proof`() = runTest {
        var pairSetupMessages = 0
        connection.answerToRtsp = { request ->
            if (request.url == "/pair-setup" && ++pairSetupMessages == 2) {
                RtspResponse(200, "OK", emptyMap(), encodeTlv8(listOf(TlvTag.ERROR to byteArrayOf(2))))
            } else {
                receiver.answer(request)
            }
        }

        assertFailsWith("The device refused to pair (error 2)")
    }

    private suspend fun assertFailsWith(message: String) {
        try {
            TransientPairing(connection).pair()
            fail("Expected an IOException")
        } catch (expected: IOException) {
            assertEquals(message, expected.message)
        }
    }
}
