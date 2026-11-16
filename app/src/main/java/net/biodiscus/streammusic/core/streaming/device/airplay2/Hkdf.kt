package net.biodiscus.streammusic.core.streaming.device.airplay2

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private const val HMAC_ALGORITHM = "HmacSHA512"
private const val DERIVED_KEY_SIZE = 32

/**
 * Derives a 32 byte key from the pairing secret with HKDF-SHA512 (RFC 5869).
 * A key of at most 64 bytes needs only one expand step, so that is all this does.
 */
internal fun deriveKey(salt: String, info: String, secret: ByteArray): ByteArray {
    val pseudoRandomKey = hmac(key = salt.toByteArray(), data = secret)
    val firstBlock = hmac(key = pseudoRandomKey, data = info.toByteArray() + byteArrayOf(1))
    return firstBlock.copyOf(DERIVED_KEY_SIZE)
}

private fun hmac(key: ByteArray, data: ByteArray): ByteArray {
    val mac = Mac.getInstance(HMAC_ALGORITHM)
    mac.init(SecretKeySpec(key, HMAC_ALGORITHM))
    return mac.doFinal(data)
}
