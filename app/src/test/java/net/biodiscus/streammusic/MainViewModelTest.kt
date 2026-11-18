package net.biodiscus.streammusic

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import net.biodiscus.streammusic.core.model.ThemeMode
import net.biodiscus.streammusic.testing.FakeSettingsRepository
import net.biodiscus.streammusic.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MainViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settingsRepository = FakeSettingsRepository()

    @Test
    fun `starts with the saved theme`() = runTest {
        settingsRepository.setThemeMode(ThemeMode.DARK)

        val viewModel = MainViewModel(settingsRepository)

        assertEquals(ThemeMode.DARK, viewModel.themeMode.value)
    }

    @Test
    fun `follows a theme change`() = runTest {
        val viewModel = MainViewModel(settingsRepository)
        viewModel.themeMode.test {
            awaitItem()

            settingsRepository.setThemeMode(ThemeMode.LIGHT)

            assertEquals(ThemeMode.LIGHT, awaitItem())
        }
    }
}
