package net.biodiscus.streammusic.core.streaming.device.airplay

import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class RaopCryptoTest {

    private val audioKey = RaopAudioKey(ByteArray(16) { it.toByte() }, ByteArray(16) { (100 + it).toByte() })

    @Test
    fun `encrypts full blocks so that the key and vector decrypt them`() {
        val payload = ByteArray(32) { it.toByte() }

        val encrypted = encryptAudioPayload(payload, audioKey)

        assertArrayEquals(payload, decrypt(encrypted))
        assertFalse(payload.contentEquals(encrypted))
    }

    @Test
    fun `leaves the bytes after the last full block as they are`() {
        val payload = ByteArray(21) { it.toByte() }

        val encrypted = encryptAudioPayload(payload, audioKey)

        assertArrayEquals(payload.copyOfRange(16, 21), encrypted.copyOfRange(16, 21))
    }

    @Test
    fun `keeps the length of the payload`() {
        assertEquals(21, encryptAudioPayload(ByteArray(21), audioKey).size)
    }

    @Test
    fun `starts every packet from the same vector`() {
        val payload = ByteArray(16) { it.toByte() }

        assertArrayEquals(encryptAudioPayload(payload, audioKey), encryptAudioPayload(payload, audioKey))
    }

    @Test
    fun `a payload shorter than one block is not encrypted`() {
        val payload = ByteArray(10) { it.toByte() }

        assertArrayEquals(payload, encryptAudioPayload(payload, audioKey))
    }

    @Test
    fun `the key for the receiver has the size of the 2048 bit receiver key`() {
        assertEquals(256, encryptKeyForReceiver(audioKey).size)
    }

    @Test
    fun `a random key is 16 bytes with a 16 byte vector`() {
        val key = RaopAudioKey.random()

        assertEquals(16, key.key.size)
        assertEquals(16, key.initializationVector.size)
    }

    private fun decrypt(encrypted: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/CBC/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(audioKey.key, "AES"), IvParameterSpec(audioKey.initializationVector))
        return cipher.doFinal(encrypted)
    }
}
