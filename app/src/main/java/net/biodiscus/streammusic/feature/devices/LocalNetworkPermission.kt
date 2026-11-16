package net.biodiscus.streammusic.feature.devices

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/** Android 17 (API 37) only lets apps talk to devices on the local network after the user allows it. */
private const val FIRST_API_WITH_LOCAL_NETWORK_PERMISSION = 37

internal class LocalNetworkPermission(
    val isGranted: Boolean,
    val request: () -> Unit,
)

internal fun isLocalNetworkAccessGranted(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < FIRST_API_WITH_LOCAL_NETWORK_PERMISSION) return true
    val result = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_LOCAL_NETWORK)
    return result == PackageManager.PERMISSION_GRANTED
}

/** Asks for the permission once when the screen opens, and again whenever [LocalNetworkPermission.request] is called. */
@Composable
internal fun rememberLocalNetworkPermission(): LocalNetworkPermission {
    val context = LocalContext.current
    var isGranted by remember { mutableStateOf(isLocalNetworkAccessGranted(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        isGranted = granted
    }
    val request = { launcher.launch(Manifest.permission.ACCESS_LOCAL_NETWORK) }

    LaunchedEffect(Unit) {
        if (!isGranted) request()
    }
    return LocalNetworkPermission(isGranted, request)
}
