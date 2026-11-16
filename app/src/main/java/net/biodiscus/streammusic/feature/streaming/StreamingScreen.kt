package net.biodiscus.streammusic.feature.streaming

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.biodiscus.streammusic.R
import net.biodiscus.streammusic.core.model.DeviceProtocol
import net.biodiscus.streammusic.core.model.DeviceStreamStatus
import net.biodiscus.streammusic.core.model.StreamingDevice
import net.biodiscus.streammusic.core.streaming.service.StreamingServiceCommands
import net.biodiscus.streammusic.ui.ThemePreviews
import net.biodiscus.streammusic.ui.theme.StreamMusicTheme

@Composable
internal fun StreamingRoute(
    onStreamingEnded: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StreamingViewModel = hiltViewModel(),
) {
    val context: Context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    StreamingScreen(
        uiState = uiState,
        onStopClicked = {
            StreamingServiceCommands.stop(context)
            onStreamingEnded()
        },
        onBackToDevicesClicked = onStreamingEnded,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StreamingScreen(
    uiState: StreamingUiState,
    onStopClicked: () -> Unit,
    onBackToDevicesClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.streaming_title)) }) },
    ) { contentPadding ->
        Column(Modifier.padding(contentPadding).fillMaxSize()) {
            when (uiState) {
                StreamingUiState.NotStreaming -> NotStreamingContent(onBackToDevicesClicked)
                is StreamingUiState.Streaming -> StreamingContent(uiState, onStopClicked)
            }
        }
    }
}

@Composable
private fun NotStreamingContent(onBackToDevicesClicked: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.streaming_stopped), style = MaterialTheme.typography.titleMedium)
        Button(onClick = onBackToDevicesClicked, modifier = Modifier.padding(top = 16.dp)) {
            Text(stringResource(R.string.streaming_back_to_devices))
        }
    }
}

@Composable
private fun ColumnScope.StreamingContent(uiState: StreamingUiState.Streaming, onStopClicked: () -> Unit) {
    LazyColumn(Modifier.weight(1f)) {
        items(items = uiState.rows, key = { row -> row.device.id }) { row -> DeviceStatusItem(row) }
    }
    Button(
        onClick = onStopClicked,
        modifier = Modifier.fillMaxWidth().padding(16.dp),
    ) {
        Text(stringResource(R.string.streaming_stop))
    }
}

@Composable
private fun DeviceStatusItem(row: DeviceStatusRow) {
    ListItem(
        headlineContent = { Text(row.device.name) },
        supportingContent = { Text(statusText(row.status)) },
        trailingContent = { StatusIcon(row.status) },
    )
}

@Composable
private fun statusText(status: DeviceStreamStatus): String =
    when (status) {
        DeviceStreamStatus.Connecting -> stringResource(R.string.streaming_status_connecting)
        DeviceStreamStatus.Streaming -> stringResource(R.string.streaming_status_streaming)
        is DeviceStreamStatus.Failed -> stringResource(R.string.streaming_status_failed, status.reason)
    }

@Composable
private fun StatusIcon(status: DeviceStreamStatus) {
    when (status) {
        DeviceStreamStatus.Connecting -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
        DeviceStreamStatus.Streaming -> Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
        is DeviceStreamStatus.Failed -> Icon(Icons.Filled.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
    }
}

@ThemePreviews
@Composable
private fun StreamingScreenPreview() {
    StreamMusicTheme {
        StreamingScreen(
            uiState = StreamingUiState.Streaming(
                listOf(
                    DeviceStatusRow(StreamingDevice("Kitchen", DeviceProtocol.SONOS, "192.168.1.20", 1400), DeviceStreamStatus.Streaming),
                    DeviceStatusRow(StreamingDevice("TV", DeviceProtocol.CHROMECAST, "192.168.1.30", 8009), DeviceStreamStatus.Connecting),
                    DeviceStatusRow(StreamingDevice("Study", DeviceProtocol.DLNA, "192.168.1.31", 80), DeviceStreamStatus.Failed("Connection refused")),
                ),
            ),
            onStopClicked = {},
            onBackToDevicesClicked = {},
        )
    }
}
