package net.biodiscus.streammusic.feature.debug

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
import net.biodiscus.streammusic.core.discovery.DeviceRepository
import net.biodiscus.streammusic.core.model.DeviceProtocol

private const val STOP_TIMEOUT_MILLIS = 5_000L

@HiltViewModel
internal class DiscoveryDebugViewModel @Inject constructor(
    deviceRepository: DeviceRepository,
) : ViewModel() {

    private val filter = MutableStateFlow(DiscoveryDebugFilter())

    val uiState: StateFlow<DiscoveryDebugUiState> =
        combine(deviceRepository.observeDevices(), filter, ::DiscoveryDebugUiState)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = DiscoveryDebugUiState(),
            )

    fun onImplementationFilterChosen(implementation: ImplementationFilter) {
        filter.update { current -> current.copy(implementation = implementation) }
    }

    fun onProtocolFilterToggled(protocol: DeviceProtocol) {
        filter.update { current -> current.withProtocolToggled(protocol) }
    }
}
