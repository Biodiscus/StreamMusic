package net.biodiscus.streammusic.core.settings

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import net.biodiscus.streammusic.core.model.AacQuality
import net.biodiscus.streammusic.core.model.AppSettings
import net.biodiscus.streammusic.core.model.ProtocolFamily
import net.biodiscus.streammusic.core.model.SilenceTimeout
import net.biodiscus.streammusic.core.model.StreamAudioFormat
import net.biodiscus.streammusic.core.model.ThemeMode
import net.biodiscus.streammusic.core.model.withAudioFormat
import net.biodiscus.streammusic.core.model.withFamilyEnabled

private const val PREFERENCES_FILE_NAME = "settings"
private const val DISABLED_PROTOCOLS_KEY = "disabled_protocols"
private const val HIDES_SINGLE_APP_CAPTURE_KEY = "hides_single_app_capture"
private const val AUDIO_FORMATS_KEY = "audio_formats"
private const val AIRPLAY_AUDIO_DELAY_MILLIS_KEY = "airplay_audio_delay_millis"
private const val DISCONNECTS_CHROMECAST_ON_MUTE_KEY = "disconnects_chromecast_on_mute"
private const val THEME_MODE_KEY = "theme_mode"
private const val MUTES_PHONE_WHILE_STREAMING_KEY = "mutes_phone_while_streaming"
private const val RECONNECTS_DROPPED_DEVICES_KEY = "reconnects_dropped_devices"
private const val AAC_QUALITY_KEY = "aac_quality"
private const val STOPS_STREAMING_AFTER_SILENCE_KEY = "stops_streaming_after_silence"

@Singleton
internal class SharedPreferencesSettingsRepository @Inject constructor(
    @ApplicationContext context: Context,
) : SettingsRepository {

    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE)

    private val settings = MutableStateFlow(readFromPreferences())

    override val currentSettings: AppSettings
        get() = settings.value

    override fun observeSettings(): Flow<AppSettings> = settings

    override fun setProtocolFamilyEnabled(family: ProtocolFamily, isEnabled: Boolean) {
        changeSettings { current -> current.withFamilyEnabled(family, isEnabled) }
    }

    override fun setHidesSingleAppCapture(hidesSingleAppCapture: Boolean) {
        changeSettings { current -> current.copy(hidesSingleAppCapture = hidesSingleAppCapture) }
    }

    override fun setAudioFormat(family: ProtocolFamily, format: StreamAudioFormat) {
        changeSettings { current -> current.withAudioFormat(family, format) }
    }

    override fun setAirPlayAudioDelayMillis(delayMillis: Int) {
        changeSettings { current -> current.copy(airPlayAudioDelayMillis = delayMillis) }
    }

    override fun setDisconnectsChromecastOnMute(disconnectsOnMute: Boolean) {
        changeSettings { current -> current.copy(disconnectsChromecastOnMute = disconnectsOnMute) }
    }

    override fun setThemeMode(themeMode: ThemeMode) {
        changeSettings { current -> current.copy(themeMode = themeMode) }
    }

    override fun setMutesPhoneWhileStreaming(mutesPhone: Boolean) {
        changeSettings { current -> current.copy(mutesPhoneWhileStreaming = mutesPhone) }
    }

    override fun setReconnectsDroppedDevices(reconnects: Boolean) {
        changeSettings { current -> current.copy(reconnectsDroppedDevices = reconnects) }
    }

    override fun setAacQuality(quality: AacQuality) {
        changeSettings { current -> current.copy(aacQuality = quality) }
    }

    override fun setStopsStreamingAfterSilence(timeout: SilenceTimeout) {
        changeSettings { current -> current.copy(stopsStreamingAfterSilence = timeout) }
    }

    override fun resetToDefaults() {
        settings.value = AppSettings()
        preferences.edit().clear().apply()
    }

    private fun changeSettings(change: (AppSettings) -> AppSettings) {
        settings.update(change)
        writeToPreferences(settings.value)
    }

    private fun readFromPreferences(): AppSettings {
        val defaults = AppSettings()
        val disabledProtocolNames = preferences.getStringSet(DISABLED_PROTOCOLS_KEY, emptySet()).orEmpty()
        val audioFormatEntries = preferences.getStringSet(AUDIO_FORMATS_KEY, emptySet()).orEmpty()
        return AppSettings(
            disabledProtocols = decodeDisabledProtocols(disabledProtocolNames),
            hidesSingleAppCapture = preferences.getBoolean(HIDES_SINGLE_APP_CAPTURE_KEY, defaults.hidesSingleAppCapture),
            audioFormats = decodeAudioFormats(audioFormatEntries),
            airPlayAudioDelayMillis = preferences.getInt(AIRPLAY_AUDIO_DELAY_MILLIS_KEY, defaults.airPlayAudioDelayMillis),
            disconnectsChromecastOnMute = preferences.getBoolean(
                DISCONNECTS_CHROMECAST_ON_MUTE_KEY,
                defaults.disconnectsChromecastOnMute,
            ),
            themeMode = decodeEnum(preferences.getString(THEME_MODE_KEY, null), defaults.themeMode),
            mutesPhoneWhileStreaming = preferences.getBoolean(
                MUTES_PHONE_WHILE_STREAMING_KEY,
                defaults.mutesPhoneWhileStreaming,
            ),
            reconnectsDroppedDevices = preferences.getBoolean(
                RECONNECTS_DROPPED_DEVICES_KEY,
                defaults.reconnectsDroppedDevices,
            ),
            aacQuality = decodeEnum(preferences.getString(AAC_QUALITY_KEY, null), defaults.aacQuality),
            stopsStreamingAfterSilence = decodeEnum(
                preferences.getString(STOPS_STREAMING_AFTER_SILENCE_KEY, null),
                defaults.stopsStreamingAfterSilence,
            ),
        )
    }

    private fun writeToPreferences(settings: AppSettings) {
        preferences.edit()
            .putStringSet(DISABLED_PROTOCOLS_KEY, encodeDisabledProtocols(settings.disabledProtocols))
            .putBoolean(HIDES_SINGLE_APP_CAPTURE_KEY, settings.hidesSingleAppCapture)
            .putStringSet(AUDIO_FORMATS_KEY, encodeAudioFormats(settings.audioFormats))
            .putInt(AIRPLAY_AUDIO_DELAY_MILLIS_KEY, settings.airPlayAudioDelayMillis)
            .putBoolean(DISCONNECTS_CHROMECAST_ON_MUTE_KEY, settings.disconnectsChromecastOnMute)
            .putString(THEME_MODE_KEY, settings.themeMode.name)
            .putBoolean(MUTES_PHONE_WHILE_STREAMING_KEY, settings.mutesPhoneWhileStreaming)
            .putBoolean(RECONNECTS_DROPPED_DEVICES_KEY, settings.reconnectsDroppedDevices)
            .putString(AAC_QUALITY_KEY, settings.aacQuality.name)
            .putString(STOPS_STREAMING_AFTER_SILENCE_KEY, settings.stopsStreamingAfterSilence.name)
            .apply()
    }
}
