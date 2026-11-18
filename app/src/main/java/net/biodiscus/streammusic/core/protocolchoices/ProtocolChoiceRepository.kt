package net.biodiscus.streammusic.core.protocolchoices

import kotlinx.coroutines.flow.Flow
import net.biodiscus.streammusic.core.model.DeviceProtocol

/** Remembers which protocol the user picked for a device, so the choice survives closing the app. */
interface ProtocolChoiceRepository {
    /** The picked protocol per device id (the host of the device). */
    fun load(): Map<String, DeviceProtocol>

    fun save(deviceId: String, protocol: DeviceProtocol)

    /** Forgets every picked protocol. */
    fun clear()

    /** Emits each time [clear] is called, so a screen that keeps the choices in memory can forget them too. */
    fun observeClears(): Flow<Unit>
}
