package fzhlian.JokeBox.app

import android.content.Context
import fzhlian.JokeBox.app.data.db.AppDatabase
import fzhlian.JokeBox.app.data.repo.PlaybackRepository
import fzhlian.JokeBox.app.data.repo.SettingsStore
import fzhlian.JokeBox.app.data.repo.SourceRepository
import fzhlian.JokeBox.app.domain.pipeline.Fetcher
import fzhlian.JokeBox.app.domain.pipeline.Importer
import fzhlian.JokeBox.app.domain.pipeline.Processor
import fzhlian.JokeBox.app.domain.tts.AndroidTtsEngine

class AppContainer(context: Context) {
    private val db = AppDatabase.create(context)

    val settingsStore = SettingsStore(context)
    val sourceRepository = SourceRepository(context, db.sourceConfigDao(), db.rawQueueDao(), db.jokeDao())
    val playbackRepository = PlaybackRepository(
        jokeDao = db.jokeDao(),
        playedDao = db.playedDao(),
        favoriteDao = db.favoriteDao(),
        playbackStateDao = db.playbackStateDao()
    )
    val fetcher = Fetcher(sourceRepository, settingsStore)
    val processor = Processor(db.rawQueueDao(), db.jokeDao(), db.sourceConfigDao())
    val importer = Importer(db.rawQueueDao())
    val ttsEngine = AndroidTtsEngine(context)

    val rawQueueDao = db.rawQueueDao()
}

