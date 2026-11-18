package net.biodiscus.streammusic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.biodiscus.streammusic.core.model.ThemeMode
import net.biodiscus.streammusic.core.settings.SettingsRepository

private const val STOP_TIMEOUT_MILLIS = 5_000L

@HiltViewModel
internal class MainViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
) : ViewModel() {

    /** Starts with the saved theme, so the first frame already has the right colors. */
    val themeMode: StateFlow<ThemeMode> =
        settingsRepository.observeSettings()
            .map { settings -> settings.themeMode }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = settingsRepository.currentSettings.themeMode,
            )
}
