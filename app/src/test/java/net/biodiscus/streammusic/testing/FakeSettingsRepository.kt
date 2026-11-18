package net.biodiscus.streammusic.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import net.biodiscus.streammusic.core.model.AacQuality
import net.biodiscus.streammusic.core.model.AppSettings
import net.biodiscus.streammusic.core.model.ProtocolFamily
import net.biodiscus.streammusic.core.model.SilenceTimeout
import net.biodiscus.streammusic.core.model.StreamAudioFormat
import net.biodiscus.streammusic.core.model.ThemeMode
import net.biodiscus.streammusic.core.model.withAudioFormat
import net.biodiscus.streammusic.core.model.withFamilyEnabled
import net.biodiscus.streammusic.core.settings.SettingsRepository

class FakeSettingsRepository : SettingsRepository {

    private val settings = MutableStateFlow(AppSettings())

    override val currentSettings: AppSettings
        get() = settings.value

    override fun observeSettings(): Flow<AppSettings> = settings

    override fun setProtocolFamilyEnabled(family: ProtocolFamily, isEnabled: Boolean) {
        settings.value = settings.value.withFamilyEnabled(family, isEnabled)
    }

    override fun setHidesSingleAppCapture(hidesSingleAppCapture: Boolean) {
        settings.value = settings.value.copy(hidesSingleAppCapture = hidesSingleAppCapture)
    }

    override fun setAudioFormat(family: ProtocolFamily, format: StreamAudioFormat) {
        settings.value = settings.value.withAudioFormat(family, format)
    }

    override fun setAirPlayAudioDelayMillis(delayMillis: Int) {
        settings.value = settings.value.copy(airPlayAudioDelayMillis = delayMillis)
    }

    override fun setDisconnectsChromecastOnMute(disconnectsOnMute: Boolean) {
        settings.value = settings.value.copy(disconnectsChromecastOnMute = disconnectsOnMute)
    }

    override fun setThemeMode(themeMode: ThemeMode) {
        settings.value = settings.value.copy(themeMode = themeMode)
    }

    override fun setMutesPhoneWhileStreaming(mutesPhone: Boolean) {
        settings.value = settings.value.copy(mutesPhoneWhileStreaming = mutesPhone)
    }

    override fun setReconnectsDroppedDevices(reconnects: Boolean) {
        settings.value = settings.value.copy(reconnectsDroppedDevices = reconnects)
    }

    override fun setAacQuality(quality: AacQuality) {
        settings.value = settings.value.copy(aacQuality = quality)
    }

    override fun setStopsStreamingAfterSilence(timeout: SilenceTimeout) {
        settings.value = settings.value.copy(stopsStreamingAfterSilence = timeout)
    }

    override fun resetToDefaults() {
        settings.value = AppSettings()
    }
}
