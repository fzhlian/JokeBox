package fzhlian.JokeBox.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        SourceConfigEntity::class,
        RawQueueEntity::class,
        JokeEntity::class,
        PlayedEntity::class,
        FavoriteEntity::class,
        PlaybackStateEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sourceConfigDao(): SourceConfigDao
    abstract fun rawQueueDao(): RawQueueDao
    abstract fun jokeDao(): JokeDao
    abstract fun playedDao(): PlayedDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun playbackStateDao(): PlaybackStateDao

    companion object {
        fun create(context: Context): AppDatabase = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "jokebox.db"
        ).fallbackToDestructiveMigration(dropAllTables = true).build()
    }
}

