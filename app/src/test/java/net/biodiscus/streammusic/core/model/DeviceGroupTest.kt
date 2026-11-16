package net.biodiscus.streammusic.core.model

import net.biodiscus.streammusic.testing.aDevice
import net.biodiscus.streammusic.testing.aGroup
import net.biodiscus.streammusic.testing.aGroupMember
import net.biodiscus.streammusic.testing.aNetworkDevice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceGroupTest {

    private val kitchen = aDevice(name = "Kitchen", protocol = DeviceProtocol.SONOS, host = "10.0.0.2")
    private val study = aDevice(name = "Study", protocol = DeviceProtocol.DLNA, host = "10.0.0.3")

    private val kitchenMember = aGroupMember("Kitchen", DeviceProtocol.SONOS, "10.0.0.2")
    private val studyMember = aGroupMember("Study", DeviceProtocol.DLNA, "10.0.0.3")

    @Test
    fun `a group streams to each member that is on the network`() {
        val group = aGroup("Downstairs", kitchenMember, studyMember)

        val options = group.streamingOptionsIn(listOf(aNetworkDevice(kitchen), aNetworkDevice(study)))

        assertEquals(listOf(kitchen, study), options)
    }

    @Test
    fun `a group skips members that are not on the network`() {
        val group = aGroup("Downstairs", kitchenMember, studyMember)

        val options = group.streamingOptionsIn(listOf(aNetworkDevice(study)))

        assertEquals(listOf(study), options)
    }

    @Test
    fun `a member streams with the protocol saved in the group`() {
        val kitchenDlna = aDevice(protocol = DeviceProtocol.DLNA, host = kitchen.host)
        val group = aGroup("Downstairs", aGroupMember("Kitchen", DeviceProtocol.DLNA, kitchen.host))

        val options = group.streamingOptionsIn(listOf(aNetworkDevice(kitchen, kitchenDlna)))

        assertEquals(listOf(kitchenDlna), options)
    }

    @Test
    fun `a member falls back to the best protocol when the saved one is gone`() {
        val group = aGroup("Downstairs", aGroupMember("Kitchen", DeviceProtocol.CHROMECAST, kitchen.host))

        val options = group.streamingOptionsIn(listOf(aNetworkDevice(kitchen)))

        assertEquals(listOf(kitchen), options)
    }

    @Test
    fun `saving a group adds it after the existing groups`() {
        val saved = listOf(aGroup("Upstairs"))

        val groups = saved.withGroup(aGroup("Downstairs"))

        assertEquals(listOf("Upstairs", "Downstairs"), groups.map { group -> group.name })
    }

    @Test
    fun `saving a group with an existing name replaces it`() {
        val saved = listOf(aGroup("Downstairs", kitchenMember))

        val groups = saved.withGroup(aGroup("Downstairs", studyMember))

        assertEquals(listOf(aGroup("Downstairs", studyMember)), groups)
    }

    @Test
    fun `updating a group renames it and keeps its place`() {
        val saved = listOf(aGroup("Upstairs"), aGroup("Downstairs"), aGroup("Garden"))

        val groups = saved.withGroupUpdated("Downstairs", aGroup("Ground floor"))

        assertEquals(listOf("Upstairs", "Ground floor", "Garden"), groups.map { group -> group.name })
    }

    @Test
    fun `updating a group without renaming it changes its devices`() {
        val saved = listOf(aGroup("Downstairs", kitchenMember))

        val groups = saved.withGroupUpdated("Downstairs", aGroup("Downstairs", studyMember))

        assertEquals(listOf(aGroup("Downstairs", studyMember)), groups)
    }

    @Test
    fun `renaming a group to the name of another group replaces that other group`() {
        val saved = listOf(aGroup("Upstairs", kitchenMember), aGroup("Downstairs", studyMember))

        val groups = saved.withGroupUpdated("Downstairs", aGroup("Upstairs", studyMember))

        assertEquals(listOf(aGroup("Upstairs", studyMember)), groups)
    }

    @Test
    fun `a group with a name and two devices can be saved`() {
        assertTrue(isSavableGroup("Downstairs", deviceCount = 2))
    }

    @Test
    fun `a group with one device cannot be saved`() {
        assertFalse(isSavableGroup("Downstairs", deviceCount = 1))
    }

    @Test
    fun `a group without a name cannot be saved`() {
        assertFalse(isSavableGroup(" ", deviceCount = 2))
    }

    @Test
    fun `deleting a group removes only that group`() {
        val saved = listOf(aGroup("Upstairs"), aGroup("Downstairs"))

        val groups = saved.withoutGroup("Upstairs")

        assertEquals(listOf(aGroup("Downstairs")), groups)
    }

    @Test
    fun `a name with text is a valid group name`() {
        assertTrue(isValidGroupName("Downstairs"))
    }

    @Test
    fun `a blank name is not a valid group name`() {
        assertFalse(isValidGroupName("  "))
    }
}
