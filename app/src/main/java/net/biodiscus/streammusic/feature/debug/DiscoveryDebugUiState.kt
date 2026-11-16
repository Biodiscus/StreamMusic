package net.biodiscus.streammusic.feature.debug

import net.biodiscus.streammusic.core.model.DeviceProtocol
import net.biodiscus.streammusic.core.model.StreamingDevice

/** The finds of one protocol. The protocol is the category: it says whether the app can stream to them. */
internal data class DebugCategory(
    val protocol: DeviceProtocol,
    val finds: List<StreamingDevice>,
) {
    val isImplemented: Boolean = protocol.isImplemented
}

/** [finds] are the devices exactly as discovery reported them: one per protocol, not merged by host. */
internal data class DiscoveryDebugUiState(
    val finds: List<StreamingDevice> = emptyList(),
    val filter: DiscoveryDebugFilter = DiscoveryDebugFilter(),
) {
    val isSearching: Boolean = finds.isEmpty()

    private val shownFinds: List<StreamingDevice> = finds.filter { find -> filter.matches(find) }

    val shownFindCount: Int = shownFinds.size

    val totalFindCount: Int = finds.size

    /** The shown finds grouped by protocol, in the order of the protocol list. */
    val categories: List<DebugCategory> =
        shownFinds
            .groupBy { find -> find.protocol }
            .map { entry -> DebugCategory(protocol = entry.key, finds = entry.value) }
            .sortedBy { category -> category.protocol.ordinal }

    /** A chip for every protocol that was found, and for every protocol that is filtered on but no longer found. */
    val protocolChoices: List<DeviceProtocol> =
        (finds.map { find -> find.protocol } + filter.protocols).distinct().sortedBy { protocol -> protocol.ordinal }
}
