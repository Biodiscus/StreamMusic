package net.biodiscus.streammusic

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import net.biodiscus.streammusic.feature.devices.DevicesDestination
import net.biodiscus.streammusic.feature.devices.devicesScreen

@Composable
internal fun StreamMusicApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = DevicesDestination) {
        devicesScreen()
    }
}
