package net.biodiscus.streammusic.feature.logs

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
internal data object SessionLogDestination

internal fun NavGraphBuilder.sessionLogScreen(onBackClicked: () -> Unit) {
    composable<SessionLogDestination> {
        SessionLogRoute(onBackClicked = onBackClicked)
    }
}
