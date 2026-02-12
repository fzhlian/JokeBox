package com.jokebox.app.domain.pipeline

import com.jokebox.app.data.db.RawQueueDao
import com.jokebox.app.data.db.RawQueueEntity
import com.jokebox.app.data.model.RawStatus
import com.jokebox.app.data.model.SourceType
import org.json.JSONArray
import org.json.JSONObject

class Importer(private val rawDao: RawQueueDao) {
    suspend fun importText(sourceId: String, text: String, language: String, format: String) {
        val lines = when (format.lowercase()) {
            "json" -> parseJson(text)
            "csv" -> parseCsv(text)
            "html" -> parseHtml(text)
            else -> parseTxt(text)
        }
        val now = System.currentTimeMillis()
        val items = lines.map {
            RawQueueEntity(
                ownerSourceId = sourceId,
                ownerSourceType = SourceType.USER_OFFLINE,
                language = language,
                ageGroupHint = null,
                payloadJson = JSONObject(mapOf("content" to it, "language" to language)).toString(),
                fetchedAt = now,
                status = RawStatus.PENDING
            )
        }
        rawDao.insertAll(items)
    }

    private fun parseTxt(text: String): List<String> = text.lines().map { it.trim() }.filter { it.isNotBlank() }

    private fun parseJson(text: String): List<String> {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return emptyList()
        return if (trimmed.startsWith("[")) {
            val arr = JSONArray(trimmed)
            buildList {
                for (i in 0 until arr.length()) {
                    val item = arr.opt(i)
                    when (item) {
                        is String -> if (item.isNotBlank()) add(item)
                        is JSONObject -> {
                            val content = item.optString("content")
                            if (content.isNotBlank()) add(content)
                        }
                    }
                }
            }
        } else {
            val obj = JSONObject(trimmed)
            if (obj.has("items")) {
                val arr = obj.optJSONArray("items") ?: return emptyList()
                buildList {
                    for (i in 0 until arr.length()) {
                        val item = arr.opt(i)
                        when (item) {
                            is String -> if (item.isNotBlank()) add(item)
                            is JSONObject -> {
                                val content = item.optString("content")
                                if (content.isNotBlank()) add(content)
                            }
                        }
                    }
                }
            } else {
                listOfNotNull(obj.optString("content").takeIf { it.isNotBlank() })
            }
        }
    }

    private fun parseCsv(text: String): List<String> {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()
        return lines.drop(1).mapNotNull { line ->
            val first = line.split(",").firstOrNull()?.trim('"', ' ', '\t')
            first?.takeIf { it.isNotBlank() }
        }
    }

    private fun parseHtml(text: String): List<String> {
        val noTags = text.replace(Regex("<[^>]+>"), "\n")
        return noTags.lines().map { it.trim() }.filter { it.length >= 6 }
    }
}
