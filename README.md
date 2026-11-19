<img src="docs/play-store/feature-graphic.png" alt="StreamMusic: stream your phone's audio to several speakers and TVs at once">

# StreamMusic

Stream your phone's audio to several speakers and TVs on your Wi-Fi at once. No Play Services, no account, no cloud.

## Testers wanted

StreamMusic is almost on Google Play, and I'm looking for testers.

You need an Android 14+ phone and a speaker or TV on your Wi-Fi (Chromecast, Sonos, AirPlay, DLNA, HEOS, Samsung Multiroom or AriaCast).

To join:
1. Email [streammusic@biodiscus.net](mailto:streammusic@biodiscus.net) with the Google account (Gmail address) you use on the Play Store.
2. I add you to the test list and send you a link back.
3. Open the link on your phone, accept the invitation and install StreamMusic from Google Play.

I only use your address to add you to the test list. Questions and bug reports are welcome in [issues](https://github.com/Biodiscus/StreamMusic/issues).

<p>
  <img src="docs/screenshots/devices-light.png" width="200" alt="Devices found on the network and a saved group">
  <img src="docs/screenshots/devices-selected-light.png" width="200" alt="Two devices ticked, ready to stream">
  <img src="docs/screenshots/choose-protocol-light.png" width="200" alt="Choosing the protocol for a TV that speaks two">
  <img src="docs/screenshots/streaming-light.png" width="200" alt="Streaming to two devices">
</p>
<p>
  <img src="docs/screenshots/devices-dark.png" width="200" alt="Devices found on the network and a saved group, dark theme">
  <img src="docs/screenshots/devices-selected-dark.png" width="200" alt="Two devices ticked, ready to stream, dark theme">
  <img src="docs/screenshots/choose-protocol-dark.png" width="200" alt="Choosing the protocol for a TV that speaks two, dark theme">
  <img src="docs/screenshots/streaming-dark.png" width="200" alt="Streaming to two devices, dark theme">
</p>

## Why

I used AirMusic. It was removed from the Play Store, and my copy started saying "Trial mode" and beeping over the music. So I built this.

## Use

1. Open the app. It lists the devices it finds.
2. Tick one or more, tap **Stream to N devices**.
3. Allow the microphone and Android's screen-sharing dialog.

Tick or untick devices while streaming to add or remove them. Stop from the app or the notification.

## Supported

| Protocol | Devices |
|---|---|
| UPnP / DLNA | Sonos, HEOS, Samsung Multiroom, DLNA renderers |
| Chromecast | Cast speakers and TVs |
| AirPlay 1 and 2 | Receivers that pair without a code |
| AriaCast | AriaCast receivers |

Fire TV, Spotify Connect and Bose SoundTouch are listed but not supported.

## Limits

- Needs Android 14+, same network, IPv4 only.
- Captures all phone audio, no per-app filter.
- AirPlay is tested with unit tests and local fake receivers, not real speakers yet.
- More in [CLAUDE.md](CLAUDE.md#known-limits).

## Build

JDK 21 and the Android SDK.

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

How the code is organised: [CLAUDE.md](CLAUDE.md).

## License

GNU General Public License v3.0, see [LICENSE](LICENSE). [Privacy policy](docs/PRIVACY_POLICY.md).
