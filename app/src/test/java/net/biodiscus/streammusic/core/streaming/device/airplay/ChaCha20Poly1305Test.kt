package net.biodiscus.streammusic.core.streaming.device.airplay

import java.security.GeneralSecurityException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class ChaCha20Poly1305Test {

    @Test
    fun `seals the example of RFC 8439`() {
        val sealed = sealWithChaCha20Poly1305(bytes("808182838485868788898a8b8c8d8e8f909192939495969798999a9b9c9d9e9f"), bytes("070000004041424344454647"), bytes("4c616469657320616e642047656e746c656d656e206f662074686520636c617373206f66202739393a204966204920636f756c64206f6666657220796f75206f6e6c79206f6e652074697020666f7220746865206675747572652c2073756e73637265656e20776f756c642062652069742e"), bytes("50515253c0c1c2c3c4c5c6c7"))

        assertEquals("d31a8d34648e60db7b86afbc53ef7ec2a4aded51296e08fea9e2b5a736ee62d63dbea45e8ca9671282fafb69da92728b1a71de0a9e060b2905d6a5b67ecd3b3692ddbd7f2d778b8c9803aee328091b58fab324e4fad675945585808b4831d7bc3ff4def08e4b7a9de576d26586cec64b61161ae10b594f09e26a7e902ecbd0600691", hex(sealed))
    }

    @Test
    fun `seals like the cryptography library for a nonce made from a counter`() {
        val sealed = sealWithChaCha20Poly1305(bytes("000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f"), nonceFromCounter(7), bytes("000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f202122232425262728292a2b2c2d2e2f303132333435363738393a3b3c3d3e3f404142434445464748494a4b4c4d4e4f505152535455565758595a5b5c5d5e5f60616263"), bytes("0102030405060708"))

        assertEquals("f10e864b26ed7c8e6a3f4438f0851e95228cb052e5f81f12b28329fcfbf4c903300cb43aeec2d53e91316bc378c95435e45615dd3d42ee13c75a369bd3680a3940dbf57863e4d02bee972a8c7d1b3829fa372083b9005ce027692d5c810f5e49a3dd7a827edcd546c87f368e282a582b0b848ce4", hex(sealed))
    }

    @Test
    fun `opens what it sealed`() {
        val key = bytes("000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f")
        val sealed = sealWithChaCha20Poly1305(key, nonceFromCounter(1), "audio".toByteArray(), "header".toByteArray())

        val opened = openWithChaCha20Poly1305(key, nonceFromCounter(1), sealed, "header".toByteArray())

        assertEquals("audio", String(opened))
    }

    @Test
    fun `opens what the cryptography library sealed`() {
        val opened = openWithChaCha20Poly1305(bytes("000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f"), bytes("000000000700000000000000"), bytes("f10e864b26ed7c8e6a3f4438f0851e95228cb052e5f81f12b28329fcfbf4c903300cb43aeec2d53e91316bc378c95435e45615dd3d42ee13c75a369bd3680a3940dbf57863e4d02bee972a8c7d1b3829fa372083b9005ce027692d5c810f5e49a3dd7a827edcd546c87f368e282a582b0b848ce4"), bytes("0102030405060708"))

        assertEquals("000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f202122232425262728292a2b2c2d2e2f303132333435363738393a3b3c3d3e3f404142434445464748494a4b4c4d4e4f505152535455565758595a5b5c5d5e5f60616263", hex(opened))
    }

    @Test
    fun `seals an empty message to just a tag`() {
        val sealed = sealWithChaCha20Poly1305(bytes("000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f"), nonceFromCounter(0), ByteArray(0))

        assertEquals(AUTHENTICATION_TAG_SIZE, sealed.size)
    }

    @Test
    fun `refuses a message that was changed`() {
        val sealed = sealWithChaCha20Poly1305(bytes("000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f"), nonceFromCounter(0), "audio".toByteArray())
        sealed[0] = (sealed[0].toInt() xor 1).toByte()

        assertRefused { openWithChaCha20Poly1305(bytes("000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f"), nonceFromCounter(0), sealed) }
    }

    @Test
    fun `refuses a message with other additional data`() {
        val sealed = sealWithChaCha20Poly1305(bytes("000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f"), nonceFromCounter(0), "audio".toByteArray(), byteArrayOf(1))

        assertRefused { openWithChaCha20Poly1305(bytes("000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f"), nonceFromCounter(0), sealed, byteArrayOf(2)) }
    }

    @Test
    fun `refuses a message that is shorter than a tag`() {
        assertRefused { openWithChaCha20Poly1305(bytes("000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f"), nonceFromCounter(0), ByteArray(5)) }
    }

    @Test
    fun `a nonce is four zero bytes and the counter, least significant byte first`() {
        assertArrayEquals(bytes("000000000100000000000000").also { it[4] = 0x01 }, nonceFromCounter(1))
        assertEquals("00000000" + "0201000000000000", hex(nonceFromCounter(0x0102)))
    }

    private fun assertRefused(block: () -> Unit) {
        try {
            block()
            fail("Expected a GeneralSecurityException")
        } catch (expected: GeneralSecurityException) {
            // The message was not accepted.
        }
    }
}

internal fun bytes(hex: String): ByteArray = ByteArray(hex.length / 2) { index -> hex.substring(index * 2, index * 2 + 2).toInt(16).toByte() }

internal fun hex(bytes: ByteArray): String = bytes.joinToString("") { byte -> "%02x".format(byte) }
