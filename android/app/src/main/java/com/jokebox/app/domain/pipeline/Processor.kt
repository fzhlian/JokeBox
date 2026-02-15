package fzhlian.JokeBox.app.domain.pipeline

import fzhlian.JokeBox.app.data.db.JokeDao
import fzhlian.JokeBox.app.data.db.JokeEntity
import fzhlian.JokeBox.app.data.db.RawQueueDao
import fzhlian.JokeBox.app.data.db.RawQueueEntity
import fzhlian.JokeBox.app.data.db.SourceConfigDao
import fzhlian.JokeBox.app.data.db.SourceConfigEntity
import fzhlian.JokeBox.app.data.model.AgeGroup
import fzhlian.JokeBox.app.data.model.RawStatus
import fzhlian.JokeBox.app.domain.policy.ContentPolicy
import fzhlian.JokeBox.app.domain.policy.Hashing
import fzhlian.JokeBox.app.domain.policy.SimHash64
import fzhlian.JokeBox.app.domain.policy.TextNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class Processor(
    private val rawDao: RawQueueDao,
    private val jokeDao: JokeDao,
    private val sourceDao: SourceConfigDao
) {
    suspend fun processBatch(
        targetAgeGroup: AgeGroup,
        defaultLanguage: String,
        nearThreshold: Int,
        batchSize: Int = 50
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val pending = rawDao.listByStatus(RawStatus.PENDING, batchSize)
            if (pending.isEmpty()) return@runCatching 0
            var accepted = 0
            val sourceMap = sourceDao.listEnabled().associateBy { it.sourceId }
            pending.forEach { raw ->
                rawDao.updateStatus(raw.rawId, RawStatus.PROCESSING, raw.failCount, null, null)
                val result = runCatching {
                    processRaw(raw, sourceMap[raw.ownerSourceId], targetAgeGroup, defaultLanguage, nearThreshold)
                }
                if (result.isFailure) {
                    val failCount = raw.failCount + 1
                    val status = if (failCount >= 3) RawStatus.FAILED else RawStatus.PENDING
                    rawDao.updateStatus(raw.rawId, status, failCount, result.exceptionOrNull()?.message, null)
                } else {
                    val state = result.getOrThrow()
                    when (state) {
                        ProcessDecision.ACCEPTED -> {
                            accepted++
                            rawDao.updateStatus(raw.rawId, RawStatus.DONE, raw.failCount, null, null)
                        }
                        ProcessDecision.DROPPED_POLICY ->
                            rawDao.updateStatus(raw.rawId, RawStatus.DROPPED, raw.failCount, null, "policy")

                        ProcessDecision.DROPPED_DUP ->
                            rawDao.updateStatus(raw.rawId, RawStatus.DROPPED, raw.failCount, null, "duplicate")
                    }
                }
            }
            accepted
        }
    }

    private suspend fun processRaw(
        raw: RawQueueEntity,
        source: SourceConfigEntity?,
        targetAgeGroup: AgeGroup,
        defaultLanguage: String,
        nearThreshold: Int
    ): ProcessDecision {
        val root = JSONObject(raw.payloadJson)
        val mapping = source?.configJson?.let { JSONObject(it).optJSONObject("mapping") }
        val contentPath = mapping?.optString("content")?.takeIf { it.isNotBlank() } ?: "content"
        val languagePath = mapping?.optString("language")?.takeIf { it.isNotBlank() }
        val sourceUrlPath = mapping?.optString("sourceUrl")?.takeIf { it.isNotBlank() }
        val content = JsonPath.getString(root, contentPath)?.trim().orEmpty()
        if (content.isBlank()) return ProcessDecision.DROPPED_POLICY

        val contentNorm = TextNormalizer.normalize(content)
        val age = targetAgeGroup.value
        if (!ContentPolicy.allow(contentNorm, age)) return ProcessDecision.DROPPED_POLICY

        val hashId = Hashing.sha256Hex(contentNorm, 32)
        if (jokeDao.exists(hashId)) return ProcessDecision.DROPPED_DUP

        val language = normalizeLanguageTag(JsonPath.getString(root, languagePath) ?: raw.language ?: defaultLanguage)
        val simHash = SimHash64.compute(contentNorm)
        val bucket = SimHash64.bucket(simHash)
        val nearCandidates = jokeDao.listByBucket(age, language, bucket.length, bucket)
        val nearDup = nearCandidates.any {
            SimHash64.hammingDistance(it.simHash, simHash) <= nearThreshold
        }
        if (nearDup) return ProcessDecision.DROPPED_DUP

        val now = System.currentTimeMillis()
        jokeDao.insert(
            JokeEntity(
                id = hashId,
                ageGroup = age,
                language = language,
                content = content,
                contentNorm = contentNorm,
                simHash = simHash,
                sourceId = raw.ownerSourceId,
                sourceType = raw.ownerSourceType,
                sourceUrl = JsonPath.getString(root, sourceUrlPath),
                createdAt = now,
                updatedAt = now
            )
        )
        return ProcessDecision.ACCEPTED
    }
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

private enum class ProcessDecision {
    ACCEPTED,
    DROPPED_POLICY,
    DROPPED_DUP
}

