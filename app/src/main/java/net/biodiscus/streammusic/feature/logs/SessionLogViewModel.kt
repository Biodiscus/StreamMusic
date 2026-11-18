package net.biodiscus.streammusic.feature.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import net.biodiscus.streammusic.core.logging.LogLevel
import net.biodiscus.streammusic.core.logging.SessionLog

private const val STOP_TIMEOUT_MILLIS = 5_000L

@HiltViewModel
internal class SessionLogViewModel @Inject constructor(
    sessionLog: SessionLog,
) : ViewModel() {

    private val shownLevels = MutableStateFlow(LogLevel.entries.toSet())

    val uiState: StateFlow<SessionLogUiState> =
        combine(sessionLog.entries, shownLevels, ::SessionLogUiState)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = SessionLogUiState(),
            )

    fun onLevelToggled(level: LogLevel) {
        shownLevels.update { levels -> levels.withLevelToggled(level) }
    }
}
