package net.biodiscus.streammusic.core.settings

import kotlinx.coroutines.flow.Flow
import net.biodiscus.streammusic.core.model.AacQuality
import net.biodiscus.streammusic.core.model.AppSettings
import net.biodiscus.streammusic.core.model.ProtocolFamily
import net.biodiscus.streammusic.core.model.SilenceTimeout
import net.biodiscus.streammusic.core.model.StreamAudioFormat
import net.biodiscus.streammusic.core.model.ThemeMode

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

    fun setThemeMode(themeMode: ThemeMode)

    fun setMutesPhoneWhileStreaming(mutesPhone: Boolean)

    fun setReconnectsDroppedDevices(reconnects: Boolean)

    fun setAacQuality(quality: AacQuality)

    fun setStopsStreamingAfterSilence(timeout: SilenceTimeout)

    /** Puts every setting back to what it was when the app was installed. */
    fun resetToDefaults()
}
