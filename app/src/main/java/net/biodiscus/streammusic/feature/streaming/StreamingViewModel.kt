package net.biodiscus.streammusic.feature.streaming

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.biodiscus.streammusic.core.streaming.StreamingStateRepository

private const val STOP_TIMEOUT_MILLIS = 5_000L

@HiltViewModel
internal class StreamingViewModel @Inject constructor(
    stateRepository: StreamingStateRepository,
) : ViewModel() {

    val uiState: StateFlow<StreamingUiState> =
        stateRepository.state
            .map(::toStreamingUiState)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = toStreamingUiState(stateRepository.state.value),
            )
}
