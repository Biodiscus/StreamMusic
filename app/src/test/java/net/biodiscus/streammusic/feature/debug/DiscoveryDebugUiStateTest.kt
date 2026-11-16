package net.biodiscus.streammusic.feature.debug

import net.biodiscus.streammusic.core.model.DeviceProtocol
import net.biodiscus.streammusic.testing.aDevice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoveryDebugUiStateTest {

    private val tvAsDlna = aDevice(name = "TV", protocol = DeviceProtocol.DLNA, host = "10.0.0.5")
    private val tvAsChromecast = aDevice(name = "TV", protocol = DeviceProtocol.CHROMECAST, host = "10.0.0.5")
    private val speakerAsDlna = aDevice(name = "Speaker", protocol = DeviceProtocol.DLNA, host = "10.0.0.6")
    private val fireTv = aDevice(name = "Fire TV", protocol = DeviceProtocol.FIRE_TV, host = "10.0.0.7")

    private val allFinds = listOf(fireTv, tvAsChromecast, tvAsDlna, speakerAsDlna)

    @Test
    fun `groups the finds into a category per protocol`() {
        val state = DiscoveryDebugUiState(allFinds)

        assertEquals(
            listOf(DeviceProtocol.DLNA, DeviceProtocol.CHROMECAST, DeviceProtocol.FIRE_TV),
            state.categories.map { category -> category.protocol },
        )
    }

    @Test
    fun `puts every find of a protocol in its category`() {
        val state = DiscoveryDebugUiState(allFinds)

        val dlnaCategory = state.categories.first { category -> category.protocol == DeviceProtocol.DLNA }
        assertEquals(listOf(tvAsDlna, speakerAsDlna), dlnaCategory.finds)
    }

    @Test
    fun `marks a category as implemented when the app can stream to its protocol`() {
        val state = DiscoveryDebugUiState(allFinds)

        val implementedByProtocol = state.categories.associate { category -> category.protocol to category.isImplemented }
        assertEquals(
            mapOf(DeviceProtocol.DLNA to true, DeviceProtocol.CHROMECAST to true, DeviceProtocol.FIRE_TV to false),
            implementedByProtocol,
        )
    }

    @Test
    fun `leaves out the categories the filter removes`() {
        val filter = DiscoveryDebugFilter(implementation = ImplementationFilter.NOT_IMPLEMENTED)

        val state = DiscoveryDebugUiState(allFinds, filter)

        assertEquals(listOf(DeviceProtocol.FIRE_TV), state.categories.map { category -> category.protocol })
    }

    @Test
    fun `counts the shown finds and all finds`() {
        val filter = DiscoveryDebugFilter(protocols = setOf(DeviceProtocol.DLNA))

        val state = DiscoveryDebugUiState(allFinds, filter)

        assertEquals(2, state.shownFindCount)
        assertEquals(4, state.totalFindCount)
    }

    @Test
    fun `offers a chip for every protocol that was found`() {
        val state = DiscoveryDebugUiState(allFinds)

        assertEquals(
            listOf(DeviceProtocol.DLNA, DeviceProtocol.CHROMECAST, DeviceProtocol.FIRE_TV),
            state.protocolChoices,
        )
    }

    @Test
    fun `keeps the chip of a filtered protocol that is no longer found`() {
        val filter = DiscoveryDebugFilter(protocols = setOf(DeviceProtocol.SONOS))

        val state = DiscoveryDebugUiState(listOf(fireTv), filter)

        assertTrue(DeviceProtocol.SONOS in state.protocolChoices)
    }

    @Test
    fun `is not searching when finds exist but the filter hides them all`() {
        val filter = DiscoveryDebugFilter(protocols = setOf(DeviceProtocol.SONOS))

        val state = DiscoveryDebugUiState(listOf(fireTv), filter)

        assertFalse(state.isSearching)
        assertEquals(0, state.shownFindCount)
    }
}
