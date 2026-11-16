package net.biodiscus.streammusic.core.streaming.device.airplay

import java.nio.ByteBuffer
import java.nio.ByteOrder
import net.biodiscus.streammusic.core.streaming.PcmFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RaopAudioChunkerTest {

    private val chunker = RaopAudioChunker()

    @Test
    fun `waits until a whole chunk is available`() {
        val chunks = chunker.add(capturedFrame(sample = 100))

        assertEquals(2, chunks.size)
        assertTrue(chunks.all { chunk -> chunk.size == RAOP_FRAMES_PER_PACKET * 4 })
    }

    @Test
    fun `keeps the audio that does not fill a chunk for the next call`() {
        val chunksPerHundredMilliseconds = (1..5).sumOf { chunker.add(capturedFrame(sample = 100)).size }

        assertEquals(12, chunksPerHundredMilliseconds)
    }

    @Test
    fun `gives the same amount of audio back at the lower sample rate`() {
        val outputBytes = (1..100).sumOf { chunker.add(capturedFrame(sample = 100)).sumOf { it.size } }
        val outputFrames = outputBytes / 4

        val expectedFrames = 2 * RAOP_SAMPLE_RATE_HZ
        assertTrue("got $outputFrames frames", outputFrames in (expectedFrames - RAOP_FRAMES_PER_PACKET)..expectedFrames)
    }

    @Test
    fun `a steady tone stays steady after resampling`() {
        val resampler = StereoResampler(48_000, 44_100)
        resampler.resample(pcmOf(listOf(1000 to 1000)))

        val samples = samplesOf(resampler.resample(pcmOf(List(960) { 1000 to 1000 })))

        assertTrue(samples.all { it == 1000 })
    }

    @Test
    fun `draws a straight line between two input frames`() {
        val resampler = StereoResampler(inputRate = 1, outputRate = 2)

        val leftSamples = samplesOf(resampler.resample(pcmOf(listOf(0 to 0, 100 to -100))))
            .filterIndexed { index, _ -> index % 2 == 0 }

        assertEquals(listOf(0, 0, 0, 50), leftSamples)
    }

    @Test
    fun `joins two blocks without a gap`() {
        val resampler = StereoResampler(inputRate = 1, outputRate = 2)
        resampler.resample(pcmOf(listOf(0 to 0, 100 to 0)))

        val leftSamples = samplesOf(resampler.resample(pcmOf(listOf(200 to 0)))).filterIndexed { index, _ -> index % 2 == 0 }

        assertEquals(listOf(100, 150), leftSamples)
    }

    private fun capturedFrame(sample: Int): ByteArray =
        pcmOf(List(PcmFormat.SAMPLES_PER_FRAME) { sample to sample })

    private fun pcmOf(frames: List<Pair<Int, Int>>): ByteArray {
        val buffer = ByteBuffer.allocate(frames.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        frames.forEach { (left, right) -> buffer.putShort(left.toShort()).putShort(right.toShort()) }
        return buffer.array()
    }

    private fun samplesOf(pcm: ByteArray): List<Int> {
        val buffer = ByteBuffer.wrap(pcm).order(ByteOrder.LITTLE_ENDIAN)
        return List(pcm.size / 2) { index -> buffer.getShort(index * 2).toInt() }
    }
}
