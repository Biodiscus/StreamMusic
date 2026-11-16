package net.biodiscus.streammusic.core.settings.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.biodiscus.streammusic.core.settings.SettingsRepository
import net.biodiscus.streammusic.core.settings.SharedPreferencesSettingsRepository

@Module
@InstallIn(SingletonComponent::class)
internal abstract class SettingsModule {

    @Binds
    abstract fun bindSettingsRepository(repository: SharedPreferencesSettingsRepository): SettingsRepository
}
