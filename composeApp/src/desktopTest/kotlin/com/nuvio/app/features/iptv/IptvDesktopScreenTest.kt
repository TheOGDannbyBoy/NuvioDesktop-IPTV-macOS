package com.nuvio.app.features.iptv

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.nuvio.app.core.ui.NuvioTheme
import java.nio.file.Files
import org.junit.Rule
import kotlin.test.Test

class IptvDesktopScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun savedPlaylistAppearsInLiveTvSection() {
        val dir = Files.createTempDirectory("nuvio-iptv-ui-test-")
        System.setProperty("nuvio.iptv.storageDir", dir.toString())
        try {
            val playlist = dir.resolve("demo.m3u")
            Files.writeString(playlist, "#EXTM3U\n#EXTINF:-1 tvg-id=\"demo\" group-title=\"Demo\",Demo Channel\nfile:///tmp/demo.mp4\n")
            IptvSourceStore.save(listOf(IptvSource("demo", "Local test", playlist.toUri().toString())))
            compose.setContent { NuvioTheme { IptvDesktopScreen(onBack = {}) } }
            compose.waitUntil(timeoutMillis = 20_000) {
                compose.onAllNodesWithText("1. Demo Channel").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("1. Demo Channel").assertExists()
        } finally {
            System.clearProperty("nuvio.iptv.storageDir")
            Files.list(dir).use { files -> files.forEach(Files::deleteIfExists) }
            Files.deleteIfExists(dir)
        }
    }
}
