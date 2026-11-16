package net.biodiscus.streammusic.feature.streaming

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
internal data object StreamingDestination

internal fun NavController.navigateToStreaming() {
    navigate(StreamingDestination)
}

internal fun NavGraphBuilder.streamingScreen(onStreamingEnded: () -> Unit) {
    composable<StreamingDestination> {
        StreamingRoute(onStreamingEnded = onStreamingEnded)
    }
}
