package net.biodiscus.streammusic.feature.devices

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import net.biodiscus.streammusic.core.model.NetworkDevice
import net.biodiscus.streammusic.core.model.StreamingDevice
import net.biodiscus.streammusic.core.model.groupIntoNetworkDevices
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
        onProtocolChosen = viewModel::onProtocolChosen,
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
    onDeviceClicked: (NetworkDevice) -> Unit,
    onProtocolChosen: (NetworkDevice, DeviceProtocol) -> Unit,
    onRefreshClicked: () -> Unit,
    onContinueClicked: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var protocolChoiceDeviceId by rememberSaveable { mutableStateOf<String?>(null) }

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
                ExtendedFloatingActionButton(onClick = { onContinueClicked(uiState.selectedStreamingDeviceIds) }) {
                    Text(pluralStringResource(R.plurals.devices_continue, uiState.selectedCount, uiState.selectedCount))
                }
            }
        },
    ) { contentPadding ->
        Column(Modifier.padding(contentPadding)) {
            SearchingIndicator()
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
    onDeviceClicked: (NetworkDevice) -> Unit,
    onChooseProtocolClicked: (NetworkDevice) -> Unit,
) {
    LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
        items(items = uiState.devices, key = { device -> device.id }) { device ->
            DeviceRow(
                device = device,
                isSelected = device.id in uiState.selectedDeviceIds,
                protocolInUse = uiState.streamingOptionOf(device)?.protocol,
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
    onClick: () -> Unit,
    onChooseProtocolClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rowModifier = if (device.canReceiveStream) modifier.clickable(onClick = onClick) else modifier
    ListItem(
        modifier = rowModifier,
        headlineContent = { Text(device.name) },
        supportingContent = { Text(deviceDescription(device, protocolInUse)) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (device.streamingOptions.size > 1) {
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
private fun deviceDescription(device: NetworkDevice, protocolInUse: DeviceProtocol?): String {
    if (protocolInUse != null) return "${protocolInUse.displayName} · ${device.host}"
    val protocolNames = device.protocols.joinToString(" · ") { protocol -> protocol.displayName }
    return "$protocolNames · ${device.host}\n${stringResource(R.string.devices_discovery_only)}"
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
            onContinueClicked = {},
        )
    }
}

private val previewDevices = listOf(
    StreamingDevice("Kitchen", DeviceProtocol.SONOS, "192.168.1.20", 1400),
    StreamingDevice("Kitchen", DeviceProtocol.DLNA, "192.168.1.20", 1400),
    StreamingDevice("Living room TV", DeviceProtocol.CHROMECAST, "192.168.1.30", 8009),
    StreamingDevice("HomePod", DeviceProtocol.AIRPLAY, "192.168.1.40", 7000),
)
