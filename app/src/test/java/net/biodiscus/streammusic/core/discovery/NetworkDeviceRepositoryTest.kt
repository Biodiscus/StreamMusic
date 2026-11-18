package net.biodiscus.streammusic.core.discovery

import kotlinx.coroutines.flow.last
import kotlinx.coroutines.test.runTest
import net.biodiscus.streammusic.core.logging.SessionLogTree
import net.biodiscus.streammusic.core.model.DeviceProtocol
import net.biodiscus.streammusic.testing.FakeDeviceDiscovery
import net.biodiscus.streammusic.testing.aDevice
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import timber.log.Timber

class NetworkDeviceRepositoryTest {

    private val log = SessionLogTree(entriesKept = 50, currentTimeMillis = { 0L })

    @Before
    fun plantLog() {
        Timber.plant(log)
    }

    @After
    fun uprootLog() {
        Timber.uprootAll()
    }

    @Test
    fun `combines devices from all discoveries`() = runTest {
        val sonos = aDevice(name = "Kitchen", protocol = DeviceProtocol.SONOS, host = "10.0.0.2")
        val chromecast = aDevice(name = "TV", protocol = DeviceProtocol.CHROMECAST, host = "10.0.0.3")
        val repository = repositoryWith(FakeDeviceDiscovery(sonos), FakeDeviceDiscovery(chromecast))

        val devices = repository.observeDevices().last()

        assertEquals(setOf(sonos, chromecast), devices.toSet())
    }

    @Test
    fun `lists a device found twice only once`() = runTest {
        val device = aDevice(host = "10.0.0.2")
        val repository = repositoryWith(FakeDeviceDiscovery(device), FakeDeviceDiscovery(device))

        val devices = repository.observeDevices().last()

        assertEquals(listOf(device), devices)
    }

    @Test
    fun `sorts devices by name ignoring case`() = runTest {
        val bedroom = aDevice(name = "bedroom", host = "10.0.0.2")
        val attic = aDevice(name = "Attic", host = "10.0.0.3")
        val repository = repositoryWith(FakeDeviceDiscovery(bedroom, attic))

        val devices = repository.observeDevices().last()

        assertEquals(listOf(attic, bedroom), devices)
    }

    @Test
    fun `finds known devices by id`() = runTest {
        val kitchen = aDevice(name = "Kitchen", host = "10.0.0.2")
        val garage = aDevice(name = "Garage", host = "10.0.0.3")
        val repository = repositoryWith(FakeDeviceDiscovery(kitchen, garage))
        repository.observeDevices().last()

        val found = repository.findDevices(setOf(kitchen.id))

        assertEquals(listOf(kitchen), found)
    }

    @Test
    fun `forgets all devices`() = runTest {
        val repository = repositoryWith(FakeDeviceDiscovery(aDevice()))
        repository.observeDevices().last()

        repository.forgetAllDevices()

        assertTrue(repository.findDevices(setOf(aDevice().id)).isEmpty())
    }

    @Test
    fun `logs a device the first time it is found`() = runTest {
        val kitchen = aDevice(name = "Kitchen", protocol = DeviceProtocol.SONOS, host = "10.0.0.2")
        val repository = repositoryWith(FakeDeviceDiscovery(kitchen))

        repository.observeDevices().last()

        assertEquals(listOf("Found Kitchen (SONOS at 10.0.0.2:80), can stream: true"), foundMessages())
    }

    @Test
    fun `does not log a device again when it is found again`() = runTest {
        val kitchen = aDevice(name = "Kitchen", host = "10.0.0.2")
        val repository = repositoryWith(FakeDeviceDiscovery(kitchen), FakeDeviceDiscovery(kitchen))

        repository.observeDevices().last()

        assertEquals(1, foundMessages().size)
    }

    private fun foundMessages(): List<String> =
        log.entries.value.map { entry -> entry.message }.filter { message -> message.startsWith("Found ") }

    private fun repositoryWith(vararg discoveries: DeviceDiscovery) =
        NetworkDeviceRepository(discoveries.toSet())
}
