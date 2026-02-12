package com.jokebox.app.data.repo

import com.jokebox.app.data.db.FavoriteDao
import com.jokebox.app.data.db.JokeDao
import com.jokebox.app.data.db.PlaybackStateDao
import com.jokebox.app.data.db.PlaybackStateEntity
import com.jokebox.app.data.db.PlayedDao
import com.jokebox.app.data.db.PlayedEntity
import com.jokebox.app.data.model.AgeGroup
import com.jokebox.app.data.model.JokeUiItem
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

    suspend fun next(ageGroup: AgeGroup, language: String): JokeUiItem? {
        val joke = jokeDao.pickNextUnplayed(ageGroup.value, language) ?: return null
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
            favoriteDao.upsert(com.jokebox.app.data.db.FavoriteEntity(jokeId, System.currentTimeMillis()))
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
}

private fun com.jokebox.app.data.db.JokeEntity.toUiItem(favorite: Boolean) = JokeUiItem(
    id = id,
    content = content,
    language = language,
    favorite = favorite
)
