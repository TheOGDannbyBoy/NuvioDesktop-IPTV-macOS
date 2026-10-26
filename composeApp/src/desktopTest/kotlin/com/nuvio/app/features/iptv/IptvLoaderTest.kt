package com.nuvio.app.features.iptv

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IptvLoaderTest {
    @Test
    fun loadsPlaylistAndMatchesXmltvGuide() = runBlocking {
        val dir = Files.createTempDirectory("nuvio-iptv-test-")
        try {
            val guideFile = dir.resolve("guide.xml")
            Files.writeString(guideFile, guideXml())
            val playlistFile = dir.resolve("channels.m3u")
            Files.writeString(playlistFile, playlist())
            val result = IptvLoader.load(
                listOf(IptvSource("test", "Test source", playlistFile.toUri().toString(), guideFile.toUri().toString())),
            )
            assertEquals(emptyList(), result.errors)
            assertEquals(1, result.channels.size)
            val channel = result.channels.single()
            assertEquals("Demo Channel", channel.name)
            assertEquals("Demo", channel.group)
            assertEquals("TestAgent", channel.headers["User-Agent"])
            assertEquals("Live fixture", result.programs[channel.key]?.firstOrNull()?.title)
        } finally {
            Files.list(dir).use { files -> files.forEach(Files::deleteIfExists) }
            Files.deleteIfExists(dir)
        }
    }

    @Test
    fun loadsXtreamPlaylistAndGuideFromProviderEndpoints() = runBlocking {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val requested = mutableListOf<String>()
        server.createContext("/get.php") { exchange ->
            requested += exchange.requestURI.rawQuery.orEmpty()
            val bytes = playlist().toByteArray(StandardCharsets.UTF_8)
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.createContext("/xmltv.php") { exchange ->
            requested += exchange.requestURI.rawQuery.orEmpty()
            val bytes = guideXml().toByteArray(StandardCharsets.UTF_8)
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            val source = IptvSource("xtream", "Demo provider", "http://127.0.0.1:${server.address.port}", kind = "xtream", username = "demo user", password = "test pass")
            val result = IptvLoader.load(listOf(source))
            assertEquals(emptyList(), result.errors)
            assertEquals("Live fixture", result.programs[result.channels.single().key]?.single()?.title)
            assertEquals(2, requested.size)
            assertTrue(requested.all { "username=demo+user" in it && "password=test+pass" in it })
        } finally {
            server.stop(0)
        }
    }

    private fun playlist(): String = """
        #EXTM3U
        #EXTINF:-1 tvg-id="demo" tvg-name="Demo Channel" group-title="Demo",Demo Channel
        #EXTVLCOPT:http-user-agent=TestAgent
        file:///tmp/demo.mp4
    """.trimIndent()

    private fun guideXml(): String {
        val start = System.currentTimeMillis() - 60_000
        val stop = start + 120_000
        val formatter = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        return """<tv><channel id="demo"><display-name>Demo Channel</display-name></channel><programme channel="demo" start="${formatter.format(Date(start))}" stop="${formatter.format(Date(stop))}"><title>Live fixture</title></programme></tv>"""
    }
}
