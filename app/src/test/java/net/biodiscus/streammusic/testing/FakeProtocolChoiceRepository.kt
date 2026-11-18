package net.biodiscus.streammusic.testing

import net.biodiscus.streammusic.core.model.DeviceProtocol
import net.biodiscus.streammusic.core.protocolchoices.ProtocolChoiceRepository

class FakeProtocolChoiceRepository(
    var savedChoices: Map<String, DeviceProtocol> = emptyMap(),
) : ProtocolChoiceRepository {

    override fun load(): Map<String, DeviceProtocol> = savedChoices

    override fun save(deviceId: String, protocol: DeviceProtocol) {
        savedChoices = savedChoices + (deviceId to protocol)
    }
}
