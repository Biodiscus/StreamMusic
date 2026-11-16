# StreamMusic

Android app that finds speakers and TVs on the local network and streams the audio of the phone to several of them at once. Package `net.biodiscus.streammusic`, minSdk 29 (capturing other apps' audio needs Android 10), targetSdk 37, Kotlin, Jetpack Compose, Hilt, Gradle Kotlin DSL with a version catalog (`gradle/libs.versions.toml`).

## Android skill

The `.claude/skills/claude-android-skill/` folder in this repo is the source of truth for Android architecture, Compose, modularization, Gradle and testing. Always follow it when creating or changing code.

@.claude/skills/claude-android-skill/SKILL.md

Before working on a topic, read the matching reference first:

| Working on | Read |
|---|---|
| Layers, repositories, ViewModels, UiState | `.claude/skills/claude-android-skill/references/architecture.md` |
| Screens, state hoisting, navigation, theming, previews | `.claude/skills/claude-android-skill/references/compose-patterns.md` |
| New modules, dependency rules between modules | `.claude/skills/claude-android-skill/references/modularization.md` |
| Build files, convention plugins, version catalog | `.claude/skills/claude-android-skill/references/gradle-setup.md` |
| Writing tests, test doubles | `.claude/skills/claude-android-skill/references/testing.md` |

`.claude/skills/claude-android-skill/scripts/generate_feature.py` scaffolds a feature module. Prefer it over writing the boilerplate by hand.

The project's own files win over the skill where they differ:
- The skill's example version numbers are placeholders. Take real versions from `gradle/libs.versions.toml`.
- The app is a single `:app` module. Compose, Hilt (with KSP), Navigation and OkHttp are set up. Room and a real multi-module split are not. The packages are laid out like the modules of the skill (see "Project structure"), so a split later is mostly moving packages. Check the build files before assuming a dependency exists, and add it to the version catalog when a task needs it.
- There are no convention plugins and no `build-logic/`. `generate_feature.py` creates api/impl modules and does not fit the single-module layout yet, so features are written by hand in `feature/<name>/`.

## Project structure

### What the app does

1. **Devices screen** searches the network and lists every device it finds. The user ticks one or more devices that can receive audio.
2. The user taps "Stream to N devices". The app asks for the microphone permission (the audio-capture API needs it) and then shows **Android's own screen-sharing dialog**. That dialog is the only place the user chooses what to share. The app cannot see which app was chosen, so it captures all audio the phone plays.
3. A **foreground service** captures the audio and streams it to every chosen device. The **Now streaming screen** shows one status per device (connecting, streaming, failed) and a Stop button. A notification with a Stop action keeps it controllable from other apps.

### Packages (`app/src/main/java/net/biodiscus/streammusic/`)

Each top-level package is shaped like a module of the skill. Keep the dependency direction: `feature` -> `core`, never the other way around, and never between two features.

```
core/model/       Pure Kotlin data: StreamingDevice, NetworkDevice, DeviceProtocol, StreamTransport, StreamingState.
                  Depends on nothing.
core/discovery/   Finding devices.
  ssdp/           SSDP multicast search (UPnP, DLNA, Sonos, HEOS, Samsung Multiroom). Message building/parsing.
  upnp/           Downloads and parses a UPnP device description; decides which protocol a device is.
  mdns/           mDNS/Bonjour browsing with NsdManager (Chromecast, AirPlay, AriaCast, Fire TV, Spotify Connect,
                  Bose SoundTouch). The table of service types is in MdnsServices.kt.
  DeviceRepository / NetworkDeviceRepository   Merges all discoveries into one sorted list, one entry per device id.
core/streaming/   Sending audio.
  capture/        PlaybackAudioCapture: AudioRecord + MediaProjection -> 20 ms PCM frames.
  aac/            AacEncoder (MediaCodec), AdtsHeader, AacHttpStream (tiny HTTP server for /stream.aac).
  device/         One DeviceStreamer per protocol family (upnp/, cast/, ariacast/) and the factory that picks one.
  StreamingSession          Runs one session: shares the audio, starts the streamers, tracks per-device status.
  StreamingStateRepository  What the screens observe about the (service-owned) session.
  service/        StreamingService (foreground service), StreamingNotification, StreamingRequest (Intent packing).
feature/devices/    Devices screen: route, screen, ViewModel, UiState, permission and stream-start flow.
feature/streaming/  Now streaming screen: route, screen, ViewModel, UiState.
ui/                 Theme and preview annotations.
```

Each `*Module.kt` under a `di/` folder holds the Hilt bindings of its package. Everything is `internal` except what Android or Hilt must see (Application, Activity, Service, repositories used by ViewModels).

### How the audio flows

```
PlaybackAudioCapture -> 20 ms PCM frames (48 kHz, 16-bit, stereo, 3840 bytes)
        |  shared as a hot flow; a slow listener loses its oldest frames instead of blocking the recording
        +--> AriaCastDeviceStreamer: sends the raw PCM frames over a WebSocket
        +--> AacEncoder -> ADTS frames -> AacHttpStream serves http://<phone>:<port>/stream.aac
                 ^ devices pull this URL; UpnpDeviceStreamer and ChromecastDeviceStreamer tell them to
```

The AAC stream is only started when at least one chosen device pulls an HTTP stream.

### Protocols

`DeviceProtocol.transport` says how audio reaches a device and is the single source of truth for what is supported:

| Protocol | Found with | Streaming |
|---|---|---|
| Sonos, HEOS, Samsung Multiroom, DLNA/UPnP | SSDP | UPnP AVTransport (`SetAVTransportURI`, `Play`, `Stop`) to the AAC URL. Sonos needs the `x-rincon-mp3radio://` scheme. |
| Chromecast | mDNS `_googlecast._tcp` | Cast v2 protocol over TLS (port 8009), written by hand without Play Services: launch the Default Media Receiver, `LOAD` the AAC URL. |
| AriaCast | mDNS `_audiocast._tcp` | WebSocket `ws://<host>:12889/audio`, raw PCM. The port is the documented default, not read from mDNS. |
| AirPlay, Fire TV, Spotify Connect, Bose SoundTouch | mDNS | **Not supported.** They are listed with a "found, but not supported" label and cannot be selected. |

A device that is reachable by two methods of the same protocol (for example AirPlay announces `_airplay` and `_raop`) is one `StreamingDevice`, because its `id` is `protocol@host`. A device that speaks several protocols (a TV that is both Chromecast and DLNA) is found once per protocol. The Devices screen joins those finds by host into one `NetworkDevice`, and streams with its most preferred supported protocol (`STREAMING_PREFERENCE` in `NetworkDevice.kt`).

### Contracts that matter

- **`DeviceStreamer.stream(onStreaming): Nothing`** never returns. Cancelling the coroutine stops the device (cleanup runs in `finally` with `NonCancellable`). An exception means that device failed. `StreamingSession` catches it per device, so one failing device never stops the others.
- **`StreamingSession.run()` switches to `Dispatchers.IO` itself.** The service calls it from the main thread, where every socket call throws `NetworkOnMainThreadException`. Any new code that touches the network must run under it or on `Dispatchers.IO`.
- **Discovery flows run only while collected.** `DevicesViewModel` collects them while the Devices screen is visible. Blocking socket reads are unblocked by closing the socket in `awaitClose`, because cancellation does not interrupt them.
- **Only IPv4 devices are listed.** Stream URLs and control URLs are built from the IPv4 address (`isIpv4Address`).
- **Android 17 (API 37)** needs the `ACCESS_LOCAL_NETWORK` permission to talk to local devices. The Devices screen asks for it before searching.

### How to extend

- **A new UPnP-like brand:** add a branch to `classifyUpnpDevice` (+ test). It streams automatically through the UPnP streamer.
- **A new mDNS device type:** add a row to `MDNS_SERVICE_KINDS` and, if its name needs cleaning, to `friendlyMdnsName` (+ test). Add the enum entry in `DeviceProtocol` with `NOT_SUPPORTED`.
- **Streaming to a new protocol:** add a `DeviceProtocol` entry with its `StreamTransport`, write a `DeviceStreamer` that follows the contract above, add it to `ProtocolDeviceStreamerFactory` and to `STREAMING_PREFERENCE`, and write its tests with a hand-written fake of its connection (see `ChromecastDeviceStreamerTest`).

### Known limits

- No per-app audio filter: all phone audio is captured, and apps that opt out of capture arrive as silence.
- A Sonos speaker that is a non-coordinator member of a group refuses to play. This is not handled.
- Plain DLNA renderers must be able to play AAC (ADTS) streams.
- The Chromecast connection accepts any TLS certificate, because Cast devices use certificates Android does not trust. Only control messages use it.

## Commands

```bash
./gradlew testDebugUnitTest          # JVM unit tests (run after every change)
./gradlew testDebugUnitTest --tests "net.biodiscus.streammusic.SomeTest"
./gradlew connectedDebugAndroidTest  # instrumented tests, needs a device or emulator
./gradlew assembleDebug              # build
./gradlew lintDebug                  # lint
```

Android Studio is installed as a Flatpak, which gives it a private `/tmp`. A Gradle daemon started from a normal terminal cannot see the init scripts the IDE writes there, so a later IDE sync can fail with `The specified initialization script '/tmp/ijMapper1.gradle' does not exist`. Run `./gradlew --stop` after terminal builds, or add `--no-daemon`.

## Clean Code

Clean Code is the main quality bar in this project. When in doubt, choose the more readable option.

**Write for a junior developer.** Every piece of code must be understandable by a developer with little experience who is new to this codebase. They should be able to read a method and know what it does without asking anyone.
- Choose the plain, obvious solution over the clever one. No dense one-liners, deeply chained operators, or tricks that need a second read. Use a named intermediate value or an extracted method instead.
- Use well-known patterns and the standard library before inventing abstractions. Add an abstraction only when it removes real duplication or complexity today, not for a future that may never come.
- Use domain words from the music world (`Track`, `Playlist`, `PlaybackPosition`), not abbreviations or generic names like `data`, `item`, `manager`, `helper`, `util`.
- Avoid advanced language features when a simple one does the job: heavy generics, operator overloading, reflection, nested scope functions (`let`/`run`/`apply`/`also` stacked), and complex `Flow` operator chains without named steps.
- Keep nesting shallow. Use early returns and small methods instead of deep `if`/`when`/loop nesting.
- When you do use an advanced concept that the code needs (a coroutine operator, a Compose side effect, a Gradle convention plugin), isolate it in a small, well-named method so the rest of the code stays simple. Explain the *why* only if the decision is not obvious, following the comment rule below.
- Before finishing, reread your code as a newcomer would. If any line makes you pause, rename it or simplify it.

- **Names carry the meaning.** A name says exactly what the thing is or does: `isPlaylistEmpty()`, `formatDurationAsMinutesAndSeconds()`, not `check()` or `process()`. If you cannot name a function precisely, it is doing too much. Split it.
- **Small methods with one purpose.** Each method has one clear job, a clear input and a clear output. Prefer pure functions: the same input gives the same output and nothing else changes. Keep side effects (I/O, state changes) at the edges, in thin methods of their own.
- **A comment is a missing method.** Before writing a comment, extract the commented block into a method whose name says what the comment would have said. Then delete the comment.
- **No dead code, no commented-out code, no TODOs left behind.**
- **Follow the Kotlin conventions:** immutable by default (`val`, `data class`, read-only collections), expression bodies for one-liners, sealed types for closed sets of states, no `!!`.
- **Keep changes minimal.** Do not refactor or reformat code that is unrelated to the task. Mention it instead.

### Comments

Write a comment only when it explains a decision that the code cannot show: why this approach, why not the obvious one, a workaround for a platform bug, a constraint from outside the code. Never write a comment that says what the code does or restates the method name. KDoc on public API is not needed unless the contract is not clear from the signature.

## Testing

A **unit** is one small method with a clear purpose, input and output. Each unit gets its own tests. Because the methods are small and pure, the tests stay short and easy to read.

- **Write the unit test together with the method.** A new or changed method is not done until its tests exist and pass. Fixing a bug starts with a failing test that reproduces it.
- **Design for testability.** If a method is hard to test, the method is the problem. Extract the logic into a small pure method and test that.
- **One behavior per test.** Test names describe the behavior in plain words, in backticks: ``fun `formats 125 seconds as 2 minutes 5 seconds`()``. The name should make a failure understandable without opening the test.
- **Arrange, act, assert.** Separate the three parts with blank lines, no comments needed. Prefer one assertion per test, or a few that check one result.
- **No mocking libraries.** Use hand-written test doubles that implement the real interface (see `testing.md`). Shared ones live in `app/src/test/java/net/biodiscus/streammusic/testing/` (`FakeDeviceRepository`, `FakeDeviceDiscovery`, `MainDispatcherRule`, `aDevice(...)`) and move to a testing module if the project is split. Code that needs real hardware (multicast and mDNS sockets, `AudioRecord`, `MediaCodec`, the HTTP server) is kept thin and is not unit tested. The logic around it is, through small pure functions and fake connections.
- **Tests are deterministic.** No real network, no real clock, no sleeping. Inject a clock or dispatcher when needed and use Turbine for `Flow`.
- **Keep the tests as clean as the production code:** small helpers with good names for building test data, no copy-pasted setup.
- Unit tests go in `app/src/test/java/net/biodiscus/streammusic/` (mirror the package of the class under test). Instrumented and Compose UI tests go in `app/src/androidTest/`. UI tests are for wiring, not for logic that can be a unit test.

## Workflow

1. Read the relevant skill reference.
2. Write the smallest method that does the job, with a precise name.
3. Write its unit tests.
4. Run `./gradlew testDebugUnitTest` and make sure it is green before reporting the work as done.
