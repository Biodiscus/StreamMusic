package net.biodiscus.streammusic.core.streaming.service

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import net.biodiscus.streammusic.core.streaming.StreamingController

internal class ServiceStreamingController @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : StreamingController {

    override fun addDevice(deviceId: String) = StreamingServiceCommands.addDevice(context, deviceId)

    override fun removeDevice(deviceId: String) = StreamingServiceCommands.removeDevice(context, deviceId)

    override fun stop() = StreamingServiceCommands.stop(context)
}
