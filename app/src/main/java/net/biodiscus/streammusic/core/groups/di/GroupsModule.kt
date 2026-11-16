package net.biodiscus.streammusic.core.groups.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.biodiscus.streammusic.core.groups.DeviceGroupRepository
import net.biodiscus.streammusic.core.groups.SharedPreferencesDeviceGroupRepository

@Module
@InstallIn(SingletonComponent::class)
internal abstract class GroupsModule {

    @Binds
    abstract fun bindDeviceGroupRepository(repository: SharedPreferencesDeviceGroupRepository): DeviceGroupRepository
}
