package com.jokebox.app.domain.pipeline

import com.jokebox.app.data.model.OnlineSourceConfig
import com.jokebox.app.data.model.RequestSpec
import com.jokebox.app.data.model.ResponseSpec
import com.jokebox.app.data.model.MappingSpec
import com.jokebox.app.data.repo.SettingsStore
import com.jokebox.app.data.repo.SourceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

class Fetcher(
    private val sourceRepository: SourceRepository,
    private val settingsStore: SettingsStore,
    private val client: OkHttpClient = OkHttpClient()
) {
    suspend fun fetchOnce(limit: Int = 20): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            var inserted = 0
            val currentLanguage = normalizeLanguageTag(settingsStore.contentLanguageFlow.first())
            sourceRepository.enabledFetchableSources().forEach { source ->
                runCatching {
                    val rawCfg = source.configJson ?: return@runCatching
                    val cfg = parseOnlineSourceConfig(JSONObject(rawCfg))
                    val supported = source.supportedLanguages.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    val requestLang = pickRequestLanguage(currentLanguage, supported) ?: return@runCatching
                    val url = cfg.request.url
                        .replace("{{lang}}", requestLang)
                        .replace("{{limit}}", limit.toString())
                        .replace("{{cursor}}", "")

                    val reqBuilder = Request.Builder().url(url)
                    cfg.request.headers.forEach { (k, v) -> reqBuilder.addHeader(k, v) }
                    val request = when (cfg.request.method.uppercase()) {
                        "GET" -> reqBuilder.get().build()
                        else -> reqBuilder.get().build()
                    }
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) return@runCatching
                        val payload = response.body?.string().orEmpty()
                        if (payload.isBlank()) return@runCatching
                        val root = parsePayloadToRoot(payload)
                        val items = JsonPath.getItems(root, cfg.response.itemsPath)
                        sourceRepository.addRawItems(source, items, requestLang)
                        inserted += items.size
                    }
                }
            }
            settingsStore.setLastUpdateAt(System.currentTimeMillis())
            inserted
        }
    }

    private fun parseOnlineSourceConfig(json: JSONObject): OnlineSourceConfig {
        val reqObj = json.getJSONObject("request")
        val responseObj = json.getJSONObject("response")
        val mappingObj = json.getJSONObject("mapping")
        return OnlineSourceConfig(
            request = RequestSpec(
                method = reqObj.optString("method", "GET"),
                url = reqObj.getString("url"),
                headers = reqObj.optJSONObject("headers").toMap(),
                query = reqObj.optJSONObject("query").toMap(),
                body = reqObj.optNullableString("body")
            ),
            response = ResponseSpec(
                itemsPath = responseObj.getString("itemsPath"),
                cursorPath = responseObj.optNullableString("cursorPath")
            ),
            mapping = MappingSpec(
                content = mappingObj.getString("content"),
                title = mappingObj.optNullableString("title"),
                ageHint = mappingObj.optNullableString("ageHint"),
                language = mappingObj.optNullableString("language"),
                sourceUrl = mappingObj.optNullableString("sourceUrl")
            )
        )
    }
}

private fun parsePayloadToRoot(payload: String): JSONObject {
    val trimmed = payload.trim()
    return if (trimmed.startsWith("[")) {
        JSONObject(mapOf("items" to JSONArray(trimmed)))
    } else {
        JSONObject(trimmed)
    }
}

private fun JSONObject?.toMap(): Map<String, String> {
    if (this == null) return emptyMap()
    val result = mutableMapOf<String, String>()
    keys().forEach { key -> result[key] = optString(key) }
    return result
}

private fun JSONObject.optNullableString(key: String): String? {
    if (!has(key)) return null
    val value = optString(key)
    return value.takeIf { it.isNotBlank() }
}

private fun pickRequestLanguage(currentLanguage: String, supportedLanguages: List<String>): String? {
    if (supportedLanguages.isEmpty()) return currentLanguage
    val target = normalizeLanguageTag(currentLanguage)
    val normalizedPairs = supportedLanguages.associateWith { normalizeLanguageTag(it) }
    supportedLanguages.firstOrNull { normalizedPairs[it] == target }?.let { return it }
    val targetPrefix = target.substringBefore('-')
    supportedLanguages.firstOrNull { normalizedPairs[it]?.substringBefore('-') == targetPrefix }?.let { return it }
    return null
}

private fun normalizeLanguageTag(language: String): String {
    val normalized = language.trim().replace('_', '-').lowercase()
    return when {
        normalized.startsWith("zh-hant") || normalized.startsWith("zh-tw") || normalized.startsWith("zh-hk") -> "zh-Hant"
        normalized.startsWith("zh-hans") || normalized.startsWith("zh-cn") || normalized == "zh" -> "zh-Hans"
        normalized.startsWith("en") -> "en"
        normalized.isBlank() -> "zh-Hans"
        else -> language.trim()
    }
}
