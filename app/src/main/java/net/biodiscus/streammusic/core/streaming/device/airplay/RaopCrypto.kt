package net.biodiscus.streammusic.core.streaming.device.airplay

import java.math.BigInteger
import java.security.KeyFactory
import java.security.SecureRandom
import java.security.spec.RSAPublicKeySpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

private const val AES_BLOCK_SIZE = 16

/** The public RSA key of the original AirPort Express. Receivers that accept encrypted audio hold its private half. */
private const val RECEIVER_RSA_MODULUS_BASE64 =
    "59dE8qLieItsH1WgjrcFRKj6eUWqi+bGLOX1HL3U3GhC/j0Qg90u3sG/1CUtwC" +
        "5vOYvfDmFI6oSFXi5ELabWJmT2dKHzBJKa3k9ok+8t9ucRqMd6DZHJ2YCCLlDR" +
        "KSKv6kDqnw4UwPdpOMXziC/AMj3Z/lUVX1G7WSHCAWKf1zNS1eLvqr+boEjXuB" +
        "OitnZ/bDzPHrTOZz0Dew0uowxf/+sG+NCK3eQJVxqcaJ/vEHKIVd2M+5qL71yJ" +
        "Q+87X6oV3eaYvt3zWZYD6z5vYTcrtij2VZ9Zmni/UAaHqn9JdsBWLUEpVviYnh" +
        "imNVvYFZeCXg/IdTQ+x4IRdiXNv5hEew=="
private const val RECEIVER_RSA_EXPONENT_BASE64 = "AQAB"

/** The AES key and starting block that encrypt the audio of one session. */
internal class RaopAudioKey(
    val key: ByteArray,
    val initializationVector: ByteArray,
) {
    companion object {
        fun random(): RaopAudioKey {
            val random = SecureRandom()
            val key = ByteArray(AES_BLOCK_SIZE).also(random::nextBytes)
            val initializationVector = ByteArray(AES_BLOCK_SIZE).also(random::nextBytes)
            return RaopAudioKey(key, initializationVector)
        }
    }
}

/** The AES key in a form only the receiver can open, as it is sent in the session description. */
internal fun encryptKeyForReceiver(audioKey: RaopAudioKey): ByteArray {
    val modulus = BigInteger(1, Base64.getDecoder().decode(RECEIVER_RSA_MODULUS_BASE64))
    val exponent = BigInteger(1, Base64.getDecoder().decode(RECEIVER_RSA_EXPONENT_BASE64))
    val receiverPublicKey = KeyFactory.getInstance("RSA").generatePublic(RSAPublicKeySpec(modulus, exponent))
    val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding")
    cipher.init(Cipher.ENCRYPT_MODE, receiverPublicKey)
    return cipher.doFinal(audioKey.key)
}

/**
 * Encrypts the audio of one packet. Every packet starts again from the same initialization vector,
 * and the bytes after the last full block of 16 are sent as they are.
 */
internal fun encryptAudioPayload(payload: ByteArray, audioKey: RaopAudioKey): ByteArray {
    val encryptedLength = payload.size / AES_BLOCK_SIZE * AES_BLOCK_SIZE
    val cipher = Cipher.getInstance("AES/CBC/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(audioKey.key, "AES"), IvParameterSpec(audioKey.initializationVector))
    val encryptedBlocks = cipher.doFinal(payload, 0, encryptedLength)
    return encryptedBlocks + payload.copyOfRange(encryptedLength, payload.size)
}
