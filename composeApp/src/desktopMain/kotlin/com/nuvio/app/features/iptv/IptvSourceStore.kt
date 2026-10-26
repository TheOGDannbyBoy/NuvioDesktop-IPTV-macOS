package com.nuvio.app.features.iptv

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermission

/** Private per-user source storage. IPTV URLs and Xtream logins may contain credentials. */
internal object IptvSourceStore {
    private val json = Json { ignoreUnknownKeys = true }

    private fun path(): Path {
        val override = System.getProperty("nuvio.iptv.storageDir")
        val dir = if (override != null) Path.of(override) else Path.of(
            System.getProperty("user.home"), "Library", "Application Support", "Nuvio IPTV",
        )
        return dir.resolve("sources.json")
    }

    fun load(): List<IptvSource> = runCatching {
        val file = path()
        if (!Files.isRegularFile(file)) return emptyList()
        json.parseToJsonElement(Files.readString(file)).jsonArray.mapNotNull { entry ->
            val o = entry.jsonObject
            val id = o.value("id") ?: return@mapNotNull null
            val url = o.value("url") ?: return@mapNotNull null
            IptvSource(
                id = id,
                name = o.value("name") ?: "Playlist",
                url = url,
                epgUrl = o.value("epgUrl").orEmpty(),
                kind = o.value("kind") ?: "m3u",
                username = o.value("username").orEmpty(),
                password = o.value("password").orEmpty(),
            )
        }
    }.getOrDefault(emptyList())

    fun save(sources: List<IptvSource>) {
        val file = path()
        Files.createDirectories(file.parent)
        val payload = buildJsonArray {
            sources.forEach { source ->
                add(buildJsonObject {
                    put("id", JsonPrimitive(source.id))
                    put("name", JsonPrimitive(source.name))
                    put("url", JsonPrimitive(source.url))
                    put("epgUrl", JsonPrimitive(source.epgUrl))
                    put("kind", JsonPrimitive(source.kind))
                    put("username", JsonPrimitive(source.username))
                    put("password", JsonPrimitive(source.password))
                })
            }
        }
        val temporary = Files.createTempFile(file.parent, "sources-", ".json")
        try {
            setOwnerOnly(temporary)
            Files.writeString(temporary, payload.toString())
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            setOwnerOnly(file)
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    private fun setOwnerOnly(file: Path) {
        runCatching {
            Files.setPosixFilePermissions(
                file,
                setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE),
            )
        }
    }

    private fun JsonObject.value(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull
}
