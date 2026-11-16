package net.biodiscus.streammusic.feature.debug

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.biodiscus.streammusic.R
import net.biodiscus.streammusic.core.model.DeviceProtocol
import net.biodiscus.streammusic.core.model.StreamingDevice
import net.biodiscus.streammusic.ui.ThemePreviews
import net.biodiscus.streammusic.ui.theme.StreamMusicTheme

@Composable
internal fun DiscoveryDebugRoute(
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DiscoveryDebugViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DiscoveryDebugScreen(
        uiState = uiState,
        onBackClicked = onBackClicked,
        onImplementationFilterChosen = viewModel::onImplementationFilterChosen,
        onProtocolFilterToggled = viewModel::onProtocolFilterToggled,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DiscoveryDebugScreen(
    uiState: DiscoveryDebugUiState,
    onBackClicked: () -> Unit,
    onImplementationFilterChosen: (ImplementationFilter) -> Unit,
    onProtocolFilterToggled: (DeviceProtocol) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.debug_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.settings_back))
                    }
                },
            )
        },
    ) { contentPadding ->
        Column(Modifier.padding(contentPadding).fillMaxSize()) {
            ImplementationFilterRow(uiState.filter.implementation, onImplementationFilterChosen)
            ProtocolFilterRow(uiState, onProtocolFilterToggled)
            FindCount(uiState)
            CategoryList(uiState.categories)
        }
    }
}

@Composable
private fun ImplementationFilterRow(
    chosen: ImplementationFilter,
    onChosen: (ImplementationFilter) -> Unit,
) {
    ChipRow {
        ImplementationFilter.entries.forEach { implementation ->
            FilterChip(
                selected = implementation == chosen,
                onClick = { onChosen(implementation) },
                label = { Text(stringResource(implementation.labelResource())) },
            )
        }
    }
}

@Composable
private fun ProtocolFilterRow(
    uiState: DiscoveryDebugUiState,
    onProtocolToggled: (DeviceProtocol) -> Unit,
) {
    ChipRow {
        uiState.protocolChoices.forEach { protocol ->
            FilterChip(
                selected = protocol in uiState.filter.protocols,
                onClick = { onProtocolToggled(protocol) },
                label = { Text(protocol.displayName) },
            )
        }
    }
}

@Composable
private fun ChipRow(chips: @Composable () -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        chips()
    }
}

@Composable
private fun FindCount(uiState: DiscoveryDebugUiState) {
    val text = if (uiState.isSearching) {
        stringResource(R.string.debug_searching)
    } else {
        stringResource(R.string.debug_find_count, uiState.shownFindCount, uiState.totalFindCount)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun CategoryList(categories: List<DebugCategory>) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        categories.forEach { category ->
            item(key = category.protocol.name) { CategoryHeader(category) }
            items(items = category.finds, key = { find -> find.id }) { find -> FindCard(find) }
        }
    }
}

@Composable
private fun CategoryHeader(category: DebugCategory) {
    val statusResource = if (category.isImplemented) R.string.debug_implemented else R.string.debug_not_implemented
    Column {
        Text(category.protocol.displayName, style = MaterialTheme.typography.titleLarge)
        Text(stringResource(statusResource), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun FindCard(find: StreamingDevice) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(find.name, style = MaterialTheme.typography.titleMedium)
            DetailLine(stringResource(R.string.debug_transport, find.protocol.transport.name))
            DetailLine(stringResource(R.string.debug_address, find.host, find.port))
            DetailLine(stringResource(R.string.debug_id, find.id))
            ControlUrlLine(find.avTransportControlUrl)
        }
    }
}

@Composable
private fun ControlUrlLine(controlUrl: String?) {
    if (controlUrl == null) return
    DetailLine(stringResource(R.string.debug_control_url, controlUrl))
}

@Composable
private fun DetailLine(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall)
}

private fun ImplementationFilter.labelResource(): Int =
    when (this) {
        ImplementationFilter.ALL -> R.string.debug_filter_all
        ImplementationFilter.IMPLEMENTED -> R.string.debug_implemented
        ImplementationFilter.NOT_IMPLEMENTED -> R.string.debug_not_implemented
    }

private val previewFinds = listOf(
    StreamingDevice("Living Room TV", DeviceProtocol.CHROMECAST, "192.168.1.20", 8009),
    StreamingDevice(
        name = "Living Room TV",
        protocol = DeviceProtocol.DLNA,
        host = "192.168.1.20",
        port = 9197,
        avTransportControlUrl = "http://192.168.1.20:9197/upnp/control/AVTransport1",
    ),
    StreamingDevice("HomePod", DeviceProtocol.AIRPLAY, "192.168.1.30", 7000),
)

@ThemePreviews
@Composable
private fun DiscoveryDebugScreenPreview() {
    StreamMusicTheme {
        DiscoveryDebugScreen(
            uiState = DiscoveryDebugUiState(finds = previewFinds),
            onBackClicked = {},
            onImplementationFilterChosen = {},
            onProtocolFilterToggled = {},
        )
    }
}

@ThemePreviews
@Composable
private fun DiscoveryDebugScreenNotImplementedOnlyPreview() {
    StreamMusicTheme {
        DiscoveryDebugScreen(
            uiState = DiscoveryDebugUiState(
                finds = previewFinds,
                filter = DiscoveryDebugFilter(implementation = ImplementationFilter.NOT_IMPLEMENTED),
            ),
            onBackClicked = {},
            onImplementationFilterChosen = {},
            onProtocolFilterToggled = {},
        )
    }
}

@ThemePreviews
@Composable
private fun DiscoveryDebugScreenSearchingPreview() {
    StreamMusicTheme {
        DiscoveryDebugScreen(
            uiState = DiscoveryDebugUiState(),
            onBackClicked = {},
            onImplementationFilterChosen = {},
            onProtocolFilterToggled = {},
        )
    }
}
