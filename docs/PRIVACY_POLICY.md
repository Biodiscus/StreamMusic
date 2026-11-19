# Privacy Policy for StreamMusic

Last updated: 19 November 2026

StreamMusic ("the app") streams the audio your phone plays to speakers and TVs on your Wi-Fi network. This policy explains what the app does with your data. The short version: **the developer collects nothing.** The app has no account, no analytics, no advertising and no server of its own.

## What the app does with data

### Audio from your phone
When you tap "Stream", Android asks you to allow screen sharing. The app uses that permission only to capture the audio your phone is playing. It sends this audio over your local network to the devices you ticked. The app never records the screen, and it does not store or upload the audio. When you press Stop, close the screen-sharing session or the app is stopped, capturing ends.

The app asks for the microphone permission only because Android requires it for capturing playback audio. The app does not use the microphone as a sound source.

### Devices on your network
To list speakers and TVs, the app searches your local network (using SSDP, mDNS and similar protocols). It reads the name, address and type of the devices that answer. This information is used on your phone to show the list and to connect. It is not sent to the developer or to any third party.

### Information stored on your phone
The app stores these on your phone only:
- Your settings (for example which protocols are shown, audio format, theme).
- Device groups you save (a name and the host, name and protocol of each device).
- The protocol you chose for a device.

You can remove all of this at any time with "Reset all data" at the bottom of the Settings screen, or by clearing the app's storage or uninstalling the app.

### Session log
The app keeps a short log of the current session in memory (the latest 500 lines) to help fix problems. It is never written to disk and never sent anywhere. The log may contain device names and local network addresses. Only if you choose "Download" on the log screen, a file is saved at the place you pick. You decide whether to share it.

## Permissions

| Permission | Why |
|---|---|
| Microphone (`RECORD_AUDIO`) | Required by Android to capture the audio your phone plays. |
| Screen sharing (system dialog) | Lets you allow audio capture for a streaming session. |
| Local network access (`ACCESS_LOCAL_NETWORK`), Internet, network and Wi-Fi state, multicast | Find and talk to devices on your local network. |
| Notifications | Show the notification with a Stop button while streaming. |
| Foreground service | Keep streaming while the app is in the background. |

The app's network traffic goes to devices on your local network. It does not contact any server on the internet.

## Sharing
The developer receives no data from the app, so there is nothing to sell or share. The audio and control messages go only to the devices you chose. Those devices (for example a Sonos speaker or a Chromecast) are made by other companies and may have their own privacy policies.

## Backups
The app stores its settings and saved groups in its private storage. If you use Android's backup feature, Android may include this data in your device backup, under the terms of your backup provider (for example Google).

## Open source
StreamMusic is open source. You can read the code, and check for yourself what the app does, at [github.com/Biodiscus/StreamMusic](https://github.com/Biodiscus/StreamMusic).

## Children
The app is not directed at children and collects no personal data from anyone.

## Changes
If this policy changes, the new version will be published at this address with a new date.

## Contact
Questions about this policy: **biodiscus**, **streammusic@biodiscus.net**
