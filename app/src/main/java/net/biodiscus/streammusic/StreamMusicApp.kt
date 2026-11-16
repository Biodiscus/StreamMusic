package net.biodiscus.streammusic

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import net.biodiscus.streammusic.feature.devices.DevicesDestination
import net.biodiscus.streammusic.feature.devices.devicesScreen
import net.biodiscus.streammusic.feature.streaming.StreamingDestination
import net.biodiscus.streammusic.feature.streaming.navigateToStreaming
import net.biodiscus.streammusic.feature.streaming.streamingScreen

@Composable
internal fun StreamMusicApp(isStreamingAlready: Boolean) {
    val navController = rememberNavController()
    val startDestination: Any = if (isStreamingAlready) StreamingDestination else DevicesDestination

    NavHost(navController = navController, startDestination = startDestination) {
        devicesScreen(onStreamingStarted = navController::navigateToStreaming)
        streamingScreen(
            onStreamingEnded = { navController.popBackStack(DevicesDestination, inclusive = false) },
        )
    }
}
