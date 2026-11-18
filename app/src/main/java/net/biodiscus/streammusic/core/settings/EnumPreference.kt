package net.biodiscus.streammusic.core.settings

/** A name that no entry has anymore (from an older version of the app) gives the default. */
internal inline fun <reified T : Enum<T>> decodeEnum(name: String?, default: T): T =
    enumValues<T>().firstOrNull { entry -> entry.name == name } ?: default
