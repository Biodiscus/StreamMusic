package net.biodiscus.streammusic.feature.debug

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import net.biodiscus.streammusic.core.model.DeviceProtocol
import net.biodiscus.streammusic.testing.FakeDeviceRepository
import net.biodiscus.streammusic.testing.MainDispatcherRule
import net.biodiscus.streammusic.testing.aDevice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DiscoveryDebugViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeDeviceRepository()
    private val viewModel = DiscoveryDebugViewModel(repository)

    @Test
    fun `is searching until something is found`() = runTest {
        viewModel.uiState.test {
            assertTrue(awaitItem().isSearching)
        }
    }

    @Test
    fun `shows everything that was found`() = runTest {
        val tvAsChromecast = aDevice(name = "TV", protocol = DeviceProtocol.CHROMECAST, host = "10.0.0.5")
        val fireTv = aDevice(name = "Fire TV", protocol = DeviceProtocol.FIRE_TV, host = "10.0.0.6")
        repository.setFoundDevices(tvAsChromecast, fireTv)

        viewModel.uiState.test {
            awaitItem()

            assertEquals(listOf(tvAsChromecast, fireTv), awaitItem().finds)
        }
    }

    @Test
    fun `lists a device that speaks two protocols once per protocol`() = runTest {
        val tvAsChromecast = aDevice(name = "TV", protocol = DeviceProtocol.CHROMECAST, host = "10.0.0.5")
        val tvAsDlna = aDevice(name = "TV", protocol = DeviceProtocol.DLNA, host = "10.0.0.5")
        repository.setFoundDevices(tvAsChromecast, tvAsDlna)

        viewModel.uiState.test {
            awaitItem()

            assertEquals(2, awaitItem().finds.size)
        }
    }

    @Test
    fun `is no longer searching once something is found`() = runTest {
        repository.setFoundDevices(aDevice())

        viewModel.uiState.test {
            awaitItem()

            assertFalse(awaitItem().isSearching)
        }
    }

    @Test
    fun `choosing the implemented filter hides protocols that are not implemented`() = runTest {
        val chromecast = aDevice(protocol = DeviceProtocol.CHROMECAST, host = "10.0.0.5")
        val fireTv = aDevice(protocol = DeviceProtocol.FIRE_TV, host = "10.0.0.6")
        repository.setFoundDevices(chromecast, fireTv)
        viewModel.uiState.test {
            awaitItem()
            awaitItem()

            viewModel.onImplementationFilterChosen(ImplementationFilter.IMPLEMENTED)

            val shownProtocols = awaitItem().categories.map { category -> category.protocol }
            assertEquals(listOf(DeviceProtocol.CHROMECAST), shownProtocols)
        }
    }

    @Test
    fun `toggling a protocol twice shows everything again`() = runTest {
        val chromecast = aDevice(protocol = DeviceProtocol.CHROMECAST, host = "10.0.0.5")
        val fireTv = aDevice(protocol = DeviceProtocol.FIRE_TV, host = "10.0.0.6")
        repository.setFoundDevices(chromecast, fireTv)
        viewModel.uiState.test {
            awaitItem()
            awaitItem()

            viewModel.onProtocolFilterToggled(DeviceProtocol.FIRE_TV)
            awaitItem()
            viewModel.onProtocolFilterToggled(DeviceProtocol.FIRE_TV)

            assertEquals(2, awaitItem().shownFindCount)
        }
    }
}
