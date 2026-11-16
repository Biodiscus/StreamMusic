package net.biodiscus.streammusic.core.settings

import kotlinx.coroutines.flow.Flow
import net.biodiscus.streammusic.core.model.AppSettings
import net.biodiscus.streammusic.core.model.ProtocolFamily
import net.biodiscus.streammusic.core.model.StreamAudioFormat

interface SettingsRepository {
    /** The settings right now, for code that needs them once, like a stream that is starting. */
    val currentSettings: AppSettings

    /** Emits the current settings, and again after every change. */
    fun observeSettings(): Flow<AppSettings>

    fun setProtocolFamilyEnabled(family: ProtocolFamily, isEnabled: Boolean)

    fun setHidesSingleAppCapture(hidesSingleAppCapture: Boolean)

    fun setAudioFormat(family: ProtocolFamily, format: StreamAudioFormat)

    fun setAirPlayAudioDelayMillis(delayMillis: Int)

    fun setDisconnectsChromecastOnMute(disconnectsOnMute: Boolean)
}
