package net.biodiscus.streammusic.core.logging

import java.io.IOException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import timber.log.Timber

class SessionLogTreeTest {

    private var nowMillis = 1_000L
    private val tree = aTreeKeeping(entries = 10)

    private fun aTreeKeeping(entries: Int) = SessionLogTree(entriesKept = entries, currentTimeMillis = { nowMillis })

    @Before
    fun plantTree() {
        Timber.plant(tree)
    }

    @After
    fun uprootTree() {
        Timber.uprootAll()
    }

    @Test
    fun `starts without entries`() {
        assertTrue(tree.entries.value.isEmpty())
    }

    @Test
    fun `stores the message with the time it was logged`() {
        Timber.i("Connecting to %s", "Kitchen")

        val entry = tree.entries.value.single()
        assertEquals("Connecting to Kitchen", entry.message)
        assertEquals(1_000L, entry.timestampMillis)
    }

    @Test
    fun `maps each Timber level to a log level`() {
        Timber.d("debug")
        Timber.i("info")
        Timber.w("warning")
        Timber.e("error")

        val levels = tree.entries.value.map { entry -> entry.level }
        assertEquals(listOf(LogLevel.DEBUG, LogLevel.INFO, LogLevel.WARNING, LogLevel.ERROR), levels)
    }

    @Test
    fun `keeps the stack trace of a logged error`() {
        Timber.e(IOException("Connection refused"), "Streaming failed")

        val stackTrace = tree.entries.value.single().stackTrace
        assertNotNull(stackTrace)
        assertTrue(stackTrace!!.contains("java.io.IOException: Connection refused"))
    }

    @Test
    fun `has no stack trace when no error was logged`() {
        Timber.i("Streaming")

        assertNull(tree.entries.value.single().stackTrace)
    }

    @Test
    fun `tags an entry with the class that logged it`() {
        Timber.i("Streaming")

        assertEquals("SessionLogTreeTest", tree.entries.value.single().tag)
    }

    @Test
    fun `drops the oldest entries when more are logged than are kept`() {
        val smallTree = aTreeKeeping(entries = 3)
        Timber.plant(smallTree)

        listOf("one", "two", "three", "four").forEach { message -> Timber.i(message) }

        val messages = smallTree.entries.value.map { entry -> entry.message }
        assertEquals(listOf("two", "three", "four"), messages)
    }
}
