package net.biodiscus.streammusic.feature.debug

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
internal data object DiscoveryDebugDestination

internal fun NavGraphBuilder.discoveryDebugScreen(onBackClicked: () -> Unit) {
    composable<DiscoveryDebugDestination> {
        DiscoveryDebugRoute(onBackClicked = onBackClicked)
    }
}
