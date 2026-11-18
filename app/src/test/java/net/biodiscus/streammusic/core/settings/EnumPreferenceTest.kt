package net.biodiscus.streammusic.core.settings

import net.biodiscus.streammusic.core.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test

class EnumPreferenceTest {

    @Test
    fun `decodes the name of an entry`() {
        assertEquals(ThemeMode.DARK, decodeEnum("DARK", default = ThemeMode.SYSTEM))
    }

    @Test
    fun `gives the default when nothing was saved`() {
        assertEquals(ThemeMode.SYSTEM, decodeEnum(null, default = ThemeMode.SYSTEM))
    }

    @Test
    fun `gives the default for a name that no entry has anymore`() {
        assertEquals(ThemeMode.SYSTEM, decodeEnum("SEPIA", default = ThemeMode.SYSTEM))
    }
}
