package com.jokebox.app.domain.pipeline

import android.util.Log
import com.jokebox.app.data.model.MappingSpec
import com.jokebox.app.data.model.OnlineSourceConfig
import com.jokebox.app.data.model.RequestSpec
import com.jokebox.app.data.model.ResponseSpec
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
            val sources = sourceRepository.enabledFetchableSources()
            Log.i("JokeBoxFetcher", "fetch start: lang=$currentLanguage limit=$limit sources=${sources.size}")
            sources.forEach { source ->
                runCatching {
                    val rawCfg = source.configJson ?: return@runCatching
                    val cfg = parseOnlineSourceConfig(JSONObject(rawCfg))
                    val supported = parseSupportedLanguages(source.supportedLanguages)
                    val requestLang = pickRequestLanguage(currentLanguage, supported)
                    if (requestLang == null) {
                        Log.i("JokeBoxFetcher", "skip ${source.sourceId}: no language match in $supported")
                        return@runCatching
                    }

                    val url = cfg.request.url
                        .replace("{{lang}}", requestLang)
                        .replace("{{limit}}", limit.toString())
                        .replace("{{cursor}}", "")

                    if (url.startsWith("local://cn-jokes/")) {
                        val category = url.removePrefix("local://cn-jokes/").substringBefore('?')
                        val localItems = buildLocalCnJokes(category, limit)
                        sourceRepository.addRawItems(source, localItems, requestLang)
                        inserted += localItems.size
                        Log.i("JokeBoxFetcher", "local source ${source.sourceId} inserted=${localItems.size} lang=$requestLang")
                        return@runCatching
                    }

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
                        Log.i("JokeBoxFetcher", "remote source ${source.sourceId} inserted=${items.size} lang=$requestLang")
                    }
                }.onFailure {
                    Log.w("JokeBoxFetcher", "source failed: ${source.sourceId} err=${it.message}")
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
    if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
        return JSONObject(mapOf("items" to JSONArray(listOf(JSONObject(mapOf("content" to trimmed))))))
    }
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

private fun parseSupportedLanguages(raw: String): List<String> {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return emptyList()

    if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
        return runCatching {
            val arr = JSONArray(trimmed)
            buildList {
                for (i in 0 until arr.length()) {
                    val value = arr.optString(i).trim().trim('"')
                    if (value.isNotBlank()) add(value)
                }
            }
        }.getOrElse { emptyList() }
    }

    return trimmed.split(",")
        .map { it.trim().trim('"').trim('[', ']') }
        .filter { it.isNotBlank() }
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

private fun buildLocalCnJokes(category: String, limit: Int): List<JSONObject> {
    val seeds = when (category.lowercase()) {
        "daily" -> listOf(
            "我从毕业到现在有两个人格，一个是回忆，一个是失忆。",
            "别人熬夜是追剧，我熬夜是等灵感，结果等来的是黑眼圈。",
            "今天去买菜，老板夸我会过日子，因为我只敢问价不敢买。",
            "我说要早睡，手机说再刷五分钟，我们都很坚持。",
            "我每次减肥都很认真，认真地从明天开始。"
        )

        "tech" -> listOf(
            "程序员去相亲，对方问会做饭吗？他说：会，煮异常最拿手。",
            "我问 AI 你会讲笑话吗？它说会，然后给我输出了一个 bug。",
            "同事说代码要优雅，我把 if 写成了诗，他让我重构。",
            "产品说这个需求很简单，我的 CPU 当场进入省电模式。",
            "老板问进度如何，我说已经 80%，剩下 80% 明天完成。"
        )

        "campus" -> listOf(
            "老师问我为什么迟到，我说在路上思考人生，结果人生堵车了。",
            "室友说要早起背单词，闹钟响了三次，他把英语关机了。",
            "考试前我和同学互相鼓励，考试后我们互相安慰。",
            "食堂阿姨手不抖的时候，我怀疑今天是不是放假。",
            "图书馆里最努力的人，是一直在找座位的我。"
        )

        "chumor" -> listOf(
            "今天我去图书馆学习，结果被 Wi-Fi 成功说服，转学到了视频区。",
            "导师说论文要有创新，我把标题从“研究”改成“再研究一次”。",
            "实验做了三天终于有结果：我的咖啡消耗量显著增加。",
            "同学问我为什么天天写代码，我说这是人类和 bug 的长期对话。",
            "周会汇报时我说进度稳定，意思是问题和昨天一样稳定。"
        )

        else -> listOf(
            "测试提了 100 个问题，我说这证明系统覆盖率很高。",
            "我把注释删了，代码跑得更快了，因为没人敢改了。",
            "数据库说我很稳定，只是偶尔在周五晚上情绪化。",
            "今天修了一个线上 bug，奖励是再给我一个线上 bug。",
            "代码评审时我写了 todo，领导说这就是长期规划。"
        )
    }

    return List(limit.coerceAtMost(50)) { idx ->
        JSONObject(mapOf("content" to seeds[idx % seeds.size]))
    }
}
