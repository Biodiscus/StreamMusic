package net.biodiscus.streammusic.feature.devices

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import net.biodiscus.streammusic.core.model.DeviceStreamStatus
import net.biodiscus.streammusic.core.model.NetworkDevice
import net.biodiscus.streammusic.core.model.StreamingDevice
import net.biodiscus.streammusic.core.model.groupIntoNetworkDevices
import net.biodiscus.streammusic.ui.ThemePreviews
import net.biodiscus.streammusic.ui.theme.StreamMusicTheme

@Composable
internal fun DevicesRoute(
    onSettingsClicked: () -> Unit,
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
    )

    DevicesScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onDeviceClicked = viewModel::onDeviceClicked,
        onProtocolChosen = viewModel::onProtocolChosen,
        onRefreshClicked = viewModel::onRefreshClicked,
        onSettingsClicked = onSettingsClicked,
        onContinueClicked = startStreaming,
        onStopClicked = viewModel::onStopClicked,
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
    onDeviceClicked: (NetworkDevice) -> Unit,
    onProtocolChosen: (NetworkDevice, DeviceProtocol) -> Unit,
    onRefreshClicked: () -> Unit,
    onSettingsClicked: () -> Unit,
    onContinueClicked: (Set<String>) -> Unit,
    onStopClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var protocolChoiceDeviceId by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onRefreshClicked) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.devices_search_again))
                    }
                    IconButton(onClick = onSettingsClicked) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.devices_settings))
                    }
                },
            )
        },
        floatingActionButton = { StreamingButton(uiState, onContinueClicked, onStopClicked) },
    ) { contentPadding ->
        Column(Modifier.padding(contentPadding)) {
            if (uiState is DevicesUiState.DevicesFound) {
                DeviceList(uiState, onDeviceClicked, onChooseProtocolClicked = { device -> protocolChoiceDeviceId = device.id })
            }
        }
    }

    if (uiState is DevicesUiState.DevicesFound) {
        val deviceToChooseFor = uiState.devices.firstOrNull { device -> device.id == protocolChoiceDeviceId }
        if (deviceToChooseFor != null) {
            ProtocolChoiceDialog(
                device = deviceToChooseFor,
                protocolInUse = uiState.streamingOptionOf(deviceToChooseFor)?.protocol,
                onProtocolChosen = { protocol ->
                    onProtocolChosen(deviceToChooseFor, protocol)
                    protocolChoiceDeviceId = null
                },
                onDismiss = { protocolChoiceDeviceId = null },
            )
        }
    }
}

@Composable
private fun StreamingButton(
    uiState: DevicesUiState,
    onContinueClicked: (Set<String>) -> Unit,
    onStopClicked: () -> Unit,
) {
    if (uiState.isStreaming) {
        ExtendedFloatingActionButton(onClick = onStopClicked) {
            Text(stringResource(R.string.devices_stop))
        }
    } else if (uiState is DevicesUiState.DevicesFound && uiState.selectedCount > 0) {
        ExtendedFloatingActionButton(onClick = { onContinueClicked(uiState.selectedStreamingDeviceIds) }) {
            Text(pluralStringResource(R.plurals.devices_continue, uiState.selectedCount, uiState.selectedCount))
        }
    }
}

@Composable
private fun DeviceList(
    uiState: DevicesUiState.DevicesFound,
    onDeviceClicked: (NetworkDevice) -> Unit,
    onChooseProtocolClicked: (NetworkDevice) -> Unit,
) {
    LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
        items(items = uiState.devices, key = { device -> device.id }) { device ->
            DeviceRow(
                device = device,
                isSelected = device.id in uiState.selectedDeviceIds,
                protocolInUse = uiState.streamingOptionOf(device)?.protocol,
                streamStatus = uiState.streamStatusByDeviceId[device.id],
                onClick = { onDeviceClicked(device) },
                onChooseProtocolClicked = { onChooseProtocolClicked(device) },
            )
        }
    }
}

@Composable
internal fun DeviceRow(
    device: NetworkDevice,
    isSelected: Boolean,
    protocolInUse: DeviceProtocol?,
    streamStatus: DeviceStreamStatus?,
    onClick: () -> Unit,
    onChooseProtocolClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rowModifier = if (device.canReceiveStream) modifier.clickable(onClick = onClick) else modifier
    ListItem(
        modifier = rowModifier,
        headlineContent = { Text(device.name) },
        supportingContent = { Text(deviceDescription(device, protocolInUse, streamStatus)) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (streamStatus != null) StreamStatusIcon(streamStatus)
                if (canChooseProtocol(device, streamStatus)) {
                    IconButton(onClick = onChooseProtocolClicked) {
                        Icon(Icons.Filled.Tune, contentDescription = stringResource(R.string.devices_choose_protocol))
                    }
                }
                Checkbox(checked = isSelected, onCheckedChange = null, enabled = device.canReceiveStream)
            }
        },
    )
}

@Composable
private fun deviceDescription(
    device: NetworkDevice,
    protocolInUse: DeviceProtocol?,
    streamStatus: DeviceStreamStatus?,
): String {
    if (protocolInUse != null && streamStatus != null) {
        return "${protocolInUse.displayName} · ${device.host}\n${streamStatusText(streamStatus)}"
    }
    if (protocolInUse != null) return "${protocolInUse.displayName} · ${device.host}"
    val protocolNames = device.protocols.joinToString(" · ") { protocol -> protocol.displayName }
    return "$protocolNames · ${device.host}\n${stringResource(R.string.devices_discovery_only)}"
}

@Composable
private fun streamStatusText(status: DeviceStreamStatus): String =
    when (status) {
        DeviceStreamStatus.Connecting -> stringResource(R.string.devices_status_connecting)
        DeviceStreamStatus.Streaming -> stringResource(R.string.devices_status_playing)
        is DeviceStreamStatus.Failed -> stringResource(R.string.devices_status_failed, status.reason)
    }

@Composable
private fun StreamStatusIcon(status: DeviceStreamStatus) {
    when (status) {
        DeviceStreamStatus.Connecting -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
        DeviceStreamStatus.Streaming -> Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
        is DeviceStreamStatus.Failed -> Icon(Icons.Filled.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun ProtocolChoiceDialog(
    device: NetworkDevice,
    protocolInUse: DeviceProtocol?,
    onProtocolChosen: (DeviceProtocol) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.devices_choose_protocol_title, device.name)) },
        text = {
            Column {
                device.streamingOptions.forEach { option ->
                    ListItem(
                        modifier = Modifier.clickable { onProtocolChosen(option.protocol) },
                        headlineContent = { Text(option.protocol.displayName) },
                        trailingContent = { RadioButton(selected = option.protocol == protocolInUse, onClick = null) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.devices_choose_protocol_cancel)) }
        },
    )
}

@ThemePreviews
@Composable
private fun DevicesScreenPreview() {
    StreamMusicTheme {
        DevicesScreen(
            snackbarHostState = remember { SnackbarHostState() },
            uiState = DevicesUiState.DevicesFound(
                devices = previewDevices.groupIntoNetworkDevices(),
                selectedDeviceIds = setOf("192.168.1.20"),
            ),
            onDeviceClicked = {},
            onProtocolChosen = { _, _ -> },
            onRefreshClicked = {},
            onSettingsClicked = {},
            onContinueClicked = {},
            onStopClicked = {},
        )
    }
}

@ThemePreviews
@Composable
private fun DevicesScreenStreamingPreview() {
    StreamMusicTheme {
        DevicesScreen(
            snackbarHostState = remember { SnackbarHostState() },
            uiState = DevicesUiState.DevicesFound(
                devices = previewDevices.groupIntoNetworkDevices(),
                selectedDeviceIds = setOf("192.168.1.20", "192.168.1.30"),
                streamStatusByDeviceId = mapOf(
                    "192.168.1.20" to DeviceStreamStatus.Streaming,
                    "192.168.1.30" to DeviceStreamStatus.Failed("Connection refused"),
                ),
                isStreaming = true,
            ),
            onDeviceClicked = {},
            onProtocolChosen = { _, _ -> },
            onRefreshClicked = {},
            onSettingsClicked = {},
            onContinueClicked = {},
            onStopClicked = {},
        )
    }
}

private val previewDevices = listOf(
    StreamingDevice("Kitchen", DeviceProtocol.SONOS, "192.168.1.20", 1400),
    StreamingDevice("Kitchen", DeviceProtocol.DLNA, "192.168.1.20", 1400),
    StreamingDevice("Living room TV", DeviceProtocol.CHROMECAST, "192.168.1.30", 8009),
    StreamingDevice("HomePod", DeviceProtocol.AIRPLAY, "192.168.1.40", 7000),
)
