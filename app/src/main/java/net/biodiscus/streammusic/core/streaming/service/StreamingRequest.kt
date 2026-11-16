package net.biodiscus.streammusic.core.streaming.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat

private const val EXTRA_DEVICE_IDS = "deviceIds"
private const val EXTRA_PROJECTION_RESULT_CODE = "projectionResultCode"
private const val EXTRA_PROJECTION_DATA = "projectionData"
private const val ACTION_START = "net.biodiscus.streammusic.action.START_STREAMING"
private const val ACTION_STOP = "net.biodiscus.streammusic.action.STOP_STREAMING"

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

    internal fun stopIntent(context: Context): Intent =
        Intent(context, StreamingService::class.java).setAction(ACTION_STOP)

    internal fun isStart(intent: Intent?): Boolean = intent?.action == ACTION_START

    internal fun isStop(intent: Intent?): Boolean = intent?.action == ACTION_STOP

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
