package net.biodiscus.streammusic

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import net.biodiscus.streammusic.feature.devices.DevicesDestination
import net.biodiscus.streammusic.feature.devices.devicesScreen
import net.biodiscus.streammusic.feature.settings.SettingsDestination
import net.biodiscus.streammusic.feature.settings.settingsScreen

@Composable
internal fun StreamMusicApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = DevicesDestination) {
        devicesScreen(onSettingsClicked = { navController.navigate(SettingsDestination) })
        settingsScreen(onBackClicked = { navController.popBackStack() })
    }
}
