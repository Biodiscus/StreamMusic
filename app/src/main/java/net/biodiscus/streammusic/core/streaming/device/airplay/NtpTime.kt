package net.biodiscus.streammusic.core.streaming.device.airplay

private const val SECONDS_FROM_1900_TO_1970 = 2_208_988_800L
private const val MILLIS_PER_SECOND = 1000L
private const val FRACTIONS_PER_SECOND = 1L shl 32

/**
 * A time as 32 bits of seconds since 1900 followed by 32 bits of fraction of a second, kept in one Long.
 * This is the clock the receiver and the phone agree on.
 */
internal fun ntpTimeFromEpochMillis(epochMillis: Long): Long {
    val seconds = epochMillis / MILLIS_PER_SECOND + SECONDS_FROM_1900_TO_1970
    val fraction = (epochMillis % MILLIS_PER_SECOND) * FRACTIONS_PER_SECOND / MILLIS_PER_SECOND
    return (seconds shl 32) or fraction
}

/** The audio sample number that is playing at [ntpTime]. All 64 bits are used, so the math is unsigned. */
internal fun sampleNumberAt(ntpTime: Long, sampleRate: Int): Long {
    val secondsWithPartOfFraction = ntpTime.toULong() shr 16
    return ((secondsWithPartOfFraction * sampleRate.toULong()) shr 16).toLong()
}

/** The time at which sample number [sampleNumber] is playing. The reverse of [sampleNumberAt]. */
internal fun ntpTimeOfSample(sampleNumber: Long, sampleRate: Int): Long {
    val shiftedSamples = sampleNumber.toULong() shl 16
    return ((shiftedSamples / sampleRate.toULong()) shl 16).toLong()
}
