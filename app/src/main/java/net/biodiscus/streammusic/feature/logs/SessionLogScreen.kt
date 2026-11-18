package net.biodiscus.streammusic.feature.logs

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import net.biodiscus.streammusic.R
import net.biodiscus.streammusic.core.logging.LogEntry
import net.biodiscus.streammusic.core.logging.LogLevel
import net.biodiscus.streammusic.ui.ThemePreviews
import net.biodiscus.streammusic.ui.theme.StreamMusicTheme

private const val LOG_FILE_MIME_TYPE = "text/plain"

@Composable
internal fun SessionLogRoute(
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SessionLogViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val chooseFileToSaveLog = rememberLauncherForActivityResult(CreateDocument(LOG_FILE_MIME_TYPE)) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val wasSaved = saveTextToUri(context.contentResolver, uri, formatLogFileText(uiState.entries))
            val message = if (wasSaved) R.string.logs_saved else R.string.logs_save_failed
            snackbarHostState.showSnackbar(context.getString(message))
        }
    }
    SessionLogScreen(
        uiState = uiState,
        onBackClicked = onBackClicked,
        onLevelToggled = viewModel::onLevelToggled,
        onDownloadClicked = { chooseFileToSaveLog.launch(logFileName(System.currentTimeMillis())) },
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SessionLogScreen(
    uiState: SessionLogUiState,
    onBackClicked: () -> Unit,
    onLevelToggled: (LogLevel) -> Unit,
    onDownloadClicked: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.logs_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.settings_back))
                    }
                },
                actions = {
                    IconButton(onClick = onDownloadClicked, enabled = uiState.totalEntryCount > 0) {
                        Icon(Icons.Filled.Download, contentDescription = stringResource(R.string.logs_download))
                    }
                },
            )
        },
    ) { contentPadding ->
        Column(Modifier.padding(contentPadding).fillMaxSize()) {
            ShownLevelsRow(uiState.shownLevels, onLevelToggled)
            EmptyLogMessage(uiState)
            LogLines(uiState.shownEntries)
        }
    }
}

@Composable
private fun ShownLevelsRow(shownLevels: Set<LogLevel>, onLevelToggled: (LogLevel) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        LogLevel.entries.forEach { level ->
            ShownLevelCheckbox(
                label = stringResource(level.filterLabelResource()),
                isChecked = level in shownLevels,
                onCheckedChange = { onLevelToggled(level) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ShownLevelCheckbox(
    label: String,
    isChecked: Boolean,
    onCheckedChange: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(48.dp)
            .toggleable(value = isChecked, onValueChange = { onCheckedChange() }, role = Role.Checkbox),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = isChecked, onCheckedChange = null)
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
private fun EmptyLogMessage(uiState: SessionLogUiState) {
    if (uiState.totalEntryCount > 0) return
    Text(
        text = stringResource(R.string.logs_empty),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

/** Dark whatever the app theme is, so that white debug lines can be read. */
@Composable
private fun LogLines(entries: List<LogEntry>) {
    SelectionContainer(Modifier.fillMaxSize().background(logBackgroundColor)) {
        LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
            items(items = entries) { entry -> LogLine(entry) }
        }
    }
}

@Composable
private fun LogLine(entry: LogEntry) {
    val color = levelColor(entry.level)
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp)) {
        Text(
            text = formatLogLine(entry),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = color,
        )
        StackTrace(entry.stackTrace, color)
    }
}

@Composable
private fun StackTrace(stackTrace: String?, color: Color) {
    if (stackTrace == null) return
    Text(
        text = stackTrace,
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
        color = color,
        modifier = Modifier.horizontalScroll(rememberScrollState()),
    )
}

private val logBackgroundColor = Color(0xFF1E1E1E)

private fun levelColor(level: LogLevel): Color =
    when (level) {
        LogLevel.DEBUG -> Color.White
        LogLevel.INFO -> Color(0xFFFFEB3B)
        LogLevel.WARNING -> Color(0xFFFF9800)
        LogLevel.ERROR -> Color(0xFFFF5252)
    }

private fun LogLevel.filterLabelResource(): Int =
    when (this) {
        LogLevel.DEBUG -> R.string.logs_filter_all
        LogLevel.INFO -> R.string.logs_filter_info
        LogLevel.WARNING -> R.string.logs_filter_warning
        LogLevel.ERROR -> R.string.logs_filter_error
    }

private val previewEntries = listOf(
    LogEntry(1_000L, LogLevel.INFO, "StreamingSession", "Connecting to Kitchen (SONOS at 192.168.1.20:1400)", null),
    LogEntry(1_200L, LogLevel.DEBUG, "SoapAvTransportClient", "SetAVTransportURI answered HTTP 200", null),
    LogEntry(
        timestampMillis = 1_900L,
        level = LogLevel.ERROR,
        tag = "StreamingSession",
        message = "Streaming to Kitchen (SONOS at 192.168.1.20:1400) failed",
        stackTrace = "java.io.IOException: Play was rejected: HTTP 500\n\tat SoapAvTransportClient.callAction(AvTransportClient.kt:44)",
    ),
)

@ThemePreviews
@Composable
private fun SessionLogScreenPreview() {
    StreamMusicTheme {
        SessionLogScreen(
            uiState = SessionLogUiState(entries = previewEntries),
            onBackClicked = {},
            onLevelToggled = {},
            onDownloadClicked = {},
        )
    }
}

@ThemePreviews
@Composable
private fun SessionLogScreenEmptyPreview() {
    StreamMusicTheme {
        SessionLogScreen(
            uiState = SessionLogUiState(),
            onBackClicked = {},
            onLevelToggled = {},
            onDownloadClicked = {},
        )
    }
}
