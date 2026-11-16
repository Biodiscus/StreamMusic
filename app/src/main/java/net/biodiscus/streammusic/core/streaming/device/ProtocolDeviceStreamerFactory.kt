package net.biodiscus.streammusic.core.streaming.device

import javax.inject.Inject
import net.biodiscus.streammusic.core.model.DeviceProtocol
import net.biodiscus.streammusic.core.model.StreamingDevice
import net.biodiscus.streammusic.core.settings.SettingsRepository
import net.biodiscus.streammusic.core.streaming.device.airplay.RaopConnector
import net.biodiscus.streammusic.core.streaming.device.airplay.RaopDeviceStreamer
import net.biodiscus.streammusic.core.streaming.device.airplay2.AirPlay2DeviceStreamer
import net.biodiscus.streammusic.core.streaming.device.ariacast.AriaCastConnector
import net.biodiscus.streammusic.core.streaming.device.ariacast.AriaCastDeviceStreamer
import net.biodiscus.streammusic.core.streaming.device.cast.CastChannelOpener
import net.biodiscus.streammusic.core.streaming.device.cast.ChromecastDeviceStreamer
import net.biodiscus.streammusic.core.streaming.device.upnp.AvTransportClient
import net.biodiscus.streammusic.core.streaming.device.upnp.UpnpDeviceStreamer

internal class ProtocolDeviceStreamerFactory @Inject constructor(
    private val avTransportClient: AvTransportClient,
    private val castChannelOpener: CastChannelOpener,
    private val ariaCastConnector: AriaCastConnector,
    private val raopConnector: RaopConnector,
    private val settingsRepository: SettingsRepository,
) : DeviceStreamerFactory {

    override fun create(device: StreamingDevice, audio: SessionAudio): DeviceStreamer {
        val settings = settingsRepository.currentSettings
        return when (device.protocol) {
            DeviceProtocol.SONOS,
            DeviceProtocol.HEOS,
            DeviceProtocol.SAMSUNG_MULTIROOM,
            DeviceProtocol.DLNA,
            -> UpnpDeviceStreamer(device, audio, avTransportClient)

            DeviceProtocol.CHROMECAST -> ChromecastDeviceStreamer(
                device,
                audio,
                disconnectsOnMute = settings.disconnectsChromecastOnMute,
                channelOpener = castChannelOpener,
            )
            DeviceProtocol.ARIACAST -> AriaCastDeviceStreamer(device, audio, ariaCastConnector)
            DeviceProtocol.AIRPLAY -> RaopDeviceStreamer(
                device,
                audio,
                raopConnector,
                audioDelayMillis = settings.airPlayAudioDelayMillis,
            )
            DeviceProtocol.AIRPLAY_2 -> AirPlay2DeviceStreamer(
                device,
                audio,
                raopConnector,
                audioDelayMillis = settings.airPlayAudioDelayMillis,
            )

            DeviceProtocol.FIRE_TV,
            DeviceProtocol.SPOTIFY_CONNECT,
            DeviceProtocol.BOSE_SOUNDTOUCH,
            -> error("Streaming to ${device.protocol.displayName} is not supported")
        }
    }
}
