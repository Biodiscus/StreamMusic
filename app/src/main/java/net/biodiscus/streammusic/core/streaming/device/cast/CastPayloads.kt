package net.biodiscus.streammusic.core.streaming.device.cast

import org.json.JSONObject

internal const val SENDER_ID = "sender-0"
internal const val PLATFORM_RECEIVER_ID = "receiver-0"

/** The built-in Chromecast app that plays any audio or video address it is given. */
internal const val DEFAULT_MEDIA_RECEIVER_APP_ID = "CC1AD845"

internal object CastNamespace {
    const val CONNECTION = "urn:x-cast:com.google.cast.tp.connection"
    const val HEARTBEAT = "urn:x-cast:com.google.cast.tp.heartbeat"
    const val RECEIVER = "urn:x-cast:com.google.cast.receiver"
    const val MEDIA = "urn:x-cast:com.google.cast.media"
}

internal data class LaunchedCastApp(
    val transportId: String,
    val sessionId: String,
)

internal fun connectPayload(): String = JSONObject().put("type", "CONNECT").toString()

internal fun pingPayload(): String = JSONObject().put("type", "PING").toString()

internal fun pongPayload(): String = JSONObject().put("type", "PONG").toString()

internal fun launchPayload(appId: String, requestId: Int): String =
    JSONObject()
        .put("type", "LAUNCH")
        .put("appId", appId)
        .put("requestId", requestId)
        .toString()

internal fun stopPayload(sessionId: String, requestId: Int): String =
    JSONObject()
        .put("type", "STOP")
        .put("sessionId", sessionId)
        .put("requestId", requestId)
        .toString()

internal fun loadLiveAudioPayload(streamUrl: String, contentType: String, requestId: Int): String {
    val media = JSONObject()
        .put("contentId", streamUrl)
        .put("streamType", "LIVE")
        .put("contentType", contentType)
    return JSONObject()
        .put("type", "LOAD")
        .put("requestId", requestId)
        .put("autoplay", true)
        .put("media", media)
        .toString()
}

internal fun payloadType(payload: String): String = JSONObject(payload).optString("type")

/** Reads a RECEIVER_STATUS message and returns the app with [appId] if it is running. */
internal fun findLaunchedApp(payload: String, appId: String): LaunchedCastApp? {
    val applications = JSONObject(payload).optJSONObject("status")?.optJSONArray("applications") ?: return null
    for (index in 0 until applications.length()) {
        val application = applications.getJSONObject(index)
        if (application.optString("appId") == appId) {
            return LaunchedCastApp(
                transportId = application.getString("transportId"),
                sessionId = application.getString("sessionId"),
            )
        }
    }
    return null
}

/** True for a MEDIA_STATUS message that describes a player, as opposed to the empty status sent on connect. */
internal fun isPlayerStatus(payload: String): Boolean {
    val statuses = JSONObject(payload).optJSONArray("status") ?: return false
    return statuses.length() > 0
}

/** Returns why the player went idle (for example `ERROR`), or null while it is still playing or buffering. */
internal fun findIdleReason(payload: String): String? {
    val statuses = JSONObject(payload).optJSONArray("status") ?: return null
    if (statuses.length() == 0) return null
    val status = statuses.getJSONObject(0)
    val isIdle = status.optString("playerState") == "IDLE"
    return if (isIdle) status.optString("idleReason", "UNKNOWN") else null
}

/** Reads a RECEIVER_STATUS message and tells whether the Chromecast is muted. Null for any other message. */
internal fun findMuted(payload: String): Boolean? {
    val json = JSONObject(payload)
    if (json.optString("type") != "RECEIVER_STATUS") return null
    val volume = json.optJSONObject("status")?.optJSONObject("volume") ?: return null
    return if (volume.has("muted")) volume.getBoolean("muted") else null
}
