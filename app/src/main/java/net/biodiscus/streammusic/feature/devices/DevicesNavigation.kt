package net.biodiscus.streammusic.feature.devices

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
internal data object DevicesDestination

internal fun NavGraphBuilder.devicesScreen(onStreamingStarted: () -> Unit) {
    composable<DevicesDestination> {
        DevicesRoute(onStreamingStarted = onStreamingStarted)
    }
}
