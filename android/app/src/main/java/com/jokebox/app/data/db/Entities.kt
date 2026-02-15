package fzhlian.jokebox.app.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import fzhlian.jokebox.app.data.model.RawStatus
import fzhlian.jokebox.app.data.model.SourceType

@Entity(
    tableName = "source_config",
    indices = [Index("type"), Index("enabled")]
)
data class SourceConfigEntity(
    @PrimaryKey val sourceId: String,
    val type: SourceType,
    val name: String,
    val enabled: Boolean,
    val supportedLanguages: String,
    val configJson: String?,
    val licenseNote: String?,
    val toSNote: String?,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "raw_queue",
    indices = [Index("status"), Index("ownerSourceId")]
)
data class RawQueueEntity(
    @PrimaryKey(autoGenerate = true) val rawId: Long = 0,
    val ownerSourceId: String,
    val ownerSourceType: SourceType,
    val language: String?,
    val ageGroupHint: Int?,
    val payloadJson: String,
    val fetchedAt: Long,
    val status: RawStatus,
    val failCount: Int = 0,
    val lastError: String? = null,
    val dropReason: String? = null
)

@Entity(
    tableName = "joke",
    indices = [
        Index("ageGroup"),
        Index("language"),
        Index("simHash"),
        Index(value = ["ageGroup", "language", "simHash"])
    ]
)
data class JokeEntity(
    @PrimaryKey val id: String,
    val ageGroup: Int,
    val language: String,
    val content: String,
    val contentNorm: String,
    val simHash: String,
    val sourceId: String,
    val sourceType: SourceType,
    val sourceUrl: String?,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "played")
data class PlayedEntity(
    @PrimaryKey val jokeId: String,
    val playedAt: Long
)

@Entity(tableName = "favorite")
data class FavoriteEntity(
    @PrimaryKey val jokeId: String,
    val starredAt: Long
)

@Entity(tableName = "playback_state")
data class PlaybackStateEntity(
    @PrimaryKey val key: String = "main",
    val lastJokeId: String?,
    val playedCount: Long,
    val updatedAt: Long
)


