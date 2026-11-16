package net.biodiscus.streammusic.feature.devices

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import net.biodiscus.streammusic.R
import net.biodiscus.streammusic.core.model.DeviceProtocol
import net.biodiscus.streammusic.core.model.StreamingDevice
import net.biodiscus.streammusic.ui.ThemePreviews
import net.biodiscus.streammusic.ui.theme.StreamMusicTheme

@Composable
internal fun DevicesRoute(
    onStreamingStarted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DevicesViewModel = hiltViewModel(),
) {
    val localNetworkPermission = rememberLocalNetworkPermission()
    if (!localNetworkPermission.isGranted) {
        LocalNetworkPermissionScreen(onAllowClicked = localNetworkPermission.request, modifier = modifier)
        return
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val recordAudioDeniedMessage = stringResource(R.string.devices_record_audio_denied)

    val startStreaming = rememberStreamStarter(
        onRecordAudioDenied = {
            coroutineScope.launch { snackbarHostState.showSnackbar(recordAudioDeniedMessage) }
        },
        onStreamingStarted = onStreamingStarted,
    )

    DevicesScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onDeviceClicked = viewModel::onDeviceClicked,
        onRefreshClicked = viewModel::onRefreshClicked,
        onContinueClicked = startStreaming,
        modifier = modifier,
    )
}

@Composable
internal fun LocalNetworkPermissionScreen(
    onAllowClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.devices_permission_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.padding(8.dp))
        Text(stringResource(R.string.devices_permission_body), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.padding(12.dp))
        Button(onClick = onAllowClicked) { Text(stringResource(R.string.devices_permission_button)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DevicesScreen(
    uiState: DevicesUiState,
    snackbarHostState: SnackbarHostState,
    onDeviceClicked: (StreamingDevice) -> Unit,
    onRefreshClicked: () -> Unit,
    onContinueClicked: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.devices_title)) },
                actions = {
                    IconButton(onClick = onRefreshClicked) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.devices_search_again))
                    }
                },
            )
        },
        floatingActionButton = {
            if (uiState is DevicesUiState.DevicesFound && uiState.selectedCount > 0) {
                ExtendedFloatingActionButton(onClick = { onContinueClicked(uiState.selectedDeviceIds) }) {
                    Text(pluralStringResource(R.plurals.devices_continue, uiState.selectedCount, uiState.selectedCount))
                }
            }
        },
    ) { contentPadding ->
        Column(Modifier.padding(contentPadding)) {
            SearchingIndicator()
            if (uiState is DevicesUiState.DevicesFound) {
                DeviceList(uiState, onDeviceClicked)
            }
        }
    }
}

@Composable
private fun SearchingIndicator() {
    LinearProgressIndicator(Modifier.fillMaxWidth())
    Text(
        text = stringResource(R.string.devices_searching),
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun DeviceList(
    uiState: DevicesUiState.DevicesFound,
    onDeviceClicked: (StreamingDevice) -> Unit,
) {
    LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
        items(items = uiState.devices, key = { device -> device.id }) { device ->
            DeviceRow(
                device = device,
                isSelected = device.id in uiState.selectedDeviceIds,
                onClick = { onDeviceClicked(device) },
            )
        }
    }
}

@Composable
internal fun DeviceRow(
    device: StreamingDevice,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rowModifier = if (device.canReceiveStream) modifier.clickable(onClick = onClick) else modifier
    ListItem(
        modifier = rowModifier,
        headlineContent = { Text(device.name) },
        supportingContent = { Text(deviceDescription(device)) },
        trailingContent = {
            Checkbox(checked = isSelected, onCheckedChange = null, enabled = device.canReceiveStream)
        },
    )
}

@Composable
private fun deviceDescription(device: StreamingDevice): String {
    val protocolAndAddress = "${device.protocol.displayName} · ${device.host}"
    return if (device.canReceiveStream) {
        protocolAndAddress
    } else {
        "$protocolAndAddress\n${stringResource(R.string.devices_discovery_only)}"
    }
}

@ThemePreviews
@Composable
private fun DevicesScreenPreview() {
    StreamMusicTheme {
        DevicesScreen(
            snackbarHostState = remember { SnackbarHostState() },
            uiState = DevicesUiState.DevicesFound(
                devices = previewDevices,
                selectedDeviceIds = setOf(previewDevices.first().id),
            ),
            onDeviceClicked = {},
            onRefreshClicked = {},
            onContinueClicked = {},
        )
    }
}

private val previewDevices = listOf(
    StreamingDevice("Kitchen", DeviceProtocol.SONOS, "192.168.1.20", 1400),
    StreamingDevice("Living room TV", DeviceProtocol.CHROMECAST, "192.168.1.30", 8009),
    StreamingDevice("HomePod", DeviceProtocol.AIRPLAY, "192.168.1.40", 7000),
)
