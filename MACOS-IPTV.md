# Nuvio + IPTV for macOS

This Apple silicon macOS build combines [Nuvio Desktop](https://github.com/NuvioMedia/NuvioDesktop) with a Live TV section adapted from [Nuvio + IPTV](https://github.com/homelessbrian/NuvioPlusIPTV). IPTV playback has been confirmed in this build. It installs separately from the original `Nuvio.app`, with its own bundle ID, app data, cache, and URL scheme.

## Current features

- Add more than one M3U playlist or Xtream live TV account.
- Load XMLTV guides from a URL, the playlist header, or Xtream's XMLTV endpoint.
- Open **Live TV** from the desktop sidebar (or top navigation), browse groups, search channels, see guide listings, and play channels with Nuvio Desktop's macOS player.
- Use M3U request headers and common catch-up URL formats for replay and watch-from-start.
- Save sources in `~/Library/Application Support/Nuvio IPTV/sources.json` with owner-only file permissions. Provider credentials in this file are **not encrypted**. Anyone with access to your macOS user account can read them.

The macOS **IPTV** menu also has an **Open Live TV** shortcut. Use the sidebar to move between Live TV and Nuvio's other sections, or use the back button in Live TV.

## Current limits

This build is not feature-for-feature parity with the Android TV fork. Its Sports hub, recordings, Xtream VOD library, channel editing, and TV remote interface have not been ported. DRM playback depends on the desktop player and has not been verified. The user has confirmed their IPTV works in this build; other providers remain unverified. Test with a source you are authorized to use.

The DMG is for Apple Silicon (arm64) Macs. The app is unsigned and not notarized. macOS may require you to approve it in Privacy & Security. Do not replace your official Nuvio app with this build.

## Build

The build needs Xcode, a JDK, Gradle, Git LFS, and the official `Nuvio.app` installed at `/Applications/Nuvio.app`. Fetch the arm64 macOS LFS media libraries and run:

```sh
git lfs install --local
git lfs pull -I 'composeApp/src/desktopMain/native/macos/runtime/arm64/*'
java scripts/BuildMacIptvFromInstalledNuvio.java
```

The build helper reads only the official app's **publishable** client configuration in memory so sign-in can connect to the same official service. It does not read account passwords or session data and does not print those values. Sentry configuration is left blank. You can instead provide your own build environment values for `NUVIO_SUPABASE_URL`, `NUVIO_SUPABASE_ANON_KEY`, and `TMDB_API_KEY` when building directly with Gradle.

The source used here was Nuvio Desktop commit `5e18596aa919a6a4e7b6cdee7f90dfaabbf71b00` and Nuvio + IPTV commit `50e713e33b9ed6fc48bd3bdec5a773e0a552d160`. The adapted M3U and catch-up parsers retain the fork's GPL-3.0 terms; the complete project remains GPL-3.0.
