package net.biodiscus.streammusic.feature.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.biodiscus.streammusic.R
import net.biodiscus.streammusic.core.model.AIRPLAY_AUDIO_DELAY_STEP_MILLIS
import net.biodiscus.streammusic.core.model.AacQuality
import net.biodiscus.streammusic.core.model.AppSettings
import net.biodiscus.streammusic.core.model.MAX_AIRPLAY_AUDIO_DELAY_MILLIS
import net.biodiscus.streammusic.core.model.ProtocolFamily
import net.biodiscus.streammusic.core.model.SilenceTimeout
import net.biodiscus.streammusic.core.model.StreamAudioFormat
import net.biodiscus.streammusic.core.model.ThemeMode
import net.biodiscus.streammusic.core.model.audioFormatOf
import net.biodiscus.streammusic.core.model.isEnabled
import net.biodiscus.streammusic.ui.ThemePreviews
import net.biodiscus.streammusic.ui.theme.StreamMusicTheme

private const val MILLIS_PER_SECOND = 1000f

/** Everything the user can change on the Settings screen. */
internal class SettingsActions(
    val onHidesSingleAppCaptureChanged: (Boolean) -> Unit,
    val onProtocolFamilyToggled: (ProtocolFamily, Boolean) -> Unit,
    val onAudioFormatChosen: (ProtocolFamily, StreamAudioFormat) -> Unit,
    val onAirPlayAudioDelayChanged: (Int) -> Unit,
    val onUsesAirPlay2WhereSupportedChanged: (Boolean) -> Unit,
    val onDisconnectsChromecastOnMuteChanged: (Boolean) -> Unit,
    val onThemeModeChosen: (ThemeMode) -> Unit,
    val onMutesPhoneWhileStreamingChanged: (Boolean) -> Unit,
    val onReconnectsDroppedDevicesChanged: (Boolean) -> Unit,
    val onAacQualityChosen: (AacQuality) -> Unit,
    val onStopsStreamingAfterSilenceChosen: (SilenceTimeout) -> Unit,
    val onResetAllDataConfirmed: () -> Unit,
)

@Composable
internal fun SettingsRoute(
    onBackClicked: () -> Unit,
    onDiscoveryDebugClicked: () -> Unit,
    onSessionLogClicked: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val actions = SettingsActions(
        onHidesSingleAppCaptureChanged = viewModel::onHidesSingleAppCaptureChanged,
        onProtocolFamilyToggled = viewModel::onProtocolFamilyToggled,
        onAudioFormatChosen = viewModel::onAudioFormatChosen,
        onAirPlayAudioDelayChanged = viewModel::onAirPlayAudioDelayChanged,
        onUsesAirPlay2WhereSupportedChanged = viewModel::onUsesAirPlay2WhereSupportedChanged,
        onDisconnectsChromecastOnMuteChanged = viewModel::onDisconnectsChromecastOnMuteChanged,
        onThemeModeChosen = viewModel::onThemeModeChosen,
        onMutesPhoneWhileStreamingChanged = viewModel::onMutesPhoneWhileStreamingChanged,
        onReconnectsDroppedDevicesChanged = viewModel::onReconnectsDroppedDevicesChanged,
        onAacQualityChosen = viewModel::onAacQualityChosen,
        onStopsStreamingAfterSilenceChosen = viewModel::onStopsStreamingAfterSilenceChosen,
        onResetAllDataConfirmed = viewModel::onResetAllDataConfirmed,
    )
    SettingsScreen(
        settings = settings,
        actions = actions,
        onBackClicked = onBackClicked,
        onDiscoveryDebugClicked = onDiscoveryDebugClicked,
        onSessionLogClicked = onSessionLogClicked,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    settings: AppSettings,
    actions: SettingsActions,
    onBackClicked: () -> Unit,
    onDiscoveryDebugClicked: () -> Unit,
    onSessionLogClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.settings_back))
                    }
                },
            )
        },
    ) { contentPadding ->
        Column(
            Modifier
                .padding(contentPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            GeneralSettings(settings, actions)
            StreamingSettings(settings, actions)

            SectionHeader(stringResource(R.string.settings_protocols_header))
            ProtocolFamily.entries.forEach { family ->
                ProtocolFamilySettings(family, settings, actions)
            }

            SectionHeader(stringResource(R.string.settings_troubleshooting_header))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_discovery_debug)) },
                supportingContent = { Text(stringResource(R.string.settings_discovery_debug_description)) },
                leadingContent = { Icon(Icons.Filled.BugReport, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onDiscoveryDebugClicked),
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_session_log)) },
                supportingContent = { Text(stringResource(R.string.settings_session_log_description)) },
                leadingContent = { Icon(Icons.Filled.Description, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onSessionLogClicked),
            )

            AboutSettings()
            ResetSettings(onResetConfirmed = actions.onResetAllDataConfirmed)
        }
    }
}

/**
 * One family: its switch, and below it the options that only matter while the family is on.
 * Tapping the row folds or unfolds the options, so the list stays short. The switch has its own tap.
 */
@Composable
private fun ProtocolFamilySettings(
    family: ProtocolFamily,
    settings: AppSettings,
    actions: SettingsActions,
) {
    val isEnabled = settings.isEnabled(family)
    val canFoldOptions = isEnabled && hasOptions(family)
    var isExpanded by rememberSaveable(family.name) { mutableStateOf(false) }

    FamilyHeader(
        family = family,
        isEnabled = isEnabled,
        arrow = if (canFoldOptions) foldArrow(isExpanded) else null,
        onSwitchChanged = { isChecked -> actions.onProtocolFamilyToggled(family, isChecked) },
        onRowClicked = {
            if (canFoldOptions) isExpanded = !isExpanded else actions.onProtocolFamilyToggled(family, !isEnabled)
        },
    )
    AnimatedVisibility(visible = canFoldOptions && isExpanded) {
        Column { FamilyOptions(family, settings, actions) }
    }
}

@Composable
private fun FamilyHeader(
    family: ProtocolFamily,
    isEnabled: Boolean,
    arrow: FoldArrow?,
    onSwitchChanged: (Boolean) -> Unit,
    onRowClicked: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(family.displayName) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (arrow != null) {
                    Icon(arrow.icon, contentDescription = stringResource(arrow.descriptionResource))
                }
                Switch(checked = isEnabled, onCheckedChange = onSwitchChanged)
            }
        },
        modifier = Modifier.clickable(onClick = onRowClicked),
    )
}

@Composable
private fun FamilyOptions(
    family: ProtocolFamily,
    settings: AppSettings,
    actions: SettingsActions,
) {
    if (family.selectableAudioFormats.isNotEmpty()) {
        ChoiceSetting(
            title = stringResource(R.string.settings_audio_format),
            options = family.selectableAudioFormats,
            chosenOption = settings.audioFormatOf(family),
            labelOf = { format -> format.displayName },
            onOptionChosen = { format -> actions.onAudioFormatChosen(family, format) },
        )
    }
    if (family == ProtocolFamily.AIRPLAY) {
        SwitchSetting(
            title = stringResource(R.string.settings_use_airplay_2),
            description = stringResource(R.string.settings_use_airplay_2_description),
            isChecked = settings.usesAirPlay2WhereSupported,
            onCheckedChange = actions.onUsesAirPlay2WhereSupportedChanged,
        )
        AudioDelaySetting(settings.airPlayAudioDelayMillis, actions.onAirPlayAudioDelayChanged)
    }
    if (family == ProtocolFamily.GOOGLE_CAST) {
        SwitchSetting(
            title = stringResource(R.string.settings_chromecast_disconnect_on_mute),
            description = stringResource(R.string.settings_chromecast_disconnect_on_mute_description),
            isChecked = settings.disconnectsChromecastOnMute,
            onCheckedChange = actions.onDisconnectsChromecastOnMuteChanged,
        )
    }
}

private fun hasOptions(family: ProtocolFamily): Boolean =
    family.selectableAudioFormats.isNotEmpty() ||
        family == ProtocolFamily.AIRPLAY ||
        family == ProtocolFamily.GOOGLE_CAST

private class FoldArrow(val icon: ImageVector, val descriptionResource: Int)

private fun foldArrow(isExpanded: Boolean): FoldArrow =
    if (isExpanded) {
        FoldArrow(Icons.Filled.KeyboardArrowUp, R.string.settings_collapse_section)
    } else {
        FoldArrow(Icons.Filled.KeyboardArrowDown, R.string.settings_expand_section)
    }

@Composable
private fun AudioDelaySetting(delayMillis: Int, onDelayChanged: (Int) -> Unit) {
    val stepCount = MAX_AIRPLAY_AUDIO_DELAY_MILLIS / AIRPLAY_AUDIO_DELAY_STEP_MILLIS
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_airplay_audio_delay)) },
        supportingContent = {
            Column {
                Text(stringResource(R.string.settings_airplay_audio_delay_description, delayMillis / MILLIS_PER_SECOND))
                Slider(
                    value = delayMillis.toFloat(),
                    onValueChange = { newDelay -> onDelayChanged(newDelay.toInt()) },
                    valueRange = 0f..MAX_AIRPLAY_AUDIO_DELAY_MILLIS.toFloat(),
                    steps = stepCount - 1,
                )
            }
        },
    )
}

@ThemePreviews
@Composable
private fun SettingsScreenPreview() {
    StreamMusicTheme {
        SettingsScreen(
            settings = AppSettings(),
            actions = SettingsActions(
                onHidesSingleAppCaptureChanged = {},
                onProtocolFamilyToggled = { _, _ -> },
                onAudioFormatChosen = { _, _ -> },
                onAirPlayAudioDelayChanged = {},
                onUsesAirPlay2WhereSupportedChanged = {},
                onDisconnectsChromecastOnMuteChanged = {},
                onThemeModeChosen = {},
                onMutesPhoneWhileStreamingChanged = {},
                onReconnectsDroppedDevicesChanged = {},
                onAacQualityChosen = {},
                onStopsStreamingAfterSilenceChosen = {},
                onResetAllDataConfirmed = {},
            ),
            onBackClicked = {},
            onDiscoveryDebugClicked = {},
            onSessionLogClicked = {},
        )
    }
}
