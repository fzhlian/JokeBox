package fzhlian.JokeBox.app.data.repo

import android.content.Context
import fzhlian.JokeBox.app.data.db.RawQueueDao
import fzhlian.JokeBox.app.data.db.RawQueueEntity
import fzhlian.JokeBox.app.data.db.SourceConfigDao
import fzhlian.JokeBox.app.data.db.SourceConfigEntity
import fzhlian.JokeBox.app.data.db.JokeDao
import fzhlian.JokeBox.app.data.model.RawStatus
import fzhlian.JokeBox.app.data.model.SourceType
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

class SourceRepository(
    private val context: Context,
    private val sourceDao: SourceConfigDao,
    private val rawDao: RawQueueDao,
    private val jokeDao: JokeDao,
    private val httpClient: OkHttpClient = OkHttpClient()
) {
    fun observeSources(): Flow<List<SourceConfigEntity>> = sourceDao.observeAll()

    suspend fun ensureBuiltinSourcesLoaded() {
        val json = context.assets.open("sources.json").bufferedReader().readText().trimStart('\uFEFF')
        val arr = JSONArray(json)
        val now = System.currentTimeMillis()
        val entities = buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                add(
                    SourceConfigEntity(
                        sourceId = obj.getString("sourceId"),
                        type = SourceType.BUILTIN,
                        name = obj.getString("name"),
                        enabled = obj.optBoolean("enabled", true),
                        supportedLanguages = obj.optJSONArray("supportedLanguages").toCsvLanguages(),
                        configJson = when (val cfg = obj.opt("configJson")) {
                            is JSONObject -> cfg.toString()
                            is String -> cfg.takeIf { it.isNotBlank() }
                            else -> null
                        },
                        licenseNote = obj.optString("licenseNote"),
                        toSNote = obj.optString("toSNote"),
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }
        }
        sourceDao.deleteByTypes(listOf(SourceType.BUILTIN))
        sourceDao.upsertAll(entities)
    }

    suspend fun enabledFetchableSources(): List<SourceConfigEntity> = sourceDao.listEnabled().filter {
        it.type == SourceType.BUILTIN || it.type == SourceType.USER_ONLINE
    }

    suspend fun addRawItems(owner: SourceConfigEntity, payloads: List<JSONObject>, language: String?) {
        val now = System.currentTimeMillis()
        val raws = payloads.map {
            RawQueueEntity(
                ownerSourceId = owner.sourceId,
                ownerSourceType = owner.type,
                language = language,
                ageGroupHint = null,
                payloadJson = it.toString(),
                fetchedAt = now,
                status = RawStatus.PENDING
            )
        }
        rawDao.insertAll(raws)
    }

    suspend fun clearUserSourcesAndData() {
        val userTypes = listOf(SourceType.USER_ONLINE, SourceType.USER_OFFLINE)
        sourceDao.deleteByTypes(userTypes)
        rawDao.deleteBySourceTypes(userTypes)
        jokeDao.deleteBySourceTypes(userTypes)
    }

    suspend fun clearAllFetchedData() {
        rawDao.deleteAll()
        jokeDao.deleteAll()
    }

    suspend fun setSourceEnabled(sourceId: String, enabled: Boolean) {
        sourceDao.updateEnabled(sourceId, enabled, System.currentTimeMillis())
    }

    suspend fun addUserOnlineSource(
        name: String,
        url: String,
        itemsPath: String,
        contentPath: String,
        languagePath: String?,
        sourceUrlPath: String?,
        licenseNote: String?
    ) {
        val now = System.currentTimeMillis()
        val config = buildConfigJson(
            url = url,
            itemsPath = itemsPath,
            contentPath = contentPath,
            languagePath = languagePath,
            sourceUrlPath = sourceUrlPath
        )

        sourceDao.upsert(
            SourceConfigEntity(
                sourceId = "user-online-${UUID.randomUUID()}",
                type = SourceType.USER_ONLINE,
                name = name,
                enabled = true,
                supportedLanguages = "",
                configJson = config,
                licenseNote = licenseNote,
                toSNote = "User configured source. User is responsible for authorization and ToS compliance.",
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun updateUserOnlineSource(
        sourceId: String,
        name: String,
        url: String,
        itemsPath: String,
        contentPath: String,
        languagePath: String?,
        sourceUrlPath: String?,
        licenseNote: String?
    ) {
        val existing = sourceDao.getById(sourceId) ?: return
        val config = buildConfigJson(
            url = url,
            itemsPath = itemsPath,
            contentPath = contentPath,
            languagePath = languagePath,
            sourceUrlPath = sourceUrlPath
        )
        sourceDao.upsert(
            existing.copy(
                name = name,
                configJson = config,
                licenseNote = licenseNote,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteSourceById(sourceId: String) {
        sourceDao.deleteById(sourceId)
    }

    suspend fun testUserOnlineSource(
        url: String,
        itemsPath: String,
        contentPath: String
    ): Result<Int> = runCatching {
        val request = Request.Builder()
            .url(url)
            .addHeader("Accept", "application/json")
            .addHeader("User-Agent", "JokeBox/0.1 (source-tester)")
            .get()
            .build()
        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            error("HTTP ${response.code}")
        }
        val text = response.body?.string().orEmpty()
        if (text.isBlank()) error("empty response")
        val root = JSONObject(text)
        val items = getItemsByPath(root, itemsPath)
        if (items.isEmpty()) error("itemsPath not found or empty")
        val contentValue = getStringByPath(items.first(), contentPath)
        if (contentValue.isNullOrBlank()) error("contentPath invalid")
        items.size
    }

    private fun buildConfigJson(
        url: String,
        itemsPath: String,
        contentPath: String,
        languagePath: String?,
        sourceUrlPath: String?
    ): String {
        return JSONObject(
            mapOf(
                "request" to JSONObject(
                    mapOf(
                        "method" to "GET",
                        "url" to url,
                        "headers" to JSONObject(
                            mapOf(
                                "Accept" to "application/json",
                                "User-Agent" to "JokeBox/0.1 (fetcher)"
                            )
                        )
                    )
                ),
                "response" to JSONObject(
                    mapOf(
                        "itemsPath" to itemsPath,
                        "cursorPath" to JSONObject.NULL
                    )
                ),
                "mapping" to JSONObject(
                    mapOf(
                        "content" to contentPath,
                        "title" to JSONObject.NULL,
                        "ageHint" to JSONObject.NULL,
                        "language" to (languagePath ?: JSONObject.NULL),
                        "sourceUrl" to (sourceUrlPath ?: JSONObject.NULL)
                    )
                )
            )
        ).toString()
    }
}

private fun JSONArray?.toCsvLanguages(): String {
    if (this == null) return ""
    val list = mutableListOf<String>()
    for (i in 0 until length()) {
        val value = optString(i).trim().trim('"')
        if (value.isNotBlank()) list += value
    }
    return list.joinToString(",")
}

private fun getItemsByPath(root: JSONObject, path: String): List<JSONObject> {
    val tokens = path.split(".")
    var cursor: Any = root
    for (token in tokens) {
        cursor = when (cursor) {
            is JSONObject -> cursor.opt(token) ?: return emptyList()
            is JSONArray -> {
                val idx = token.toIntOrNull() ?: return emptyList()
                if (idx in 0 until cursor.length()) cursor.get(idx) else return emptyList()
            }
            else -> return emptyList()
        }
    }
    return when (cursor) {
        is JSONArray -> buildList {
            for (i in 0 until cursor.length()) {
                val value = cursor.opt(i)
                if (value is JSONObject) add(value)
            }
        }
        is JSONObject -> listOf(cursor)
        else -> emptyList()
    }
}

private fun getStringByPath(root: JSONObject, path: String): String? {
    val tokens = path.split(".")
    var cursor: Any = root
    for (token in tokens) {
        cursor = when (cursor) {
            is JSONObject -> cursor.opt(token) ?: return null
            is JSONArray -> {
                val idx = token.toIntOrNull() ?: return null
                if (idx in 0 until cursor.length()) cursor.get(idx) else return null
            }
            else -> return null
        }
    }
    return cursor.toString()
}

