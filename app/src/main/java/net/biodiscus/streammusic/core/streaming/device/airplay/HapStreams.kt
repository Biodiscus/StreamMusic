package net.biodiscus.streammusic.core.streaming.device.airplay

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.GeneralSecurityException

/** AirPlay 2 encrypts its connections in frames of at most 1024 bytes, each with its own length and tag. */
private const val LARGEST_FRAME_SIZE = 1024
private const val FRAME_LENGTH_SIZE = 2

/**
 * Encrypts everything written to it. Each frame is the length of its content (2 bytes, little-endian),
 * the encrypted content, and a tag. The length is part of what the tag protects. Every frame uses the next nonce.
 */
internal class HapOutputStream(private val destination: OutputStream, private val key: ByteArray) : OutputStream() {
    private var frameCounter = 0L

    override fun write(b: Int) = write(byteArrayOf(b.toByte()))

    override fun write(bytes: ByteArray, offset: Int, length: Int) {
        val end = offset + length
        for (frameStart in offset until end step LARGEST_FRAME_SIZE) {
            val frameEnd = minOf(frameStart + LARGEST_FRAME_SIZE, end)
            destination.write(encryptFrame(bytes.copyOfRange(frameStart, frameEnd)))
        }
    }

    override fun flush() = destination.flush()

    override fun close() = destination.close()

    private fun encryptFrame(content: ByteArray): ByteArray {
        val lengthBytes = byteArrayOf(content.size.toByte(), (content.size shr 8).toByte())
        val sealed = sealWithChaCha20Poly1305(key, nonceFromCounter(frameCounter++), content, additionalData = lengthBytes)
        return lengthBytes + sealed
    }
}

/** Reads and decrypts the frames the other side wrote with a [HapOutputStream]. */
internal class HapInputStream(private val source: InputStream, private val key: ByteArray) : InputStream() {
    private var frameCounter = 0L
    private var decryptedContent = ByteArray(0)
    private var positionInContent = 0

    override fun read(): Int {
        if (!hasContent()) return -1
        return decryptedContent[positionInContent++].toInt() and 0xFF
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (!hasContent()) return -1
        val count = minOf(length, decryptedContent.size - positionInContent)
        decryptedContent.copyInto(buffer, offset, positionInContent, positionInContent + count)
        positionInContent += count
        return count
    }

    override fun close() = source.close()

    /** Waits for frames until there is something to read. Returns false when the connection ended between frames. */
    private fun hasContent(): Boolean {
        while (positionInContent >= decryptedContent.size) {
            if (!readNextFrame()) return false
        }
        return true
    }

    private fun readNextFrame(): Boolean {
        val lengthBytes = readFully(FRAME_LENGTH_SIZE) ?: return false
        val contentLength = (lengthBytes[0].toInt() and 0xFF) or ((lengthBytes[1].toInt() and 0xFF) shl 8)
        val sealed = readFully(contentLength + AUTHENTICATION_TAG_SIZE) ?: throw IOException("The connection ended inside a frame")
        try {
            decryptedContent = openWithChaCha20Poly1305(key, nonceFromCounter(frameCounter++), sealed, additionalData = lengthBytes)
        } catch (error: GeneralSecurityException) {
            throw IOException("Could not decrypt what the device sent", error)
        }
        positionInContent = 0
        return true
    }

    /** Returns null when the connection ended before the first byte, and fails when it ended after it. */
    private fun readFully(size: Int): ByteArray? {
        val bytes = ByteArray(size)
        var filled = 0
        while (filled < size) {
            val count = source.read(bytes, filled, size - filled)
            if (count == -1) {
                if (filled == 0) return null
                throw IOException("The connection ended inside a frame")
            }
            filled += count
        }
        return bytes
    }
}
