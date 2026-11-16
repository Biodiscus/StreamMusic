package net.biodiscus.streammusic.feature.debug

import net.biodiscus.streammusic.core.model.DeviceProtocol
import net.biodiscus.streammusic.core.model.StreamingDevice

internal enum class ImplementationFilter {
    ALL,
    IMPLEMENTED,
    NOT_IMPLEMENTED,
}

/** An empty [protocols] set means every protocol. Both parts must match for a find to be shown. */
internal data class DiscoveryDebugFilter(
    val implementation: ImplementationFilter = ImplementationFilter.ALL,
    val protocols: Set<DeviceProtocol> = emptySet(),
) {
    fun matches(find: StreamingDevice): Boolean =
        matchesImplementation(find.protocol) && matchesProtocols(find.protocol)

    fun withProtocolToggled(protocol: DeviceProtocol): DiscoveryDebugFilter {
        val toggledProtocols = if (protocol in protocols) protocols - protocol else protocols + protocol
        return copy(protocols = toggledProtocols)
    }

    private fun matchesImplementation(protocol: DeviceProtocol): Boolean =
        when (implementation) {
            ImplementationFilter.ALL -> true
            ImplementationFilter.IMPLEMENTED -> protocol.isImplemented
            ImplementationFilter.NOT_IMPLEMENTED -> !protocol.isImplemented
        }

    private fun matchesProtocols(protocol: DeviceProtocol): Boolean =
        protocols.isEmpty() || protocol in protocols
}
