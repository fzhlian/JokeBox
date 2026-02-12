package com.jokebox.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jokebox.app.ui.JokeBoxViewModel
import com.jokebox.app.ui.JokeBoxViewModelFactory
import com.jokebox.app.ui.screen.MainScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as JokeBoxApp
        val factory = JokeBoxViewModelFactory(
            settingsStore = app.container.settingsStore,
            sourceRepository = app.container.sourceRepository,
            playbackRepository = app.container.playbackRepository,
            fetcher = app.container.fetcher,
            importer = app.container.importer,
            processor = app.container.processor,
            ttsEngine = app.container.ttsEngine,
            rawQueueDao = app.container.rawQueueDao
        )
        setContent {
            val vm: JokeBoxViewModel = viewModel(factory = factory)
            val state by vm.uiState.collectAsStateWithLifecycle()
            MaterialTheme {
                Surface {
                    MainScreen(
                        uiState = state,
                        onCompleteOnboarding = vm::completeOnboarding,
                        onPrev = vm::loadPrev,
                        onNext = vm::loadNext,
                        onFavorite = vm::toggleFavorite,
                        onUpdate = vm::runManualUpdate,
                        onResetPlayed = vm::resetPlayed,
                        onSpeak = vm::speakCurrent,
                        onStopSpeak = vm::stopSpeak,
                        onProcess = vm::runProcessOnly,
                        onSetSourceEnabled = vm::setSourceEnabled,
                        onDeleteSource = vm::deleteSource,
                        onAddUserOnlineSource = vm::addUserOnlineSource,
                        onUpdateUserOnlineSource = vm::updateUserOnlineSource,
                        onTestUserOnlineSource = vm::testUserOnlineSource,
                        onImportOfflineText = vm::importOfflineText,
                        onClearUserSourceData = vm::clearUserSourceData,
                        onSetUiLanguageMode = vm::setUiLanguageMode,
                        onSetUiLanguage = vm::setUiLanguage,
                        onSetContentLanguageMode = vm::setContentLanguageMode,
                        onSetContentLanguage = vm::setContentLanguage,
                        onSetAutoUpdateEnabled = vm::setAutoUpdateEnabled,
                        onSetAutoProcessEnabled = vm::setAutoProcessEnabled,
                        onSetTtsSpeed = vm::setTtsSpeed,
                        onSetTtsPitch = vm::setTtsPitch
                    )
                }
            }
        }
    }
}
