# Nuvio + IPTV (Apple silicon)

## Download for Mac

### [⬇️ Download Nuvio + IPTV for Apple silicon (.dmg)](https://github.com/TheOGDannbyBoy/NuvioDesktop-IPTV-macOS/releases/download/macos-iptv-0.1.33-alpha/Nuvio-IPTV-macOS-arm64-0.1.33-alpha.dmg)

**For Apple silicon Macs (arm64).** [Release notes and checksums](https://github.com/TheOGDannbyBoy/NuvioDesktop-IPTV-macOS/releases/tag/macos-iptv-0.1.33-alpha)

Nuvio Desktop for Apple silicon Macs, with Live TV built in. Add an M3U playlist or Xtream account, browse channels and XMLTV guides, and watch through the desktop player. The app combines [Nuvio Desktop](https://github.com/NuvioMedia/NuvioDesktop) with selected IPTV and catch-up features adapted from [Nuvio + IPTV](https://github.com/homelessbrian/NuvioPlusIPTV).

The app installs as **Nuvio IPTV**, with a separate bundle ID and local data folder from the official Nuvio app. It does not include channels, playlists, or media.

## Install

Download the DMG using the link above, open it, and drag `Nuvio IPTV.app` into Applications. Open the app and select **Live TV** in the sidebar to add your own M3U playlist or Xtream live TV account. The app is unsigned and not notarized; only run a build from a source you trust.

## What works

IPTV playback has been confirmed in this Mac build. It supports M3U and Xtream live playlists, XMLTV guides, channel browsing, desktop playback, and common catch-up formats. Compatibility varies by provider. Sports, recordings, Xtream VOD, and channel editing from the Android TV fork are not included. IPTV provider credentials are saved locally with owner-only file permissions but without encryption.

See [macOS IPTV details and build instructions](MACOS-IPTV.md) for the full feature list and current limits. The [original Nuvio Desktop README](UPSTREAM-README.md) is retained for upstream context.

## Credits and license

The desktop base is © its contributors at [NuvioMedia/NuvioDesktop](https://github.com/NuvioMedia/NuvioDesktop). Selected IPTV parsing and catch-up behavior is adapted from [homelessbrian/NuvioPlusIPTV](https://github.com/homelessbrian/NuvioPlusIPTV). This fork remains under the [GNU General Public License v3.0](LICENSE). Use only media sources you own or are authorized to access.
