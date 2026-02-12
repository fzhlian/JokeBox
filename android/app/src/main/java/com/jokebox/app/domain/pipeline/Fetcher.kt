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
import org.json.JSONObject

class Fetcher(
    private val sourceRepository: SourceRepository,
    private val settingsStore: SettingsStore,
    private val client: OkHttpClient = OkHttpClient()
) {
    suspend fun fetchOnce(limit: Int = 20): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            var inserted = 0
            val currentLanguage = settingsStore.contentLanguageFlow.first()
            sourceRepository.enabledFetchableSources().forEach { source ->
                val rawCfg = source.configJson ?: return@forEach
                val cfg = parseOnlineSourceConfig(JSONObject(rawCfg))
                val supported = source.supportedLanguages.split(",").map { it.trim() }.filter { it.isNotBlank() }
                val requestLang = if (supported.isEmpty() || supported.contains(currentLanguage)) {
                    currentLanguage
                } else {
                    supported.first()
                }
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
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) return@forEach
                val payload = response.body?.string().orEmpty()
                if (payload.isBlank()) return@forEach
                val root = JSONObject(payload)
                val items = JsonPath.getItems(root, cfg.response.itemsPath)
                sourceRepository.addRawItems(source, items, requestLang)
                inserted += items.size
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
                body = reqObj.optString("body", null)
            ),
            response = ResponseSpec(
                itemsPath = responseObj.getString("itemsPath"),
                cursorPath = responseObj.optString("cursorPath", null)
            ),
            mapping = MappingSpec(
                content = mappingObj.getString("content"),
                title = mappingObj.optString("title", null),
                ageHint = mappingObj.optString("ageHint", null),
                language = mappingObj.optString("language", null),
                sourceUrl = mappingObj.optString("sourceUrl", null)
            )
        )
    }
}

private fun JSONObject?.toMap(): Map<String, String> {
    if (this == null) return emptyMap()
    val result = mutableMapOf<String, String>()
    keys().forEach { key -> result[key] = optString(key) }
    return result
}
