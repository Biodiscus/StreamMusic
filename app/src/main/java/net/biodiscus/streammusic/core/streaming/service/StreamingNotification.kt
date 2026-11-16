package net.biodiscus.streammusic.core.streaming.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import net.biodiscus.streammusic.MainActivity
import net.biodiscus.streammusic.R

private const val CHANNEL_ID = "streaming"

internal class StreamingNotification(private val context: Context) {

    fun build(stopIntent: Intent): Notification {
        createChannelIfNeeded()
        val stopAction = Notification.Action.Builder(
            null,
            context.getString(R.string.streaming_notification_stop),
            PendingIntent.getService(context, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE),
        ).build()
        return Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(context.getString(R.string.streaming_notification_title))
            .setContentIntent(openAppIntent())
            .addAction(stopAction)
            .setOngoing(true)
            .build()
    }

    private fun createChannelIfNeeded() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.streaming_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }
}
