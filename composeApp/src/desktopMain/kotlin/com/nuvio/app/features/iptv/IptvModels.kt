package com.nuvio.app.features.iptv

/** Mac adaptation of the Nuvio + IPTV Live TV data model (GPL-3.0). */
data class CatchupInfo(
    val type: String,
    val source: String?,
    val days: Int,
    val correctionHours: Double = 0.0,
)

data class DrmInfo(
    val scheme: String,
    val license: String,
    val licenseHeaders: Map<String, String> = emptyMap(),
    val manifestType: String? = null,
)

data class IptvSource(
    val id: String,
    val name: String,
    val url: String,
    val epgUrl: String = "",
    val kind: String = "m3u",
    val username: String = "",
    val password: String = "",
)

data class LiveChannel(
    val key: String,
    val sourceId: String,
    val sourceName: String,
    val name: String,
    val tvgId: String?,
    val tvgName: String?,
    val logo: String?,
    val group: String,
    val number: Int,
    val url: String,
    val headers: Map<String, String>,
    val catchup: CatchupInfo?,
    val drm: DrmInfo?,
)

data class EpgProgram(
    val startMs: Long,
    val stopMs: Long,
    val title: String,
    val description: String? = null,
) {
    fun isLive(nowMs: Long): Boolean = nowMs in startMs until stopMs
}

data class IptvGuide(
    val channels: List<LiveChannel> = emptyList(),
    val programs: Map<String, List<EpgProgram>> = emptyMap(),
    val errors: List<String> = emptyList(),
)
