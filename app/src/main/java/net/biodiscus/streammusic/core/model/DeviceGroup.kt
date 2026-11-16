package net.biodiscus.streammusic.core.model

import kotlinx.serialization.Serializable

const val MIN_DEVICES_IN_GROUP = 2

/** What the user picked to recognise a group by. How each one looks is decided by the screen. */
enum class DeviceGroupIcon {
    SPEAKER_GROUP,
    FOLDER,
    LIBRARY,
    CATEGORY,
}

/** The colour of a group's name. How each one looks is decided by the screen. */
enum class DeviceGroupColor {
    RED,
    ORANGE,
    GREEN,
    TEAL,
    BLUE,
    INDIGO,
    PURPLE,
    PINK,
}

/** A device the user saved in a group, with the protocol it was streamed with. The [name] is kept to describe it while it is offline. */
@Serializable
data class DeviceGroupMember(
    val host: String,
    val name: String,
    val protocol: DeviceProtocol,
)

/** Devices the user streams to together. The [name] identifies the group: saving the same name again replaces it. */
@Serializable
data class DeviceGroup(
    val name: String,
    val members: List<DeviceGroupMember>,
    val icon: DeviceGroupIcon = DeviceGroupIcon.SPEAKER_GROUP,
    val color: DeviceGroupColor = DeviceGroupColor.BLUE,
)

/** The way each member streams, for the members that are on the network right now. */
fun DeviceGroup.streamingOptionsIn(devices: List<NetworkDevice>): List<StreamingDevice> =
    members.mapNotNull { member ->
        val device = devices.firstOrNull { networkDevice -> networkDevice.host == member.host }
        device?.streamingOptionFor(member.protocol)
    }

fun List<DeviceGroup>.withGroup(group: DeviceGroup): List<DeviceGroup> =
    filterNot { existing -> existing.name == group.name } + group

/**
 * Replaces the group called [oldName] in place, which also renames it.
 * Like [withGroup], another group that already has the new name is replaced.
 */
fun List<DeviceGroup>.withGroupUpdated(oldName: String, updated: DeviceGroup): List<DeviceGroup> {
    val groupsWithoutNameClash = filterNot { group -> group.name == updated.name && group.name != oldName }
    return groupsWithoutNameClash.map { group -> if (group.name == oldName) updated else group }
}

fun List<DeviceGroup>.withoutGroup(name: String): List<DeviceGroup> =
    filterNot { group -> group.name == name }

fun isValidGroupName(name: String): Boolean = name.isNotBlank()

fun isSavableGroup(name: String, deviceCount: Int): Boolean =
    isValidGroupName(name) && deviceCount >= MIN_DEVICES_IN_GROUP
