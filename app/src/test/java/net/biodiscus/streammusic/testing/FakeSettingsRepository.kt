package net.biodiscus.streammusic.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import net.biodiscus.streammusic.core.model.AppSettings
import net.biodiscus.streammusic.core.model.ProtocolFamily
import net.biodiscus.streammusic.core.model.StreamAudioFormat
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
}
