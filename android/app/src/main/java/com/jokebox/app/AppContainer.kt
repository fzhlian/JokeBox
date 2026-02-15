package fzhlian.jokebox.app

import android.content.Context
import fzhlian.jokebox.app.data.db.AppDatabase
import fzhlian.jokebox.app.data.repo.PlaybackRepository
import fzhlian.jokebox.app.data.repo.SettingsStore
import fzhlian.jokebox.app.data.repo.SourceRepository
import fzhlian.jokebox.app.domain.pipeline.Fetcher
import fzhlian.jokebox.app.domain.pipeline.Importer
import fzhlian.jokebox.app.domain.pipeline.Processor
import fzhlian.jokebox.app.domain.tts.AndroidTtsEngine

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


