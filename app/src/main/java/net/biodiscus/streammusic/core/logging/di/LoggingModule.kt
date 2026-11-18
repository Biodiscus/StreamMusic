package net.biodiscus.streammusic.core.logging.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.biodiscus.streammusic.core.logging.SessionLog
import net.biodiscus.streammusic.core.logging.SessionLogTree

@Module
@InstallIn(SingletonComponent::class)
internal abstract class LoggingModule {

    @Binds
    abstract fun bindSessionLog(tree: SessionLogTree): SessionLog
}
