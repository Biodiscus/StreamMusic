package net.biodiscus.streammusic.core.streaming.device

import java.io.IOException

/**
 * A device stopped because someone chose that, on the device or in the app's settings.
 * The session reports it as a failure like any other, but does not connect to the device again.
 */
internal class DeviceEndedOnPurposeException(message: String) : IOException(message)
