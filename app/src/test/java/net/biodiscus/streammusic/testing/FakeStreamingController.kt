package net.biodiscus.streammusic.testing

import net.biodiscus.streammusic.core.streaming.StreamingController

class FakeStreamingController : StreamingController {

    val addedDeviceIds = mutableListOf<String>()
    val removedDeviceIds = mutableListOf<String>()
    var stopCount = 0
        private set

    override fun addDevice(deviceId: String) {
        addedDeviceIds += deviceId
    }

    override fun removeDevice(deviceId: String) {
        removedDeviceIds += deviceId
    }

    override fun stop() {
        stopCount++
    }
}
