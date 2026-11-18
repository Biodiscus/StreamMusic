package net.biodiscus.streammusic.core.protocolchoices.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.biodiscus.streammusic.core.protocolchoices.ProtocolChoiceRepository
import net.biodiscus.streammusic.core.protocolchoices.SharedPreferencesProtocolChoiceRepository

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ProtocolChoicesModule {

    @Binds
    abstract fun bindProtocolChoiceRepository(repository: SharedPreferencesProtocolChoiceRepository): ProtocolChoiceRepository
}
