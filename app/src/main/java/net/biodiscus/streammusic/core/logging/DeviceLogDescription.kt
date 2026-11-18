package net.biodiscus.streammusic.core.logging

import net.biodiscus.streammusic.core.model.StreamingDevice

internal fun StreamingDevice.describeForLog(): String = "$name ($protocol at $host:$port)"
