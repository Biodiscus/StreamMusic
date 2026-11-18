package net.biodiscus.streammusic.core.protocolchoices

import net.biodiscus.streammusic.core.model.DeviceProtocol

/** Remembers which protocol the user picked for a device, so the choice survives closing the app. */
interface ProtocolChoiceRepository {
    /** The picked protocol per device id (the host of the device). */
    fun load(): Map<String, DeviceProtocol>

    fun save(deviceId: String, protocol: DeviceProtocol)
}
