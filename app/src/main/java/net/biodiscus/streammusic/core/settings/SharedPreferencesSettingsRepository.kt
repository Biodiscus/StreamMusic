package net.biodiscus.streammusic.core.settings

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import net.biodiscus.streammusic.core.model.AppSettings
import net.biodiscus.streammusic.core.model.ProtocolFamily
import net.biodiscus.streammusic.core.model.StreamAudioFormat
import net.biodiscus.streammusic.core.model.withAudioFormat
import net.biodiscus.streammusic.core.model.withFamilyEnabled

private const val PREFERENCES_FILE_NAME = "settings"
private const val DISABLED_PROTOCOLS_KEY = "disabled_protocols"
private const val HIDES_SINGLE_APP_CAPTURE_KEY = "hides_single_app_capture"
private const val AUDIO_FORMATS_KEY = "audio_formats"
private const val AIRPLAY_AUDIO_DELAY_MILLIS_KEY = "airplay_audio_delay_millis"
private const val DISCONNECTS_CHROMECAST_ON_MUTE_KEY = "disconnects_chromecast_on_mute"

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
        )
    }

    private fun writeToPreferences(settings: AppSettings) {
        preferences.edit()
            .putStringSet(DISABLED_PROTOCOLS_KEY, encodeDisabledProtocols(settings.disabledProtocols))
            .putBoolean(HIDES_SINGLE_APP_CAPTURE_KEY, settings.hidesSingleAppCapture)
            .putStringSet(AUDIO_FORMATS_KEY, encodeAudioFormats(settings.audioFormats))
            .putInt(AIRPLAY_AUDIO_DELAY_MILLIS_KEY, settings.airPlayAudioDelayMillis)
            .putBoolean(DISCONNECTS_CHROMECAST_ON_MUTE_KEY, settings.disconnectsChromecastOnMute)
            .apply()
    }
}
