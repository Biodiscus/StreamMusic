package net.biodiscus.streammusic.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import net.biodiscus.streammusic.core.model.DeviceProtocol
import net.biodiscus.streammusic.core.protocolchoices.ProtocolChoiceRepository

class FakeProtocolChoiceRepository(
    var savedChoices: Map<String, DeviceProtocol> = emptyMap(),
) : ProtocolChoiceRepository {

    private val clears = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override fun load(): Map<String, DeviceProtocol> = savedChoices

    override fun save(deviceId: String, protocol: DeviceProtocol) {
        savedChoices = savedChoices + (deviceId to protocol)
    }

    override fun clear() {
        savedChoices = emptyMap()
        clears.tryEmit(Unit)
    }

    override fun observeClears(): Flow<Unit> = clears
}
