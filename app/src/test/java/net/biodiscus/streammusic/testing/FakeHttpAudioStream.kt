package net.biodiscus.streammusic.testing

import net.biodiscus.streammusic.core.model.StreamAudioFormat
import net.biodiscus.streammusic.core.streaming.http.RunningHttpAudioStream

class FakeHttpAudioStream(
    override val port: Int = 40_000,
    override val format: StreamAudioFormat = StreamAudioFormat.AAC,
) : RunningHttpAudioStream {

    var isStopped = false
        private set

    override fun stop() {
        isStopped = true
    }
}
