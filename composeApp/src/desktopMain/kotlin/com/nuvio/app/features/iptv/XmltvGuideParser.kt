package com.nuvio.app.features.iptv

import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants

internal data class XmltvGuide(
    val programs: Map<String, List<EpgProgram>>,
    val idsByName: Map<String, String>,
)

/** Desktop StAX adaptation of the Android fork's streaming XMLTV parser. */
internal object XmltvGuideParser {
    fun parse(input: InputStream, fromMs: Long, untilMs: Long): XmltvGuide {
        val factory = XMLInputFactory.newFactory().apply {
            setProperty(XMLInputFactory.SUPPORT_DTD, false)
            setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false)
            setProperty(XMLInputFactory.IS_COALESCING, true)
        }
        val reader = factory.createXMLStreamReader(input)
        val programs = LinkedHashMap<String, MutableList<EpgProgram>>()
        val idsByName = LinkedHashMap<String, String>()
        var programCount = 0
        try {
            while (reader.hasNext() && programCount < 250_000) {
                when (reader.next()) {
                    XMLStreamConstants.START_ELEMENT -> when (reader.localName) {
                        "channel" -> {
                            val id = reader.getAttributeValue(null, "id") ?: continue
                            while (reader.hasNext()) {
                                val event = reader.next()
                                if (event == XMLStreamConstants.START_ELEMENT && reader.localName == "display-name") {
                                    idsByName[reader.elementText.trim().lowercase(Locale.ROOT)] = id
                                } else if (event == XMLStreamConstants.END_ELEMENT && reader.localName == "channel") break
                            }
                        }
                        "programme" -> {
                            val channelId = reader.getAttributeValue(null, "channel") ?: continue
                            val start = parseTime(reader.getAttributeValue(null, "start")) ?: continue
                            val stop = parseTime(reader.getAttributeValue(null, "stop")) ?: continue
                            var title = ""
                            var description: String? = null
                            while (reader.hasNext()) {
                                val event = reader.next()
                                if (event == XMLStreamConstants.START_ELEMENT) {
                                    when (reader.localName) {
                                        "title" -> title = reader.elementText.trim()
                                        "desc" -> description = reader.elementText.trim()
                                    }
                                } else if (event == XMLStreamConstants.END_ELEMENT && reader.localName == "programme") break
                            }
                            if (stop > fromMs && start < untilMs && title.isNotBlank()) {
                                programs.getOrPut(channelId) { ArrayList() }
                                    .add(EpgProgram(start, stop, title, description))
                                programCount++
                            }
                        }
                    }
                }
            }
        } finally {
            reader.close()
        }
        return XmltvGuide(programs.mapValues { (_, list) -> list.sortedBy { it.startMs } }, idsByName)
    }

    private fun parseTime(raw: String?): Long? {
        val value = raw?.trim() ?: return null
        val match = Regex("""^(\d{8,14})(?:\s*([+-]\d{4}))?.*""").matchEntire(value) ?: return null
        val digits = match.groupValues[1].padEnd(14, '0')
        val zone = match.groupValues[2].ifBlank { "+0000" }
        val formatter = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return runCatching { formatter.parse("$digits $zone")?.time }.getOrNull()
    }
}
