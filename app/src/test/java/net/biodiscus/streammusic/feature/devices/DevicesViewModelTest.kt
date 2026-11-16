package net.biodiscus.streammusic.feature.devices

import app.cash.turbine.test
import net.biodiscus.streammusic.core.model.DeviceProtocol
import net.biodiscus.streammusic.testing.FakeDeviceRepository
import net.biodiscus.streammusic.testing.MainDispatcherRule
import net.biodiscus.streammusic.testing.aDevice
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class DevicesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeDeviceRepository()
    private lateinit var viewModel: DevicesViewModel

    @Before
    fun createViewModel() {
        viewModel = DevicesViewModel(repository)
    }

    private val kitchen = aDevice(name = "Kitchen", protocol = DeviceProtocol.SONOS, host = "10.0.0.2")
    private val homePod = aDevice(name = "HomePod", protocol = DeviceProtocol.AIRPLAY, host = "10.0.0.3")

    @Test
    fun `is searching until a device is found`() = runTest {
        viewModel.uiState.test {
            assertEquals(DevicesUiState.Searching, awaitItem())
        }
    }

    @Test
    fun `lists devices that were found with nothing selected`() = runTest {
        viewModel.uiState.test {
            awaitItem()

            repository.setFoundDevices(kitchen, homePod)

            assertEquals(DevicesUiState.DevicesFound(listOf(kitchen, homePod), emptySet()), awaitItem())
        }
    }

    @Test
    fun `clicking a device selects it`() = runTest {
        repository.setFoundDevices(kitchen)
        viewModel.uiState.test {
            awaitItem()

            viewModel.onDeviceClicked(kitchen)

            assertEquals(setOf(kitchen.id), (expectMostRecentItem() as DevicesUiState.DevicesFound).selectedDeviceIds)
        }
    }

    @Test
    fun `clicking a selected device deselects it`() = runTest {
        repository.setFoundDevices(kitchen)
        viewModel.uiState.test {
            awaitItem()
            viewModel.onDeviceClicked(kitchen)

            viewModel.onDeviceClicked(kitchen)

            assertEquals(emptySet<String>(), (expectMostRecentItem() as DevicesUiState.DevicesFound).selectedDeviceIds)
        }
    }

    @Test
    fun `several devices can be selected at once`() = runTest {
        val study = aDevice(name = "Study", host = "10.0.0.4")
        repository.setFoundDevices(kitchen, study)
        viewModel.uiState.test {
            awaitItem()

            viewModel.onDeviceClicked(kitchen)
            viewModel.onDeviceClicked(study)

            assertEquals(setOf(kitchen.id, study.id), (expectMostRecentItem() as DevicesUiState.DevicesFound).selectedDeviceIds)
        }
    }

    @Test
    fun `clicking a device that cannot receive a stream does not select it`() = runTest {
        repository.setFoundDevices(homePod)
        viewModel.uiState.test {
            awaitItem()

            viewModel.onDeviceClicked(homePod)

            val uiState = viewModel.uiState.value as DevicesUiState.DevicesFound
            assertEquals(emptySet<String>(), uiState.selectedDeviceIds)
        }
    }

    @Test
    fun `searching again forgets the devices, clears the selection and restarts the search`() = runTest {
        repository.setFoundDevices(kitchen)
        viewModel.uiState.test {
            awaitItem()
            viewModel.onDeviceClicked(kitchen)

            viewModel.onRefreshClicked()

            assertEquals(1, repository.forgetAllCount)
            assertEquals(2, repository.observeCount)
            assertEquals(DevicesUiState.Searching, expectMostRecentItem())
        }
    }
}
