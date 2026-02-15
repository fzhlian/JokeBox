package fzhlian.JokeBox.app.data.repo

import fzhlian.JokeBox.app.data.db.FavoriteDao
import fzhlian.JokeBox.app.data.db.JokeDao
import fzhlian.JokeBox.app.data.db.PlaybackStateDao
import fzhlian.JokeBox.app.data.db.PlaybackStateEntity
import fzhlian.JokeBox.app.data.db.PlayedDao
import fzhlian.JokeBox.app.data.db.PlayedEntity
import fzhlian.JokeBox.app.data.model.AgeGroup
import fzhlian.JokeBox.app.data.model.JokeUiItem
import kotlinx.coroutines.flow.Flow
import java.util.ArrayDeque

class PlaybackRepository(
    private val jokeDao: JokeDao,
    private val playedDao: PlayedDao,
    private val favoriteDao: FavoriteDao,
    private val playbackStateDao: PlaybackStateDao
) {
    private val history = ArrayDeque<String>()

    fun observeUnplayedCount(): Flow<Int> = jokeDao.observeUnplayedCount()

    fun observeUnplayedCount(ageGroup: AgeGroup): Flow<Int> = jokeDao.observeUnplayedCountByAge(ageGroup.value)

    suspend fun next(ageGroup: AgeGroup, language: String): JokeUiItem? {
        val normalizedLanguage = normalizeLanguageTag(language)
        val exactCandidates = linkedSetOf(language, normalizedLanguage, normalizedLanguage.substringBefore('-'))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val joke = exactCandidates.firstNotNullOfOrNull { lang ->
            jokeDao.pickNextUnplayed(ageGroup.value, lang)
        } ?: run {
            val prefix = normalizedLanguage.substringBefore('-').ifBlank { normalizedLanguage }
            jokeDao.pickNextUnplayedByLanguagePrefix(ageGroup.value, "$prefix%")
        } ?: jokeDao.pickNextUnplayedByAge(ageGroup.value)
        ?: return null

        playedDao.upsert(PlayedEntity(joke.id, System.currentTimeMillis()))
        val state = playbackStateDao.get() ?: PlaybackStateEntity(
            key = "main",
            lastJokeId = null,
            playedCount = 0,
            updatedAt = System.currentTimeMillis()
        )
        playbackStateDao.upsert(
            state.copy(
                lastJokeId = joke.id,
                playedCount = state.playedCount + 1,
                updatedAt = System.currentTimeMillis()
            )
        )
        history.addLast(joke.id)
        if (history.size > 100) history.removeFirst()
        return joke.toUiItem(favoriteDao.isFavorite(joke.id))
    }

    suspend fun prev(): JokeUiItem? {
        if (history.size <= 1) return null
        history.removeLast()
        val previousId = history.lastOrNull() ?: return null
        val joke = jokeDao.getById(previousId) ?: return null
        return joke.toUiItem(favoriteDao.isFavorite(joke.id))
    }

    suspend fun toggleFavorite(jokeId: String): Boolean {
        val favorite = favoriteDao.isFavorite(jokeId)
        return if (favorite) {
            favoriteDao.remove(jokeId)
            false
        } else {
            favoriteDao.upsert(fzhlian.JokeBox.app.data.db.FavoriteEntity(jokeId, System.currentTimeMillis()))
            true
        }
    }

    suspend fun resetPlayed() {
        playedDao.clearAll()
        history.clear()
        val state = playbackStateDao.get() ?: PlaybackStateEntity(
            key = "main",
            lastJokeId = null,
            playedCount = 0,
            updatedAt = System.currentTimeMillis()
        )
        playbackStateDao.upsert(state.copy(lastJokeId = null, playedCount = 0, updatedAt = System.currentTimeMillis()))
    }

    suspend fun clearFavorites() {
        favoriteDao.clearAll()
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

private fun fzhlian.JokeBox.app.data.db.JokeEntity.toUiItem(favorite: Boolean) = JokeUiItem(
    id = id,
    content = content,
    language = language,
    favorite = favorite
)

