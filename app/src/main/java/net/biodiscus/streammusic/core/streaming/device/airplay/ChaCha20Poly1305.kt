package net.biodiscus.streammusic.core.streaming.device.airplay

import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.GeneralSecurityException
import java.security.MessageDigest

internal const val CHACHA_KEY_SIZE = 32
internal const val AUTHENTICATION_TAG_SIZE = 16

private const val NONCE_SIZE = 12
private const val CHACHA_BLOCK_SIZE = 64
private const val POLY1305_BLOCK_SIZE = 16
private val CHACHA_CONSTANTS = intArrayOf(0x61707865, 0x3320646e, 0x79622d32, 0x6b206574)
private val POLY1305_PRIME = BigInteger.ONE.shiftLeft(130) - BigInteger.valueOf(5)
private val POLY1305_CLAMP = BigInteger("0ffffffc0ffffffc0ffffffc0fffffff", 16)
private val TAG_MODULUS = BigInteger.ONE.shiftLeft(128)

/**
 * A nonce of 12 bytes made of four zero bytes and a counter. AirPlay uses a new counter value for every message.
 */
internal fun nonceFromCounter(counter: Long): ByteArray =
    ByteBuffer.allocate(NONCE_SIZE).order(ByteOrder.LITTLE_ENDIAN).putLong(4, counter).array()

/** Encrypts [plaintext] and appends a 16 byte tag that also covers [additionalData]. RFC 8439. */
internal fun sealWithChaCha20Poly1305(
    key: ByteArray,
    nonce: ByteArray,
    plaintext: ByteArray,
    additionalData: ByteArray = ByteArray(0),
): ByteArray {
    val ciphertext = applyChaCha20(key, nonce, plaintext, firstBlockNumber = 1)
    val tag = computeTag(key, nonce, additionalData, ciphertext)
    return ciphertext + tag
}

/** Checks the tag and decrypts. Throws [GeneralSecurityException] when the message was changed or the key is wrong. */
internal fun openWithChaCha20Poly1305(
    key: ByteArray,
    nonce: ByteArray,
    sealed: ByteArray,
    additionalData: ByteArray = ByteArray(0),
): ByteArray {
    if (sealed.size < AUTHENTICATION_TAG_SIZE) throw GeneralSecurityException("Message is shorter than its tag")
    val ciphertext = sealed.copyOfRange(0, sealed.size - AUTHENTICATION_TAG_SIZE)
    val receivedTag = sealed.copyOfRange(sealed.size - AUTHENTICATION_TAG_SIZE, sealed.size)
    val expectedTag = computeTag(key, nonce, additionalData, ciphertext)
    if (!MessageDigest.isEqual(receivedTag, expectedTag)) throw GeneralSecurityException("Wrong authentication tag")
    return applyChaCha20(key, nonce, ciphertext, firstBlockNumber = 1)
}

private fun applyChaCha20(key: ByteArray, nonce: ByteArray, data: ByteArray, firstBlockNumber: Int): ByteArray {
    val output = ByteArray(data.size)
    for (blockIndex in 0 until (data.size + CHACHA_BLOCK_SIZE - 1) / CHACHA_BLOCK_SIZE) {
        val keyStream = chaChaBlock(key, nonce, firstBlockNumber + blockIndex)
        val start = blockIndex * CHACHA_BLOCK_SIZE
        val end = minOf(start + CHACHA_BLOCK_SIZE, data.size)
        for (position in start until end) {
            output[position] = (data[position].toInt() xor keyStream[position - start].toInt()).toByte()
        }
    }
    return output
}

private fun chaChaBlock(key: ByteArray, nonce: ByteArray, blockNumber: Int): ByteArray {
    val initialState = initialChaChaState(key, nonce, blockNumber)
    val state = initialState.copyOf()
    repeat(10) {
        quarterRound(state, 0, 4, 8, 12)
        quarterRound(state, 1, 5, 9, 13)
        quarterRound(state, 2, 6, 10, 14)
        quarterRound(state, 3, 7, 11, 15)
        quarterRound(state, 0, 5, 10, 15)
        quarterRound(state, 1, 6, 11, 12)
        quarterRound(state, 2, 7, 8, 13)
        quarterRound(state, 3, 4, 9, 14)
    }
    val block = ByteBuffer.allocate(CHACHA_BLOCK_SIZE).order(ByteOrder.LITTLE_ENDIAN)
    for (index in state.indices) block.putInt(state[index] + initialState[index])
    return block.array()
}

private fun initialChaChaState(key: ByteArray, nonce: ByteArray, blockNumber: Int): IntArray {
    val keyWords = ByteBuffer.wrap(key).order(ByteOrder.LITTLE_ENDIAN)
    val nonceWords = ByteBuffer.wrap(nonce).order(ByteOrder.LITTLE_ENDIAN)
    val state = IntArray(16)
    CHACHA_CONSTANTS.copyInto(state)
    for (index in 0 until 8) state[4 + index] = keyWords.getInt(index * 4)
    state[12] = blockNumber
    for (index in 0 until 3) state[13 + index] = nonceWords.getInt(index * 4)
    return state
}

private fun quarterRound(state: IntArray, a: Int, b: Int, c: Int, d: Int) {
    state[a] += state[b]
    state[d] = (state[d] xor state[a]).rotateLeft(16)
    state[c] += state[d]
    state[b] = (state[b] xor state[c]).rotateLeft(12)
    state[a] += state[b]
    state[d] = (state[d] xor state[a]).rotateLeft(8)
    state[c] += state[d]
    state[b] = (state[b] xor state[c]).rotateLeft(7)
}

private fun computeTag(key: ByteArray, nonce: ByteArray, additionalData: ByteArray, ciphertext: ByteArray): ByteArray {
    val oneTimeKey = chaChaBlock(key, nonce, blockNumber = 0).copyOf(CHACHA_KEY_SIZE)
    val authenticatedData = additionalData.padToBlock() + ciphertext.padToBlock() +
        littleEndianLong(additionalData.size) + littleEndianLong(ciphertext.size)
    return poly1305(oneTimeKey, authenticatedData)
}

/** Poly1305 works on whole blocks of 16 bytes, so shorter data is filled with zeros. */
private fun ByteArray.padToBlock(): ByteArray {
    val missing = (POLY1305_BLOCK_SIZE - size % POLY1305_BLOCK_SIZE) % POLY1305_BLOCK_SIZE
    return this + ByteArray(missing)
}

private fun littleEndianLong(value: Int): ByteArray =
    ByteBuffer.allocate(Long.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN).putLong(value.toLong()).array()

private fun poly1305(oneTimeKey: ByteArray, message: ByteArray): ByteArray {
    val multiplier = littleEndianNumber(oneTimeKey.copyOfRange(0, 16)).and(POLY1305_CLAMP)
    val addend = littleEndianNumber(oneTimeKey.copyOfRange(16, 32))
    var accumulator = BigInteger.ZERO
    for (start in message.indices step POLY1305_BLOCK_SIZE) {
        val block = message.copyOfRange(start, minOf(start + POLY1305_BLOCK_SIZE, message.size))
        val blockWithEndMarker = littleEndianNumber(block + byteArrayOf(1))
        accumulator = (accumulator + blockWithEndMarker).multiply(multiplier).mod(POLY1305_PRIME)
    }
    val tagNumber = (accumulator + addend).mod(TAG_MODULUS)
    return numberToLittleEndian(tagNumber, AUTHENTICATION_TAG_SIZE)
}

private fun littleEndianNumber(bytes: ByteArray): BigInteger = BigInteger(1, bytes.reversedArray())

private fun numberToLittleEndian(number: BigInteger, size: Int): ByteArray {
    val bigEndian = ByteArray(size)
    val numberBytes = number.toByteArray().let { bytes -> if (bytes.size > size) bytes.copyOfRange(bytes.size - size, bytes.size) else bytes }
    numberBytes.copyInto(bigEndian, destinationOffset = size - numberBytes.size)
    return bigEndian.reversedArray()
}
