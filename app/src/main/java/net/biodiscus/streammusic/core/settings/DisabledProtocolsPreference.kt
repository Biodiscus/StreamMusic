package net.biodiscus.streammusic.core.settings

import net.biodiscus.streammusic.core.model.DeviceProtocol

internal fun encodeDisabledProtocols(protocols: Set<DeviceProtocol>): Set<String> =
    protocols.map { protocol -> protocol.name }.toSet()

/** Names that no protocol has anymore (from an older version of the app) are dropped. */
internal fun decodeDisabledProtocols(protocolNames: Set<String>): Set<DeviceProtocol> =
    DeviceProtocol.entries.filter { protocol -> protocol.name in protocolNames }.toSet()
