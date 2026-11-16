package net.biodiscus.streammusic.feature.devices

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import net.biodiscus.streammusic.core.streaming.service.StreamingServiceCommands

/**
 * Walks the user through everything Android requires before the audio of other apps can be captured:
 * the microphone permission (needed by the capture API) and the screen sharing consent (which carries the
 * audio capture right). The consent dialog is Android's own, and it is the only place the user is asked
 * what to share. Then it starts the streaming service.
 *
 * Returns the function to call with the devices the user chose.
 */
@Composable
internal fun rememberStreamStarter(
    hidesSingleAppCapture: Boolean,
    onRecordAudioDenied: () -> Unit,
): (Set<String>) -> Unit {
    val context = LocalContext.current
    var chosenDeviceIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    val projectionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val projectionData = result.data
        if (result.resultCode == Activity.RESULT_OK && projectionData != null) {
            StreamingServiceCommands.start(context, chosenDeviceIds, result.resultCode, projectionData)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        val canRecordAudio = grants[Manifest.permission.RECORD_AUDIO] == true
        if (canRecordAudio) {
            projectionLauncher.launch(createScreenCaptureIntent(context, hidesSingleAppCapture))
        } else {
            onRecordAudioDenied()
        }
    }

    return { deviceIds ->
        chosenDeviceIds = deviceIds
        permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS))
    }
}

private fun createScreenCaptureIntent(context: Context, hidesSingleAppCapture: Boolean): Intent {
    val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    if (!hidesSingleAppCapture) return projectionManager.createScreenCaptureIntent()
    return projectionManager.createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay())
}
