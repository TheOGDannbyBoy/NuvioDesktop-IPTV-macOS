package com.nuvio.app.features.iptv

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale
import java.util.zip.GZIPInputStream

internal object IptvLoader {
    suspend fun load(sources: List<IptvSource>): IptvGuide = withContext(Dispatchers.IO) {
        val channels = ArrayList<LiveChannel>()
        val epgUrls = LinkedHashSet<String>()
        val errors = ArrayList<String>()

        sources.forEach { source ->
            try {
                val playlistUrl = if (source.kind == "xtream") xtreamUrl(source, "get.php", "type=m3u_plus&output=ts") else source.url
                open(playlistUrl).use { input ->
                    val playlist = M3uParser.parse(input.bufferedReader())
                    playlist.entries.forEachIndexed { index, entry ->
                        channels += LiveChannel(
                            key = source.id + ":" + stableId("${entry.tvgId}|${entry.url}|$index"),
                            sourceId = source.id,
                            sourceName = source.name,
                            name = entry.name,
                            tvgId = entry.tvgId,
                            tvgName = entry.tvgName,
                            logo = entry.logo,
                            group = entry.group?.ifBlank { null } ?: "Other",
                            number = entry.chno ?: index + 1,
                            url = entry.url,
                            headers = entry.headers,
                            catchup = entry.catchup,
                            drm = entry.drm,
                        )
                    }
                    epgUrls += playlist.epgUrls
                }
                if (source.epgUrl.isNotBlank()) epgUrls += source.epgUrl
                if (source.kind == "xtream") epgUrls += xtreamUrl(source, "xmltv.php")
            } catch (e: Exception) {
                errors += "${source.name}: ${safeError(e)}"
            }
        }

        val now = System.currentTimeMillis()
        val epgById = LinkedHashMap<String, List<EpgProgram>>()
        val idsByName = LinkedHashMap<String, String>()
        epgUrls.forEachIndexed { index, epgUrl ->
            try {
                open(epgUrl).use { input ->
                    val parsed = XmltvGuideParser.parse(input, now - 7L * 86_400_000, now + 7L * 86_400_000)
                    parsed.programs.forEach { (id, programs) -> epgById[id] = programs }
                    idsByName.putAll(parsed.idsByName)
                }
            } catch (e: Exception) {
                errors += "Guide ${index + 1}: ${safeError(e)}"
            }
        }
        val programs = channels.associate { channel ->
            val id = channel.tvgId?.takeIf { it in epgById }
                ?: channel.tvgName?.lowercase(Locale.ROOT)?.let(idsByName::get)
                ?: idsByName[channel.name.lowercase(Locale.ROOT)]
            channel.key to (id?.let(epgById::get) ?: emptyList())
        }
        IptvGuide(channels.sortedWith(compareBy(LiveChannel::sourceName, LiveChannel::group, LiveChannel::number)), programs, errors)
    }

    private fun open(rawUrl: String): InputStream {
        val uri = URI.create(rawUrl.trim())
        require(uri.scheme?.lowercase() in setOf("http", "https", "file")) { "Use an HTTP(S) or file URL" }
        val connection = uri.toURL().openConnection()
        connection.connectTimeout = 20_000
        connection.readTimeout = 90_000
        connection.setRequestProperty("Accept-Encoding", "gzip")
        connection.setRequestProperty("User-Agent", "NuvioIPTVMac/0.1")
        if (connection is HttpURLConnection) {
            val status = connection.responseCode
            if (status !in 200..299) {
                connection.disconnect()
                error("HTTP $status")
            }
        }
        val input = BufferedInputStream(connection.getInputStream())
        return if (uri.path?.endsWith(".gz", ignoreCase = true) == true || connection.contentEncoding.equals("gzip", true)) {
            GZIPInputStream(input)
        } else input
    }

    private fun xtreamUrl(source: IptvSource, endpoint: String, extra: String = ""): String {
        val base = source.url.trim().trimEnd('/')
        require(base.startsWith("http://") || base.startsWith("https://")) { "Xtream server must be HTTP(S)" }
        fun encoded(value: String) = URLEncoder.encode(value, StandardCharsets.UTF_8)
        val query = "username=${encoded(source.username)}&password=${encoded(source.password)}"
        return "$base/$endpoint?$query${if (extra.isEmpty()) "" else "&$extra"}"
    }

    private fun stableId(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8))
        .take(8)
        .joinToString("") { "%02x".format(it) }

    private fun safeError(error: Exception): String = when {
        error.message?.matches(Regex("HTTP [0-9]{3}")) == true -> error.message!!
        error is IllegalArgumentException -> "Invalid source URL"
        else -> error.javaClass.simpleName
    }
}
