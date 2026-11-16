package net.biodiscus.streammusic.core.groups

import kotlinx.coroutines.flow.Flow
import net.biodiscus.streammusic.core.model.DeviceGroup

interface DeviceGroupRepository {
    /** Emits the saved groups, in the order they were saved, and again after every change. */
    fun observeGroups(): Flow<List<DeviceGroup>>

    /** Saves the group, replacing the saved group with the same name. */
    fun saveGroup(group: DeviceGroup)

    /** Replaces the group called [oldName] with [group], which may have another name. */
    fun updateGroup(oldName: String, group: DeviceGroup)

    fun deleteGroup(name: String)
}
