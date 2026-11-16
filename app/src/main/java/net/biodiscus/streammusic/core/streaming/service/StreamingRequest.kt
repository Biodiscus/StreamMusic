package net.biodiscus.streammusic.core.streaming.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat

private const val EXTRA_DEVICE_IDS = "deviceIds"
private const val EXTRA_DEVICE_ID = "deviceId"
private const val EXTRA_PROJECTION_RESULT_CODE = "projectionResultCode"
private const val EXTRA_PROJECTION_DATA = "projectionData"
private const val ACTION_START = "net.biodiscus.streammusic.action.START_STREAMING"
private const val ACTION_STOP = "net.biodiscus.streammusic.action.STOP_STREAMING"
private const val ACTION_ADD_DEVICE = "net.biodiscus.streammusic.action.ADD_STREAMING_DEVICE"
private const val ACTION_REMOVE_DEVICE = "net.biodiscus.streammusic.action.REMOVE_STREAMING_DEVICE"

/** Everything the service needs to start streaming. The user already granted screen capture. */
internal data class StreamingRequest(
    val deviceIds: Set<String>,
    val projectionResultCode: Int,
    val projectionData: Intent,
)

/** The only place that knows how a [StreamingRequest] is packed into an Intent for [StreamingService]. */
object StreamingServiceCommands {

    fun start(
        context: Context,
        deviceIds: Set<String>,
        projectionResultCode: Int,
        projectionData: Intent,
    ) {
        val intent = Intent(context, StreamingService::class.java)
        intent.action = ACTION_START
        intent.putExtra(EXTRA_DEVICE_IDS, deviceIds.toTypedArray())
        intent.putExtra(EXTRA_PROJECTION_RESULT_CODE, projectionResultCode)
        intent.putExtra(EXTRA_PROJECTION_DATA, projectionData)
        ContextCompat.startForegroundService(context, intent)
    }

    fun stop(context: Context) {
        context.startService(stopIntent(context))
    }

    fun addDevice(context: Context, deviceId: String) {
        context.startService(deviceIntent(context, ACTION_ADD_DEVICE, deviceId))
    }

    fun removeDevice(context: Context, deviceId: String) {
        context.startService(deviceIntent(context, ACTION_REMOVE_DEVICE, deviceId))
    }

    internal fun stopIntent(context: Context): Intent =
        Intent(context, StreamingService::class.java).setAction(ACTION_STOP)

    private fun deviceIntent(context: Context, action: String, deviceId: String): Intent =
        Intent(context, StreamingService::class.java).setAction(action).putExtra(EXTRA_DEVICE_ID, deviceId)

    internal fun isStart(intent: Intent?): Boolean = intent?.action == ACTION_START

    internal fun isStop(intent: Intent?): Boolean = intent?.action == ACTION_STOP

    internal fun isAddDevice(intent: Intent?): Boolean = intent?.action == ACTION_ADD_DEVICE

    internal fun isRemoveDevice(intent: Intent?): Boolean = intent?.action == ACTION_REMOVE_DEVICE

    internal fun readDeviceId(intent: Intent): String? = intent.getStringExtra(EXTRA_DEVICE_ID)

    internal fun readRequest(intent: Intent): StreamingRequest? {
        val deviceIds = intent.getStringArrayExtra(EXTRA_DEVICE_IDS)?.toSet() ?: return null
        val projectionData = IntentCompat.getParcelableExtra(intent, EXTRA_PROJECTION_DATA, Intent::class.java)
            ?: return null
        return StreamingRequest(
            deviceIds = deviceIds,
            projectionResultCode = intent.getIntExtra(EXTRA_PROJECTION_RESULT_CODE, 0),
            projectionData = projectionData,
        )
    }
}
