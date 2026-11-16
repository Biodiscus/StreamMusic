package net.biodiscus.streammusic.core.streaming.device.airplay2

import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom

private const val SRP_USER_NAME = "Pair-Setup"
private const val PRIVATE_KEY_SIZE = 32
private val GENERATOR = BigInteger.valueOf(5)

/** The 3072 bit group of RFC 5054, which HomeKit pairing uses. */
private val PRIME = BigInteger(
    1,
    hexToBytes(
        "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD129024E088A67CC74" +
        "020BBEA63B139B22514A08798E3404DDEF9519B3CD3A431B302B0A6DF25F1437" +
        "4FE1356D6D51C245E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7ED" +
        "EE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3DC2007CB8A163BF05" +
        "98DA48361C55D39A69163FA8FD24CF5F83655D23DCA3AD961C62F356208552BB" +
        "9ED529077096966D670C354E4ABC9804F1746C08CA18217C32905E462E36CE3B" +
        "E39E772C180E86039B2783A2EC07A28FB5C55DF06F4C52C9DE2BCBF695581718" +
        "3995497CEA956AE515D2261898FA051015728E5A8AAAC42DAD33170D04507A33" +
        "A85521ABDF1CBA64ECFB850458DBEF0A8AEA71575D060C7DB3970F85A6E1E4C7" +
        "ABF5AE8CDB0933D71E8C94E04A25619DCEE3D2261AD2EE6BF12FFA06D98A0864" +
        "D87602733EC86A64521F2B18177B200CBBE117577A615D6C770988C0BAD946E2" +
        "08E24FA074E5AB3143DB5BFCE0FD108E4B82D120A93AD2CAFFFFFFFFFFFFFFFF",
    ),
)

/** What the receiver needs to see to accept the pin, and the secret both sides now share. */
internal class SrpAnswer(val proof: ByteArray, val sessionKey: ByteArray)

/**
 * The phone's side of the Secure Remote Password exchange that HomeKit pairing uses (SRP-6a with SHA-512).
 * The phone proves it knows the pin without sending it, and both sides end up with the same [SrpAnswer.sessionKey].
 */
internal class HomeKitSrpClient(
    private val pin: String,
    privateKey: ByteArray = randomPrivateKey(),
) {
    private val privateNumber = BigInteger(1, privateKey)
    private val publicNumber = GENERATOR.modPow(privateNumber, PRIME)

    /** The public key to send to the receiver. */
    val publicKey: ByteArray = publicNumber.toUnsignedBytes()

    fun answerChallenge(serverPublicKey: ByteArray, salt: ByteArray): SrpAnswer {
        val serverPublicNumber = BigInteger(1, serverPublicKey)
        require(serverPublicNumber.mod(PRIME).signum() != 0) { "The receiver sent an invalid public key" }

        val sharedSecret = computeSharedSecret(serverPublicNumber, salt)
        val sessionKey = sha512(sharedSecret.toUnsignedBytes())
        val proof = computeProof(serverPublicNumber, salt, sessionKey)
        return SrpAnswer(proof, sessionKey)
    }

    /** S = (B - k * g^x) ^ (a + u * x) mod N */
    private fun computeSharedSecret(serverPublicNumber: BigInteger, salt: ByteArray): BigInteger {
        val multiplier = BigInteger(1, sha512(PRIME.toPaddedBytes(), GENERATOR.toPaddedBytes()))
        val scrambler = BigInteger(1, sha512(publicNumber.toPaddedBytes(), serverPublicNumber.toPaddedBytes()))
        val passwordHash = passwordHash(salt)
        val verifier = GENERATOR.modPow(passwordHash, PRIME)
        val base = (serverPublicNumber - multiplier * verifier).mod(PRIME)
        return base.modPow(privateNumber + scrambler * passwordHash, PRIME)
    }

    /** x = H(salt | H(user ":" pin)) */
    private fun passwordHash(salt: ByteArray): BigInteger {
        val userAndPinHash = sha512("$SRP_USER_NAME:$pin".toByteArray())
        return BigInteger(1, sha512(salt, userAndPinHash))
    }

    /** M1 = H(H(N) xor H(g) | H(user) | salt | A | B | K) */
    private fun computeProof(serverPublicNumber: BigInteger, salt: ByteArray, sessionKey: ByteArray): ByteArray {
        val groupHash = BigInteger(1, sha512(PRIME.toUnsignedBytes())).xor(BigInteger(1, sha512(GENERATOR.toUnsignedBytes())))
        return sha512(
            groupHash.toUnsignedBytes(),
            sha512(SRP_USER_NAME.toByteArray()),
            salt,
            publicNumber.toUnsignedBytes(),
            serverPublicNumber.toUnsignedBytes(),
            sessionKey,
        )
    }
}

private fun randomPrivateKey(): ByteArray = ByteArray(PRIVATE_KEY_SIZE).also(SecureRandom()::nextBytes)

private fun sha512(vararg parts: ByteArray): ByteArray {
    val digest = MessageDigest.getInstance("SHA-512")
    parts.forEach(digest::update)
    return digest.digest()
}

/** The number without a sign byte and without leading zeros, as the receiver hashes it. */
private fun BigInteger.toUnsignedBytes(): ByteArray {
    val bytes = toByteArray()
    return if (bytes.size > 1 && bytes[0].toInt() == 0) bytes.copyOfRange(1, bytes.size) else bytes
}

/** The number with leading zeros up to the size of the prime, as it is used in the scrambler and multiplier. */
private fun BigInteger.toPaddedBytes(): ByteArray {
    val unsigned = toUnsignedBytes()
    val primeSize = PRIME_SIZE_BYTES
    return ByteArray(primeSize - unsigned.size) + unsigned
}

private const val PRIME_SIZE_BYTES = 384

private fun hexToBytes(hex: String): ByteArray = ByteArray(hex.length / 2) { index -> hex.substring(index * 2, index * 2 + 2).toInt(16).toByte() }
