package net.biodiscus.streammusic.feature.devices

import org.junit.Assert.assertEquals
import org.junit.Test

class DevicesUiStateTest {

    @Test
    fun `selecting an unselected device adds it`() {
        assertEquals(setOf("a", "b"), toggleSelection(setOf("a"), "b"))
    }

    @Test
    fun `selecting a selected device removes it`() {
        assertEquals(setOf("a"), toggleSelection(setOf("a", "b"), "b"))
    }
}
