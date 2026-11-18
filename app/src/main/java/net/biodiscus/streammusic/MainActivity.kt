package net.biodiscus.streammusic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import net.biodiscus.streammusic.core.model.usesDarkTheme
import net.biodiscus.streammusic.ui.theme.StreamMusicTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            StreamMusicTheme(darkTheme = themeMode.usesDarkTheme(isSystemDark = isSystemInDarkTheme())) {
                StreamMusicApp()
            }
        }
    }
}
