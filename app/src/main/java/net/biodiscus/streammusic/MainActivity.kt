package net.biodiscus.streammusic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import net.biodiscus.streammusic.core.model.StreamingState
import net.biodiscus.streammusic.core.streaming.StreamingStateRepository
import net.biodiscus.streammusic.ui.theme.StreamMusicTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var streamingStateRepository: StreamingStateRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val isStreamingAlready = streamingStateRepository.state.value is StreamingState.Active
        setContent {
            StreamMusicTheme {
                StreamMusicApp(isStreamingAlready)
            }
        }
    }
}
