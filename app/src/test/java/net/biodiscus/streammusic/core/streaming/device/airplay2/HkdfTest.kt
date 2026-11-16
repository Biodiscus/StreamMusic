package net.biodiscus.streammusic.core.streaming.device.airplay2

import net.biodiscus.streammusic.core.streaming.device.airplay.bytes
import net.biodiscus.streammusic.core.streaming.device.airplay.hex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class HkdfTest {

    private val secret = bytes("000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f202122232425262728292a2b2c2d2e2f303132333435363738393a3b3c3d3e3f")

    @Test
    fun `derives the key the phone writes the encrypted connection with`() {
        val key = deriveKey("Control-Salt", "Control-Write-Encryption-Key", secret)

        assertEquals("5a6cb19bcbe7d4df2dd8279f39562f7fae2dbf73eb5a4f98849c245c82b2fe96", hex(key))
    }

    @Test
    fun `derives the key the phone reads the encrypted connection with`() {
        val key = deriveKey("Control-Salt", "Control-Read-Encryption-Key", secret)

        assertEquals("c2f56f1912e2fc1f6604fd5bccab619272f687345aa4bce1c3c857421bd3b821", hex(key))
    }

    @Test
    fun `derives the key that encrypts the audio`() {
        val key = deriveKey("Events-Salt", "Events-Write-Encryption-Key", secret)

        assertEquals("4ab337f2edc98d6a5dcdebc107b1aa174f3b24908a3780294fc94dc6983ee353", hex(key))
    }

    @Test
    fun `other info gives another key`() {
        val write = deriveKey("Control-Salt", "Control-Write-Encryption-Key", secret)
        val read = deriveKey("Control-Salt", "Control-Read-Encryption-Key", secret)

        assertFalse(write.contentEquals(read))
    }
}
