package net.biodiscus.streammusic.core.streaming

/** The raw audio format captured from the phone: 48 kHz, 16-bit, stereo. AriaCast expects exactly this. */
internal object PcmFormat {
    const val SAMPLE_RATE_HZ = 48_000
    const val CHANNEL_COUNT = 2
    const val BYTES_PER_SAMPLE = 2
    const val FRAME_DURATION_MILLIS = 20

    const val SAMPLES_PER_FRAME = SAMPLE_RATE_HZ * FRAME_DURATION_MILLIS / 1000
    const val BYTES_PER_FRAME = SAMPLES_PER_FRAME * CHANNEL_COUNT * BYTES_PER_SAMPLE
}
