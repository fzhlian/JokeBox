package com.jokebox.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.jokebox.app.data.model.RawStatus
import com.jokebox.app.data.model.SourceType
import kotlinx.coroutines.flow.Flow

@Dao
interface SourceConfigDao {
    @Query("SELECT * FROM source_config WHERE enabled = 1")
    suspend fun listEnabled(): List<SourceConfigEntity>

    @Query("SELECT * FROM source_config")
    fun observeAll(): Flow<List<SourceConfigEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<SourceConfigEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: SourceConfigEntity)

    @Query("SELECT * FROM source_config WHERE sourceId = :sourceId LIMIT 1")
    suspend fun getById(sourceId: String): SourceConfigEntity?

    @Query("UPDATE source_config SET enabled = :enabled, updatedAt = :updatedAt WHERE sourceId = :sourceId")
    suspend fun updateEnabled(sourceId: String, enabled: Boolean, updatedAt: Long)

    @Query("DELETE FROM source_config WHERE sourceId = :sourceId")
    suspend fun deleteById(sourceId: String)

    @Query("DELETE FROM source_config WHERE type IN (:types)")
    suspend fun deleteByTypes(types: List<SourceType>)
}

@Dao
interface RawQueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<RawQueueEntity>)

    @Query("SELECT * FROM raw_queue WHERE status = :status ORDER BY fetchedAt ASC LIMIT :limit")
    suspend fun listByStatus(status: RawStatus, limit: Int): List<RawQueueEntity>

    @Query("SELECT COUNT(*) FROM raw_queue WHERE status = :status")
    fun observeCount(status: RawStatus): Flow<Int>

    @Query(
        "UPDATE raw_queue SET status = :status, failCount = :failCount, lastError = :lastError, dropReason = :dropReason WHERE rawId = :rawId"
    )
    suspend fun updateStatus(
        rawId: Long,
        status: RawStatus,
        failCount: Int,
        lastError: String?,
        dropReason: String?
    )

    @Query("DELETE FROM raw_queue WHERE ownerSourceType IN (:types)")
    suspend fun deleteBySourceTypes(types: List<SourceType>)
}

@Dao
interface JokeDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: JokeEntity): Long

    @Query("SELECT EXISTS(SELECT 1 FROM joke WHERE id = :id)")
    suspend fun exists(id: String): Boolean

    @Query(
        "SELECT * FROM joke WHERE ageGroup = :ageGroup AND language = :language AND substr(simHash, 1, :prefixLen) = :bucket"
    )
    suspend fun listByBucket(ageGroup: Int, language: String, prefixLen: Int, bucket: String): List<JokeEntity>

    @Query(
        "SELECT j.* FROM joke j LEFT JOIN played p ON j.id = p.jokeId WHERE p.jokeId IS NULL AND j.ageGroup = :ageGroup AND j.language = :language ORDER BY j.createdAt ASC LIMIT 1"
    )
    suspend fun pickNextUnplayed(ageGroup: Int, language: String): JokeEntity?

    @Query("SELECT * FROM joke WHERE id = :id")
    suspend fun getById(id: String): JokeEntity?

    @Query("SELECT COUNT(*) FROM joke j LEFT JOIN played p ON j.id = p.jokeId WHERE p.jokeId IS NULL")
    fun observeUnplayedCount(): Flow<Int>

    @Query("DELETE FROM joke WHERE sourceType IN (:types)")
    suspend fun deleteBySourceTypes(types: List<SourceType>)
}

@Dao
interface PlayedDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: PlayedEntity)

    @Query("DELETE FROM played")
    suspend fun clearAll()
}

@Dao
interface FavoriteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: FavoriteEntity)

    @Query("DELETE FROM favorite WHERE jokeId = :jokeId")
    suspend fun remove(jokeId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite WHERE jokeId = :jokeId)")
    suspend fun isFavorite(jokeId: String): Boolean
}

@Dao
interface PlaybackStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: PlaybackStateEntity)

    @Query("SELECT * FROM playback_state WHERE `key` = :key")
    suspend fun get(key: String = "main"): PlaybackStateEntity?
}

@Dao
interface CleanupDao {
    @Transaction
    @Query("SELECT 1")
    suspend fun touch()
}
