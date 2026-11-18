package net.biodiscus.streammusic.feature.settings

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
internal data object SettingsDestination

internal fun NavGraphBuilder.settingsScreen(
    onBackClicked: () -> Unit,
    onDiscoveryDebugClicked: () -> Unit,
    onSessionLogClicked: () -> Unit,
) {
    composable<SettingsDestination> {
        SettingsRoute(
            onBackClicked = onBackClicked,
            onDiscoveryDebugClicked = onDiscoveryDebugClicked,
            onSessionLogClicked = onSessionLogClicked,
        )
    }
}
