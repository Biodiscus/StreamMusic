package net.biodiscus.streammusic.core.streaming.device.airplay

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AudioDelayTest {

    private fun frames(vararg ids: Int): List<ByteArray> = ids.map { id -> byteArrayOf(id.toByte()) }

    private fun List<ByteArray>.ids(): List<Int> = map { frame -> frame[0].toInt() }

    @Test
    fun `no delay passes every frame on`() = runTest {
        val delayed = flowOf(*frames(1, 2, 3).toTypedArray()).delayedByMillis(0).toList()

        assertEquals(listOf(1, 2, 3), delayed.ids())
    }

    @Test
    fun `holds back as many frames as the delay lasts`() = runTest {
        val delayed = flowOf(*frames(1, 2, 3, 4).toTypedArray()).delayedByMillis(40).toList()

        assertEquals(listOf(1, 2), delayed.ids())
    }

    @Test
    fun `passes nothing on while the delay has not passed`() = runTest {
        val delayed = flowOf(*frames(1, 2).toTypedArray()).delayedByMillis(100).toList()

        assertEquals(emptyList<Int>(), delayed.ids())
    }

    @Test
    fun `rounds a delay down to whole frames`() = runTest {
        val delayed = flowOf(*frames(1, 2, 3).toTypedArray()).delayedByMillis(39).toList()

        assertEquals(listOf(1, 2), delayed.ids())
    }

    @Test
    fun `a delay of one second moves the timeline by 44100 samples`() {
        assertEquals(44_100L, airPlaySamplesDelayedBy(1_000))
    }

    @Test
    fun `no delay does not move the timeline`() {
        assertEquals(0L, airPlaySamplesDelayedBy(0))
    }

    @Test
    fun `a delay of 20 milliseconds moves the timeline by one captured frame`() {
        assertEquals(882L, airPlaySamplesDelayedBy(20))
    }
}
