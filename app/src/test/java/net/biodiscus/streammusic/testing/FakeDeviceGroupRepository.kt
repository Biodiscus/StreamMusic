package net.biodiscus.streammusic.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import net.biodiscus.streammusic.core.groups.DeviceGroupRepository
import net.biodiscus.streammusic.core.model.DeviceGroup
import net.biodiscus.streammusic.core.model.withGroup
import net.biodiscus.streammusic.core.model.withGroupUpdated
import net.biodiscus.streammusic.core.model.withoutGroup

class FakeDeviceGroupRepository : DeviceGroupRepository {

    private val groups = MutableStateFlow<List<DeviceGroup>>(emptyList())

    val savedGroups: List<DeviceGroup>
        get() = groups.value

    override fun observeGroups(): Flow<List<DeviceGroup>> = groups

    override fun saveGroup(group: DeviceGroup) {
        groups.value = groups.value.withGroup(group)
    }

    override fun updateGroup(oldName: String, group: DeviceGroup) {
        groups.value = groups.value.withGroupUpdated(oldName, group)
    }

    override fun deleteGroup(name: String) {
        groups.value = groups.value.withoutGroup(name)
    }

    override fun deleteAllGroups() {
        groups.value = emptyList()
    }
}
