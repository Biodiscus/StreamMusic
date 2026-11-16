package net.biodiscus.streammusic.feature.devices

import app.cash.turbine.test
import net.biodiscus.streammusic.core.model.DeviceProtocol
import net.biodiscus.streammusic.core.model.DeviceStreamStatus
import net.biodiscus.streammusic.core.streaming.StreamingStateRepository
import net.biodiscus.streammusic.testing.FakeDeviceRepository
import net.biodiscus.streammusic.testing.FakeStreamingController
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
    private val streamingState = StreamingStateRepository()
    private val streamingController = FakeStreamingController()
    private lateinit var viewModel: DevicesViewModel

    @Before
    fun createViewModel() {
        viewModel = createViewModelWithCurrentStreamingState()
    }

    private val kitchen = aDevice(name = "Kitchen", protocol = DeviceProtocol.SONOS, host = "10.0.0.2")
    private val study = aDevice(name = "Study", protocol = DeviceProtocol.DLNA, host = "10.0.0.4")
    private val homePod = aDevice(name = "HomePod", protocol = DeviceProtocol.AIRPLAY, host = "10.0.0.3")

    private fun createViewModelWithCurrentStreamingState() =
        DevicesViewModel(repository, streamingState, streamingController)

    @Test
    fun `is searching until a device is found`() = runTest {
        viewModel.uiState.test {
            assertEquals(DevicesUiState.Searching(), awaitItem())
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
            assertEquals(DevicesUiState.Searching(), expectMostRecentItem())
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

    @Test
    fun `ticking a device while idle does not touch the streaming service`() = runTest {
        repository.setFoundDevices(kitchen)
        viewModel.uiState.test {
            awaitItem()

            viewModel.onDeviceClicked(aNetworkDevice(kitchen))

            assertEquals(emptyList<String>(), streamingController.addedDeviceIds)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ticking a device while streaming adds it to the stream`() = runTest {
        repository.setFoundDevices(kitchen, study)
        streamingState.streamingStarted(listOf(kitchen))
        viewModel.uiState.test {
            awaitItem()

            viewModel.onDeviceClicked(aNetworkDevice(study))

            assertEquals(listOf(study.id), streamingController.addedDeviceIds)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ticking a device while streaming uses the protocol the user chose`() = runTest {
        val kitchenDlna = aDevice(protocol = DeviceProtocol.DLNA, host = kitchen.host)
        val device = aNetworkDevice(kitchen, kitchenDlna)
        repository.setFoundDevices(kitchen, kitchenDlna, study)
        streamingState.streamingStarted(listOf(study))
        viewModel.uiState.test {
            awaitItem()
            viewModel.onProtocolChosen(device, DeviceProtocol.DLNA)

            viewModel.onDeviceClicked(device)

            assertEquals(listOf(kitchenDlna.id), streamingController.addedDeviceIds)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `unticking a device while streaming removes it from the stream`() = runTest {
        repository.setFoundDevices(kitchen, study)
        streamingState.streamingStarted(listOf(kitchen, study))
        viewModel = createViewModelWithCurrentStreamingState()
        viewModel.uiState.test {
            awaitItem()

            viewModel.onDeviceClicked(aNetworkDevice(study))

            assertEquals(listOf(study.id), streamingController.removedDeviceIds)
            assertEquals(0, streamingController.stopCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `unticking the last device while streaming stops the stream`() = runTest {
        repository.setFoundDevices(kitchen)
        streamingState.streamingStarted(listOf(kitchen))
        viewModel = createViewModelWithCurrentStreamingState()
        viewModel.uiState.test {
            awaitItem()

            viewModel.onDeviceClicked(aNetworkDevice(kitchen))

            assertEquals(1, streamingController.stopCount)
            assertEquals(emptyList<String>(), streamingController.removedDeviceIds)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the devices of a stream that is already running are ticked`() = runTest {
        repository.setFoundDevices(kitchen, study)
        streamingState.streamingStarted(listOf(kitchen))
        viewModel = createViewModelWithCurrentStreamingState()

        viewModel.uiState.test {
            val uiState = expectMostRecentItem() as DevicesUiState.DevicesFound
            assertEquals(setOf(kitchen.host), uiState.selectedDeviceIds)
        }
    }

    @Test
    fun `shows the status of the devices in the stream`() = runTest {
        repository.setFoundDevices(kitchen, study)
        streamingState.streamingStarted(listOf(kitchen))
        streamingState.deviceStatusChanged(kitchen.id, DeviceStreamStatus.Streaming)
        viewModel = createViewModelWithCurrentStreamingState()

        viewModel.uiState.test {
            val uiState = expectMostRecentItem() as DevicesUiState.DevicesFound
            assertEquals(mapOf(kitchen.host to DeviceStreamStatus.Streaming), uiState.streamStatusByDeviceId)
            assertEquals(true, uiState.isStreaming)
        }
    }

    @Test
    fun `is not streaming while the stream is idle`() = runTest {
        repository.setFoundDevices(kitchen)

        viewModel.uiState.test {
            val uiState = expectMostRecentItem() as DevicesUiState.DevicesFound
            assertEquals(false, uiState.isStreaming)
        }
    }

    @Test
    fun `searching again while streaming keeps the devices of the stream ticked`() = runTest {
        repository.setFoundDevices(kitchen)
        streamingState.streamingStarted(listOf(kitchen))
        viewModel = createViewModelWithCurrentStreamingState()
        viewModel.uiState.test {
            awaitItem()

            viewModel.onRefreshClicked()
            repository.setFoundDevices(kitchen)

            val uiState = expectMostRecentItem() as DevicesUiState.DevicesFound
            assertEquals(setOf(kitchen.host), uiState.selectedDeviceIds)
        }
    }

    @Test
    fun `knows that it is streaming while the search has not found a device yet`() = runTest {
        streamingState.streamingStarted(listOf(kitchen))

        viewModel.uiState.test {
            assertEquals(DevicesUiState.Searching(isStreaming = true), expectMostRecentItem())
        }
    }

    @Test
    fun `clicking stop stops the stream`() {
        viewModel.onStopClicked()

        assertEquals(1, streamingController.stopCount)
    }
}
