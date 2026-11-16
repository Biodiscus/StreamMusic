package net.biodiscus.streammusic.core.streaming.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import net.biodiscus.streammusic.core.streaming.AudioManagerPhoneMusicVolume
import net.biodiscus.streammusic.core.streaming.PhoneMusicVolume
import net.biodiscus.streammusic.core.streaming.StreamingController
import net.biodiscus.streammusic.core.streaming.device.DeviceStreamerFactory
import net.biodiscus.streammusic.core.streaming.device.ProtocolDeviceStreamerFactory
import net.biodiscus.streammusic.core.streaming.device.airplay.RaopConnector
import net.biodiscus.streammusic.core.streaming.device.airplay.SocketRaopConnector
import net.biodiscus.streammusic.core.streaming.device.ariacast.AriaCastConnector
import net.biodiscus.streammusic.core.streaming.device.ariacast.WebSocketAriaCastConnector
import net.biodiscus.streammusic.core.streaming.device.cast.CastChannelOpener
import net.biodiscus.streammusic.core.streaming.device.cast.TlsCastChannelOpener
import net.biodiscus.streammusic.core.streaming.device.upnp.AvTransportClient
import net.biodiscus.streammusic.core.streaming.device.upnp.SoapAvTransportClient
import net.biodiscus.streammusic.core.streaming.http.HttpAudioStreamServerStarter
import net.biodiscus.streammusic.core.streaming.http.HttpAudioStreamStarter
import net.biodiscus.streammusic.core.streaming.service.ServiceStreamingController
import okhttp3.OkHttpClient

private const val WEB_SOCKET_PING_SECONDS = 10L

@Module
@InstallIn(SingletonComponent::class)
internal abstract class StreamingModule {

    @Binds
    abstract fun bindAvTransportClient(client: SoapAvTransportClient): AvTransportClient

    @Binds
    abstract fun bindCastChannelOpener(opener: TlsCastChannelOpener): CastChannelOpener

    @Binds
    abstract fun bindAriaCastConnector(connector: WebSocketAriaCastConnector): AriaCastConnector

    @Binds
    abstract fun bindRaopConnector(connector: SocketRaopConnector): RaopConnector

    @Binds
    abstract fun bindDeviceStreamerFactory(factory: ProtocolDeviceStreamerFactory): DeviceStreamerFactory

    @Binds
    abstract fun bindStreamingController(controller: ServiceStreamingController): StreamingController

    @Binds
    abstract fun bindPhoneMusicVolume(volume: AudioManagerPhoneMusicVolume): PhoneMusicVolume

    @Binds
    abstract fun bindHttpAudioStreamStarter(starter: HttpAudioStreamServerStarter): HttpAudioStreamStarter

    companion object {
        /** A WebSocket that stays open for a whole listening session must not time out while the audio flows. */
        @Provides
        @Singleton
        fun provideOkHttpClient(): OkHttpClient =
            OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .pingInterval(WEB_SOCKET_PING_SECONDS, TimeUnit.SECONDS)
                .build()
    }
}
