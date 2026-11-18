package net.biodiscus.streammusic.core.groups

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import net.biodiscus.streammusic.core.model.DeviceGroup
import net.biodiscus.streammusic.core.model.withGroup
import net.biodiscus.streammusic.core.model.withGroupUpdated
import net.biodiscus.streammusic.core.model.withoutGroup

private const val PREFERENCES_FILE_NAME = "device_groups"
private const val GROUPS_KEY = "groups"

@Singleton
internal class SharedPreferencesDeviceGroupRepository @Inject constructor(
    @ApplicationContext context: Context,
) : DeviceGroupRepository {

    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE)

    private val groups = MutableStateFlow(decodeDeviceGroups(preferences.getString(GROUPS_KEY, null)))

    override fun observeGroups(): Flow<List<DeviceGroup>> = groups

    override fun saveGroup(group: DeviceGroup) {
        groups.update { saved -> saved.withGroup(group) }
        writeToPreferences()
    }

    override fun updateGroup(oldName: String, group: DeviceGroup) {
        groups.update { saved -> saved.withGroupUpdated(oldName, group) }
        writeToPreferences()
    }

    override fun deleteGroup(name: String) {
        groups.update { saved -> saved.withoutGroup(name) }
        writeToPreferences()
    }

    override fun deleteAllGroups() {
        groups.value = emptyList()
        writeToPreferences()
    }

    private fun writeToPreferences() {
        preferences.edit().putString(GROUPS_KEY, encodeDeviceGroups(groups.value)).apply()
    }
}
