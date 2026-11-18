package net.biodiscus.streammusic

import android.app.Application
import android.content.pm.ApplicationInfo
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import net.biodiscus.streammusic.core.logging.SessionLogTree
import timber.log.Timber

@HiltAndroidApp
class StreamMusicApplication : Application() {

    @Inject
    internal lateinit var sessionLogTree: SessionLogTree

    override fun onCreate() {
        super.onCreate()
        if (isDebuggableBuild()) Timber.plant(Timber.DebugTree())
        Timber.plant(sessionLogTree)
    }

    private fun isDebuggableBuild(): Boolean =
        applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
}
