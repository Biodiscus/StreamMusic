package net.biodiscus.streammusic.feature.settings

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import net.biodiscus.streammusic.R
import net.biodiscus.streammusic.core.model.AacQuality
import net.biodiscus.streammusic.core.model.AppSettings
import net.biodiscus.streammusic.core.model.SilenceTimeout
import net.biodiscus.streammusic.core.model.ThemeMode

@Composable
internal fun GeneralSettings(settings: AppSettings, actions: SettingsActions) {
    SectionHeader(stringResource(R.string.settings_general_header))
    ChoiceSetting(
        title = stringResource(R.string.settings_theme),
        options = ThemeMode.entries,
        chosenOption = settings.themeMode,
        labelOf = { themeMode -> stringResource(themeMode.labelResource()) },
        onOptionChosen = actions.onThemeModeChosen,
    )
    SwitchSetting(
        title = stringResource(R.string.settings_hide_single_app_capture),
        description = stringResource(R.string.settings_hide_single_app_capture_description),
        isChecked = settings.hidesSingleAppCapture,
        onCheckedChange = actions.onHidesSingleAppCaptureChanged,
    )
}

@Composable
internal fun StreamingSettings(settings: AppSettings, actions: SettingsActions) {
    SectionHeader(stringResource(R.string.settings_streaming_header))
    SwitchSetting(
        title = stringResource(R.string.settings_mute_phone),
        description = stringResource(R.string.settings_mute_phone_description),
        isChecked = settings.mutesPhoneWhileStreaming,
        onCheckedChange = actions.onMutesPhoneWhileStreamingChanged,
    )
    SwitchSetting(
        title = stringResource(R.string.settings_reconnect),
        description = stringResource(R.string.settings_reconnect_description),
        isChecked = settings.reconnectsDroppedDevices,
        onCheckedChange = actions.onReconnectsDroppedDevicesChanged,
    )
    ChoiceSetting(
        title = stringResource(R.string.settings_stop_after_silence),
        description = stringResource(R.string.settings_stop_after_silence_description),
        options = SilenceTimeout.entries,
        chosenOption = settings.stopsStreamingAfterSilence,
        labelOf = { timeout -> stringResource(timeout.labelResource()) },
        onOptionChosen = actions.onStopsStreamingAfterSilenceChosen,
    )
    ChoiceSetting(
        title = stringResource(R.string.settings_aac_quality),
        description = stringResource(R.string.settings_aac_quality_description),
        options = AacQuality.entries,
        chosenOption = settings.aacQuality,
        labelOf = { quality -> stringResource(quality.labelResource()) },
        onOptionChosen = actions.onAacQualityChosen,
    )
}

@Composable
internal fun AboutSettings() {
    val context = LocalContext.current
    val versionName = remember { versionNameOf(context) }
    SectionHeader(stringResource(R.string.settings_about_header))
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_version)) },
        supportingContent = { Text(versionName) },
    )
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_privacy)) },
        supportingContent = { Text(stringResource(R.string.settings_privacy_description)) },
    )
}

/** The last thing on the screen, and it asks again before it deletes anything. */
@Composable
internal fun ResetSettings(onResetConfirmed: () -> Unit) {
    var isAskingToConfirm by rememberSaveable { mutableStateOf(false) }

    SectionHeader(stringResource(R.string.settings_reset_header))
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_reset_all_data)) },
        supportingContent = { Text(stringResource(R.string.settings_reset_all_data_description)) },
        colors = ListItemDefaults.colors(headlineColor = MaterialTheme.colorScheme.error),
        modifier = Modifier.clickable { isAskingToConfirm = true },
    )
    if (isAskingToConfirm) {
        ResetConfirmationDialog(
            onConfirmed = {
                isAskingToConfirm = false
                onResetConfirmed()
            },
            onDismissed = { isAskingToConfirm = false },
        )
    }
}

@Composable
private fun ResetConfirmationDialog(onConfirmed: () -> Unit, onDismissed: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismissed,
        title = { Text(stringResource(R.string.settings_reset_title)) },
        text = { Text(stringResource(R.string.settings_reset_body)) },
        confirmButton = {
            TextButton(onClick = onConfirmed) {
                Text(stringResource(R.string.settings_reset_confirm), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissed) { Text(stringResource(R.string.settings_reset_cancel)) }
        },
    )
}

private fun versionNameOf(context: Context): String {
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    return packageInfo.versionName.orEmpty()
}

@StringRes
private fun ThemeMode.labelResource(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.settings_theme_system
    ThemeMode.LIGHT -> R.string.settings_theme_light
    ThemeMode.DARK -> R.string.settings_theme_dark
}

@StringRes
private fun AacQuality.labelResource(): Int = when (this) {
    AacQuality.LOW -> R.string.settings_aac_quality_low
    AacQuality.STANDARD -> R.string.settings_aac_quality_standard
    AacQuality.HIGH -> R.string.settings_aac_quality_high
}

@StringRes
private fun SilenceTimeout.labelResource(): Int = when (this) {
    SilenceTimeout.NEVER -> R.string.settings_silence_never
    SilenceTimeout.FIVE_MINUTES -> R.string.settings_silence_five_minutes
    SilenceTimeout.FIFTEEN_MINUTES -> R.string.settings_silence_fifteen_minutes
    SilenceTimeout.THIRTY_MINUTES -> R.string.settings_silence_thirty_minutes
    SilenceTimeout.ONE_HOUR -> R.string.settings_silence_one_hour
}
