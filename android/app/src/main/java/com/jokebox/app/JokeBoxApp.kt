package fzhlian.JokeBox.app

import android.app.Application
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import fzhlian.JokeBox.app.data.model.AgeGroup
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class JokeBoxApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        initialize()
    }

    private fun initialize() {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            container.settingsStore.setDefaultsIfMissing()
            container.sourceRepository.ensureBuiltinSourcesLoaded()
        }
        val work = PeriodicWorkRequestBuilder<SyncWorker>(12, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(androidx.work.NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "jokebox-sync",
            ExistingPeriodicWorkPolicy.UPDATE,
            work
        )
    }
}

class SyncWorker(appContext: android.content.Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val app = applicationContext as JokeBoxApp
        val age = app.container.settingsStore.getAgeGroup() ?: AgeGroup.ADULT
        val lang = app.container.settingsStore.contentLanguageFlow.first()
        val fetch = app.container.fetcher.fetchOnce()
        if (fetch.isFailure) return Result.retry()
        val process = app.container.processor.processBatch(
            targetAgeGroup = age,
            defaultLanguage = lang,
            nearThreshold = app.container.settingsStore.getNearDedupThreshold()
        )
        return if (process.isSuccess) Result.success() else Result.retry()
    }
}

