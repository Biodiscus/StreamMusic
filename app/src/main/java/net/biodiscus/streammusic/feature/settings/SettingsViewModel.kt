package net.biodiscus.streammusic.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import net.biodiscus.streammusic.core.model.AppSettings
import net.biodiscus.streammusic.core.model.ProtocolFamily
import net.biodiscus.streammusic.core.model.StreamAudioFormat
import net.biodiscus.streammusic.core.settings.SettingsRepository

private const val STOP_TIMEOUT_MILLIS = 5_000L

@HiltViewModel
internal class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> =
        settingsRepository.observeSettings().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = AppSettings(),
        )

    fun onProtocolFamilyToggled(family: ProtocolFamily, isEnabled: Boolean) {
        settingsRepository.setProtocolFamilyEnabled(family, isEnabled)
    }

    fun onHidesSingleAppCaptureChanged(hidesSingleAppCapture: Boolean) {
        settingsRepository.setHidesSingleAppCapture(hidesSingleAppCapture)
    }

    fun onAudioFormatChosen(family: ProtocolFamily, format: StreamAudioFormat) {
        settingsRepository.setAudioFormat(family, format)
    }

    fun onAirPlayAudioDelayChanged(delayMillis: Int) {
        settingsRepository.setAirPlayAudioDelayMillis(delayMillis)
    }

    fun onDisconnectsChromecastOnMuteChanged(disconnectsOnMute: Boolean) {
        settingsRepository.setDisconnectsChromecastOnMute(disconnectsOnMute)
    }
}
