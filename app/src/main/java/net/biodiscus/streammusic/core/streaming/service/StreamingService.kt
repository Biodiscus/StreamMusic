package net.biodiscus.streammusic.core.streaming.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import net.biodiscus.streammusic.core.discovery.DeviceRepository
import net.biodiscus.streammusic.core.model.StreamingDevice
import net.biodiscus.streammusic.core.streaming.StreamingSession
import net.biodiscus.streammusic.core.streaming.StoppedBecauseOfSilenceException
import net.biodiscus.streammusic.core.streaming.StreamingSessionFactory
import net.biodiscus.streammusic.core.streaming.capture.PlaybackAudioCapture
import timber.log.Timber

private const val NOTIFICATION_ID = 1

/** Keeps capturing and streaming while the user is in another app. */
@AndroidEntryPoint
class StreamingService : Service() {

    @Inject
    internal lateinit var deviceRepository: DeviceRepository

    @Inject
    internal lateinit var sessionFactory: StreamingSessionFactory

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var streamingJob: Job? = null
    private var runningSession: StreamingSession? = null
    private var mediaProjection: MediaProjection? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when {
            StreamingServiceCommands.isStart(intent) -> startStreaming(checkNotNull(intent))
            StreamingServiceCommands.isStop(intent) -> stopStreaming()
            StreamingServiceCommands.isAddDevice(intent) -> addDeviceToSession(checkNotNull(intent))
            StreamingServiceCommands.isRemoveDevice(intent) -> removeDeviceFromSession(checkNotNull(intent))
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        mediaProjection?.stop()
        super.onDestroy()
    }

    private fun startStreaming(intent: Intent) {
        startInForeground()

        val request = StreamingServiceCommands.readRequest(intent)
        if (request == null) {
            releaseEverythingAndStop()
            return
        }

        val devices = findDevicesThatCanStream(request.deviceIds)
        val projection = createMediaProjection(request)
        if (devices.isEmpty() || projection == null) {
            releaseEverythingAndStop()
            return
        }

        streamingJob?.cancel()
        val capture = PlaybackAudioCapture(projection)
        val session = sessionFactory.create(devices, capture.frames())
        runningSession = session
        streamingJob = serviceScope.launch { runSession { session.run() } }
    }

    private fun addDeviceToSession(intent: Intent) {
        val session = runningSessionOrStopService() ?: return
        val deviceId = StreamingServiceCommands.readDeviceId(intent) ?: return
        val device = findDevicesThatCanStream(setOf(deviceId)).firstOrNull() ?: return
        session.addDevice(device)
    }

    private fun removeDeviceFromSession(intent: Intent) {
        val session = runningSessionOrStopService() ?: return
        val deviceId = StreamingServiceCommands.readDeviceId(intent) ?: return
        session.removeDevice(deviceId)
    }

    /** A device command that arrives without a session would otherwise leave an idle service behind. */
    private fun runningSessionOrStopService(): StreamingSession? {
        val session = runningSession
        if (session == null) stopSelf()
        return session
    }

    /** A foreground service of type mediaProjection must be in the foreground before it uses the projection. */
    private fun startInForeground() {
        val notification = StreamingNotification(this).build(StreamingServiceCommands.stopIntent(this))
        startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
    }

    private fun findDevicesThatCanStream(deviceIds: Set<String>): List<StreamingDevice> =
        deviceRepository.findDevices(deviceIds).filter { device -> device.canReceiveStream }

    private fun createMediaProjection(request: StreamingRequest): MediaProjection? {
        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val projection = manager.getMediaProjection(request.projectionResultCode, request.projectionData)
        projection?.registerCallback(StopWhenProjectionEnds(), Handler(Looper.getMainLooper()))
        mediaProjection = projection
        return projection
    }

    private suspend fun runSession(session: suspend () -> Nothing) {
        try {
            session()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (silence: StoppedBecauseOfSilenceException) {
            Timber.i(silence, "Streaming stopped by itself")
        } catch (failure: Exception) {
            Timber.e(failure, "Streaming stopped because of an error")
        } finally {
            releaseEverythingAndStop()
        }
    }

    private fun stopStreaming() {
        val job = streamingJob
        if (job == null) {
            stopSelf()
            return
        }
        job.cancel()
    }

    private fun releaseEverythingAndStop() {
        runningSession = null
        mediaProjection?.stop()
        mediaProjection = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /** The system ends a projection when the user revokes it from the status bar. */
    private inner class StopWhenProjectionEnds : MediaProjection.Callback() {
        override fun onStop() = stopStreaming()
    }
}
