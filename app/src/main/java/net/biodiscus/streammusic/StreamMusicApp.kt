package net.biodiscus.streammusic

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import net.biodiscus.streammusic.feature.debug.DiscoveryDebugDestination
import net.biodiscus.streammusic.feature.debug.discoveryDebugScreen
import net.biodiscus.streammusic.feature.devices.DevicesDestination
import net.biodiscus.streammusic.feature.devices.devicesScreen
import net.biodiscus.streammusic.feature.logs.SessionLogDestination
import net.biodiscus.streammusic.feature.logs.sessionLogScreen
import net.biodiscus.streammusic.feature.settings.SettingsDestination
import net.biodiscus.streammusic.feature.settings.settingsScreen

@Composable
internal fun StreamMusicApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = DevicesDestination) {
        devicesScreen(onSettingsClicked = { navController.navigate(SettingsDestination) })
        settingsScreen(
            onBackClicked = { navController.popBackStack() },
            onDiscoveryDebugClicked = { navController.navigate(DiscoveryDebugDestination) },
            onSessionLogClicked = { navController.navigate(SessionLogDestination) },
        )
        discoveryDebugScreen(onBackClicked = { navController.popBackStack() })
        sessionLogScreen(onBackClicked = { navController.popBackStack() })
    }
}
