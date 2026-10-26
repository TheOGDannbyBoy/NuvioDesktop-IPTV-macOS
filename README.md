# Nuvio IPTV for macOS

An **unofficial, experimental** Apple Silicon macOS fork of [Nuvio Desktop](https://github.com/NuvioMedia/NuvioDesktop). It adds a Live TV section using selected M3U and catch-up code adapted from [Nuvio + IPTV](https://github.com/homelessbrian/NuvioPlusIPTV). Neither upstream project endorses this build.

The app installs as **Nuvio IPTV**, with a separate bundle ID and local data folder from the official Nuvio app. It does not include channels, playlists, or media.

## Install

Download the macOS arm64 DMG from this fork's **Releases** tab, open it, and drag `Nuvio IPTV.app` into Applications. Open the app and select **Live TV** in the sidebar to add your own M3U playlist or Xtream live TV account. The app is unsigned and not notarized; only run a build from a source you trust.

## Status

This is an **alpha test build**. M3U and Xtream live playlists, XMLTV guides, channel browsing, playback through the desktop player, and common catch-up formats are implemented. Playback has not been verified with a real IPTV provider. Sports, recordings, Xtream VOD, and channel editing from the Android TV fork are not ported. IPTV provider credentials are saved locally with owner-only file permissions but without encryption.

See [macOS IPTV details and build instructions](MACOS-IPTV.md) for the full feature list and current limits. The [original Nuvio Desktop README](UPSTREAM-README.md) is retained for upstream context.

## Credits and license

The desktop base is © its contributors at [NuvioMedia/NuvioDesktop](https://github.com/NuvioMedia/NuvioDesktop). Selected IPTV parsing and catch-up behavior is adapted from [homelessbrian/NuvioPlusIPTV](https://github.com/homelessbrian/NuvioPlusIPTV). This fork remains under the [GNU General Public License v3.0](LICENSE). Use only media sources you own or are authorized to access.
