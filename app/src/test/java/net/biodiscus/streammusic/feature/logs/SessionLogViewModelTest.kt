package net.biodiscus.streammusic.feature.logs

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import net.biodiscus.streammusic.core.logging.LogLevel
import net.biodiscus.streammusic.testing.FakeSessionLog
import net.biodiscus.streammusic.testing.MainDispatcherRule
import net.biodiscus.streammusic.testing.aLogEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SessionLogViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sessionLog = FakeSessionLog()
    private val viewModel = SessionLogViewModel(sessionLog)

    @Test
    fun `starts without entries`() = runTest {
        viewModel.uiState.test {
            assertTrue(awaitItem().shownEntries.isEmpty())
        }
    }

    @Test
    fun `shows what was logged`() = runTest {
        val connecting = aLogEntry(message = "Connecting")
        sessionLog.log(connecting)

        viewModel.uiState.test {
            awaitItem()

            assertEquals(listOf(connecting), awaitItem().shownEntries)
        }
    }

    @Test
    fun `shows an entry that is logged while the screen is open`() = runTest {
        viewModel.uiState.test {
            awaitItem()

            val failed = aLogEntry(message = "Failed", level = LogLevel.ERROR)
            sessionLog.log(failed)

            assertEquals(listOf(failed), awaitItem().shownEntries)
        }
    }

    @Test
    fun `unchecking a level hides its entries`() = runTest {
        val connecting = aLogEntry(message = "Connecting", level = LogLevel.INFO)
        val failed = aLogEntry(message = "Failed", level = LogLevel.ERROR)
        sessionLog.log(connecting, failed)
        viewModel.uiState.test {
            awaitItem()
            awaitItem()

            viewModel.onLevelToggled(LogLevel.INFO)

            assertEquals(listOf(failed), awaitItem().shownEntries)
        }
    }

    @Test
    fun `checking a level again shows its entries again`() = runTest {
        val connecting = aLogEntry(message = "Connecting", level = LogLevel.INFO)
        sessionLog.log(connecting)
        viewModel.uiState.test {
            awaitItem()
            awaitItem()
            viewModel.onLevelToggled(LogLevel.INFO)
            awaitItem()

            viewModel.onLevelToggled(LogLevel.INFO)

            assertEquals(listOf(connecting), awaitItem().shownEntries)
        }
    }
}
