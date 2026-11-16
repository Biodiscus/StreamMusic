package net.biodiscus.streammusic.core.streaming.device.airplay

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import net.biodiscus.streammusic.core.streaming.PcmFormat

internal const val RAOP_SAMPLE_RATE_HZ = 44_100
internal const val RAOP_FRAMES_PER_PACKET = 352

private const val BYTES_PER_SAMPLE = 2
private const val BYTES_PER_STEREO_FRAME = 4
private const val LEFT = 0
private const val RIGHT = 1
private const val RAOP_CHUNK_BYTES = RAOP_FRAMES_PER_PACKET * BYTES_PER_STEREO_FRAME

/**
 * Turns the captured audio (48 kHz) into the pieces AirPlay wants: 352 frames of 44.1 kHz, 16-bit stereo.
 * Audio that does not fill a whole piece yet waits for the next call.
 */
internal class RaopAudioChunker {
    private val resampler = StereoResampler(PcmFormat.SAMPLE_RATE_HZ, RAOP_SAMPLE_RATE_HZ)
    private val waitingBytes = ByteArrayOutputStream()

    fun add(capturedPcm: ByteArray): List<ByteArray> {
        waitingBytes.write(resampler.resample(capturedPcm))
        return takeCompleteChunks()
    }

    private fun takeCompleteChunks(): List<ByteArray> {
        val allWaiting = waitingBytes.toByteArray()
        val chunkCount = allWaiting.size / RAOP_CHUNK_BYTES
        val chunks = List(chunkCount) { index -> allWaiting.copyOfRange(index * RAOP_CHUNK_BYTES, (index + 1) * RAOP_CHUNK_BYTES) }
        waitingBytes.reset()
        waitingBytes.write(allWaiting, chunkCount * RAOP_CHUNK_BYTES, allWaiting.size - chunkCount * RAOP_CHUNK_BYTES)
        return chunks
    }
}

/**
 * Changes the sample rate of 16-bit little-endian stereo audio by drawing a straight line between the two
 * input frames around each output frame. It remembers where it stopped, so blocks join without a click.
 */
internal class StereoResampler(inputRate: Int, outputRate: Int) {
    private val inputFramesPerOutputFrame = inputRate.toDouble() / outputRate
    private var nextOutputPosition = 0.0
    private var previousLeft = 0
    private var previousRight = 0

    fun resample(pcm: ByteArray): ByteArray {
        val input = ByteBuffer.wrap(pcm).order(ByteOrder.LITTLE_ENDIAN)
        val inputFrameCount = pcm.size / BYTES_PER_STEREO_FRAME
        val output = ByteArrayOutputStream()
        while (nextOutputPosition < inputFrameCount) {
            writeFrameAt(nextOutputPosition, input, output)
            nextOutputPosition += inputFramesPerOutputFrame
        }
        nextOutputPosition -= inputFrameCount
        rememberLastFrame(input, inputFrameCount)
        return output.toByteArray()
    }

    private fun writeFrameAt(position: Double, input: ByteBuffer, output: ByteArrayOutputStream) {
        output.write(littleEndianBytesOf(interpolatedSample(input, position, channel = LEFT)))
        output.write(littleEndianBytesOf(interpolatedSample(input, position, channel = RIGHT)))
    }

    private fun interpolatedSample(input: ByteBuffer, position: Double, channel: Int): Int {
        val earlierFrame = position.toInt()
        val distanceToLaterFrame = position - earlierFrame
        val earlierSample = sampleAt(input, earlierFrame, channel)
        val laterSample = sampleAt(input, earlierFrame + 1, channel)
        return Math.round(earlierSample + (laterSample - earlierSample) * distanceToLaterFrame).toInt()
    }

    /** Frame 0 is the last frame of the previous block, frame 1 the first frame of this one. */
    private fun sampleAt(input: ByteBuffer, frameNumber: Int, channel: Int): Int {
        if (frameNumber == 0) return if (channel == LEFT) previousLeft else previousRight
        val byteIndex = (frameNumber - 1) * BYTES_PER_STEREO_FRAME + channel * BYTES_PER_SAMPLE
        return input.getShort(byteIndex).toInt()
    }

    private fun rememberLastFrame(input: ByteBuffer, inputFrameCount: Int) {
        if (inputFrameCount == 0) return
        previousLeft = sampleAt(input, inputFrameCount, channel = LEFT)
        previousRight = sampleAt(input, inputFrameCount, channel = RIGHT)
    }

    private fun littleEndianBytesOf(sample: Int): ByteArray =
        byteArrayOf(sample.toByte(), (sample shr 8).toByte())
}
