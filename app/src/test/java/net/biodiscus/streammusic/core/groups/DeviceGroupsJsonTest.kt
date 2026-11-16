package net.biodiscus.streammusic.core.groups

import net.biodiscus.streammusic.core.model.DeviceGroupColor
import net.biodiscus.streammusic.core.model.DeviceGroupIcon
import net.biodiscus.streammusic.core.model.DeviceProtocol
import net.biodiscus.streammusic.testing.aGroup
import net.biodiscus.streammusic.testing.aGroupMember
import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceGroupsJsonTest {

    @Test
    fun `saved groups are read back unchanged`() {
        val groups = listOf(
            aGroup("Downstairs", aGroupMember("Kitchen", DeviceProtocol.SONOS, "10.0.0.2"), aGroupMember("Study", host = "10.0.0.3")),
            aGroup("Upstairs"),
        )

        assertEquals(groups, decodeDeviceGroups(encodeDeviceGroups(groups)))
    }

    @Test
    fun `the icon of a group is read back`() {
        val groups = listOf(aGroup("Kitchen").copy(icon = DeviceGroupIcon.FOLDER))

        assertEquals(groups, decodeDeviceGroups(encodeDeviceGroups(groups)))
    }

    @Test
    fun `the color of a group is read back`() {
        val groups = listOf(aGroup("Kitchen").copy(color = DeviceGroupColor.PURPLE))

        assertEquals(groups, decodeDeviceGroups(encodeDeviceGroups(groups)))
    }

    @Test
    fun `a group saved before colors existed gets the default color`() {
        val text = """[{"name":"Old","members":[]}]"""

        assertEquals(DeviceGroupColor.BLUE, decodeDeviceGroups(text).single().color)
    }

    @Test
    fun `a group saved before icons existed gets the default icon`() {
        val text = """[{"name":"Old","members":[]}]"""

        assertEquals(DeviceGroupIcon.SPEAKER_GROUP, decodeDeviceGroups(text).single().icon)
    }

    @Test
    fun `nothing saved yet means no groups`() {
        assertEquals(emptyList<Nothing>(), decodeDeviceGroups(null))
    }

    @Test
    fun `text that cannot be read means no groups`() {
        assertEquals(emptyList<Nothing>(), decodeDeviceGroups("not json"))
    }

    @Test
    fun `a group with a protocol that does not exist any more means no groups`() {
        val text = """[{"name":"Old","members":[{"host":"10.0.0.2","name":"Kitchen","protocol":"REMOVED"}]}]"""

        assertEquals(emptyList<Nothing>(), decodeDeviceGroups(text))
    }

    @Test
    fun `a group with an icon that does not exist any more gets the default icon`() {
        val text = """[{"name":"Old","members":[],"icon":"KITCHEN"}]"""

        assertEquals(DeviceGroupIcon.SPEAKER_GROUP, decodeDeviceGroups(text).single().icon)
    }
}
