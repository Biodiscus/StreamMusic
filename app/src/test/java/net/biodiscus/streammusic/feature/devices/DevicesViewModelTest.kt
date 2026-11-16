package net.biodiscus.streammusic.feature.devices

import app.cash.turbine.test
import net.biodiscus.streammusic.core.model.DeviceProtocol
import net.biodiscus.streammusic.testing.FakeDeviceRepository
import net.biodiscus.streammusic.testing.MainDispatcherRule
import net.biodiscus.streammusic.testing.aDevice
import net.biodiscus.streammusic.testing.aNetworkDevice
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

            assertEquals(DevicesUiState.DevicesFound(listOf(aNetworkDevice(homePod), aNetworkDevice(kitchen)), emptySet()), awaitItem())
        }
    }

    @Test
    fun `clicking a device selects it`() = runTest {
        repository.setFoundDevices(kitchen)
        viewModel.uiState.test {
            awaitItem()

            viewModel.onDeviceClicked(aNetworkDevice(kitchen))

            assertEquals(setOf(kitchen.host), (expectMostRecentItem() as DevicesUiState.DevicesFound).selectedDeviceIds)
        }
    }

    @Test
    fun `clicking a selected device deselects it`() = runTest {
        repository.setFoundDevices(kitchen)
        viewModel.uiState.test {
            awaitItem()
            viewModel.onDeviceClicked(aNetworkDevice(kitchen))

            viewModel.onDeviceClicked(aNetworkDevice(kitchen))

            assertEquals(emptySet<String>(), (expectMostRecentItem() as DevicesUiState.DevicesFound).selectedDeviceIds)
        }
    }

    @Test
    fun `several devices can be selected at once`() = runTest {
        val study = aDevice(name = "Study", host = "10.0.0.4")
        repository.setFoundDevices(kitchen, study)
        viewModel.uiState.test {
            awaitItem()

            viewModel.onDeviceClicked(aNetworkDevice(kitchen))
            viewModel.onDeviceClicked(aNetworkDevice(study))

            assertEquals(setOf(kitchen.host, study.host), (expectMostRecentItem() as DevicesUiState.DevicesFound).selectedDeviceIds)
        }
    }

    @Test
    fun `clicking a device that cannot receive a stream does not select it`() = runTest {
        repository.setFoundDevices(homePod)
        viewModel.uiState.test {
            awaitItem()

            viewModel.onDeviceClicked(aNetworkDevice(homePod))

            val uiState = viewModel.uiState.value as DevicesUiState.DevicesFound
            assertEquals(emptySet<String>(), uiState.selectedDeviceIds)
        }
    }

    @Test
    fun `searching again forgets the devices, clears the selection and restarts the search`() = runTest {
        repository.setFoundDevices(kitchen)
        viewModel.uiState.test {
            awaitItem()
            viewModel.onDeviceClicked(aNetworkDevice(kitchen))

            viewModel.onRefreshClicked()

            assertEquals(1, repository.forgetAllCount)
            assertEquals(2, repository.observeCount)
            assertEquals(DevicesUiState.Searching, expectMostRecentItem())
        }
    }

    @Test
    fun `choosing a protocol is remembered for that device`() = runTest {
        val kitchenDlna = aDevice(protocol = DeviceProtocol.DLNA, host = kitchen.host)
        val device = aNetworkDevice(kitchen, kitchenDlna)
        repository.setFoundDevices(kitchen, kitchenDlna)
        viewModel.uiState.test {
            awaitItem()

            viewModel.onProtocolChosen(device, DeviceProtocol.DLNA)

            val uiState = expectMostRecentItem() as DevicesUiState.DevicesFound
            assertEquals(mapOf(kitchen.host to DeviceProtocol.DLNA), uiState.chosenProtocolByDeviceId)
        }
    }

    @Test
    fun `lists a device that speaks several protocols once`() = runTest {
        val kitchenDlna = aDevice(name = "Kitchen DLNA", protocol = DeviceProtocol.DLNA, host = kitchen.host)
        viewModel.uiState.test {
            awaitItem()

            repository.setFoundDevices(kitchen, kitchenDlna)

            val uiState = expectMostRecentItem() as DevicesUiState.DevicesFound
            assertEquals(listOf(aNetworkDevice(kitchen, kitchenDlna)), uiState.devices)
        }
    }
}
