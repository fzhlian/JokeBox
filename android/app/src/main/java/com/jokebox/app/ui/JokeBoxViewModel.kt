package fzhlian.JokeBox.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import fzhlian.JokeBox.app.data.db.RawQueueDao
import fzhlian.JokeBox.app.data.model.AgeGroup
import fzhlian.JokeBox.app.data.model.LanguageMode
import fzhlian.JokeBox.app.data.model.RawStatus
import fzhlian.JokeBox.app.data.repo.PlaybackRepository
import fzhlian.JokeBox.app.data.repo.SettingsStore
import fzhlian.JokeBox.app.data.repo.SourceRepository
import fzhlian.JokeBox.app.domain.pipeline.Fetcher
import fzhlian.JokeBox.app.domain.pipeline.Importer
import fzhlian.JokeBox.app.domain.pipeline.Processor
import fzhlian.JokeBox.app.domain.tts.TtsEngine
import fzhlian.JokeBox.app.ui.state.MainUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class JokeBoxViewModel(
    private val settingsStore: SettingsStore,
    private val sourceRepository: SourceRepository,
    private val playbackRepository: PlaybackRepository,
    private val fetcher: Fetcher,
    private val importer: Importer,
    private val processor: Processor,
    private val ttsEngine: TtsEngine,
    rawQueueDao: RawQueueDao
) : ViewModel() {
    private val localState = MutableStateFlow(MainUiState())

    private val baseSettingsFlow = combine(
        settingsStore.ageLockedFlow,
        settingsStore.selectedAgeGroupFlow,
        settingsStore.complianceAcceptedFlow
    ) { ageLocked, ageGroup, compliance ->
        Triple(ageLocked, ageGroup, compliance)
    }

    private val languageSettingsFlow = combine(
        settingsStore.uiLanguageModeFlow,
        settingsStore.uiLanguageFlow,
        settingsStore.contentLanguageModeFlow,
        settingsStore.contentLanguageSelectedFlow
    ) { uiMode, uiLang, contentMode, contentLang ->
        LanguageSettings(uiMode, uiLang, contentMode, contentLang)
    }

    private val featureSettingsFlow = combine(
        settingsStore.autoUpdateEnabledFlow,
        settingsStore.autoProcessEnabledFlow,
        settingsStore.ttsVoiceProfileIdFlow,
        settingsStore.ttsSpeedFlow,
        settingsStore.ttsPitchFlow
    ) { autoUpdate, autoProcess, ttsVoiceProfileId, ttsSpeed, ttsPitch ->
        FeatureSettings(autoUpdate, autoProcess, ttsVoiceProfileId, ttsSpeed, ttsPitch)
    }

    private val displaySettingsFlow = combine(languageSettingsFlow, featureSettingsFlow) { language, feature ->
        DisplaySettings(language, feature)
    }

    private val ageScopedUnplayedFlow = settingsStore.selectedAgeGroupFlow
        .flatMapLatest { ageGroup ->
            if (ageGroup == null) flowOf(0) else playbackRepository.observeUnplayedCount(ageGroup)
        }

    private val playbackStatsFlow = combine(
        ageScopedUnplayedFlow,
        rawQueueDao.observeCount(RawStatus.PENDING)
    ) { unplayed, pending ->
        PlaybackStats(unplayed, pending)
    }

    val uiState: StateFlow<MainUiState> = combine(
        localState,
        playbackStatsFlow,
        sourceRepository.observeSources(),
        baseSettingsFlow,
        displaySettingsFlow
    ) { local, stats, sources, base, display ->
        local.copy(
            initialized = true,
            unplayedCount = stats.unplayed,
            processingCount = stats.pending,
            sources = sources,
            ageLocked = base.first,
            selectedAgeGroup = base.second,
            complianceAccepted = base.third,
            uiLanguageMode = display.language.uiMode,
            uiLanguage = display.language.uiLanguage,
            contentLanguageMode = display.language.contentMode,
            contentLanguage = display.language.contentLanguage,
            autoUpdateEnabled = display.feature.autoUpdate,
            autoProcessEnabled = display.feature.autoProcess,
            ttsVoiceProfileId = display.feature.ttsVoiceProfileId,
            ttsSpeed = display.feature.ttsSpeed,
            ttsPitch = display.feature.ttsPitch
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MainUiState())

    fun completeOnboarding(ageGroup: AgeGroup) {
        viewModelScope.launch {
            settingsStore.lockAgeGroup(ageGroup)
            settingsStore.setComplianceAccepted(true)
            localState.value = localState.value.copy(message = "初始化完成")
        }
    }

    fun loadNext() {
        viewModelScope.launch {
            val ageGroup = settingsStore.getAgeGroup()
            if (ageGroup == null) {
                localState.value = localState.value.copy(message = "请先完成首次设置")
                return@launch
            }
            val language = settingsStore.contentLanguageFlow.first()
            val joke = playbackRepository.next(ageGroup, language)
            if (joke == null) {
                localState.value = localState.value.copy(message = "已播完，可更新或重置已播", currentJoke = null)
            } else {
                val voiceProfileId = settingsStore.ttsVoiceProfileIdFlow.first()
                localState.value = localState.value.copy(currentJoke = joke, message = null)
                ttsEngine.speak(
                    sanitizeForSpeech(joke.content),
                    uiState.value.ttsSpeed,
                    uiState.value.ttsPitch,
                    voiceProfileId
                )
            }
        }
    }

    fun loadPrev() {
        viewModelScope.launch {
            val joke = playbackRepository.prev()
            if (joke == null) {
                localState.value = localState.value.copy(message = "没有更早的历史")
            } else {
                localState.value = localState.value.copy(currentJoke = joke, message = null)
            }
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val current = localState.value.currentJoke ?: return@launch
            val nowFavorite = playbackRepository.toggleFavorite(current.id)
            localState.value = localState.value.copy(
                currentJoke = current.copy(favorite = nowFavorite),
                message = if (nowFavorite) "已收藏" else "已取消收藏"
            )
        }
    }

    fun speakCurrent() {
        viewModelScope.launch {
            val joke = uiState.value.currentJoke ?: return@launch
            ttsEngine.speak(
                sanitizeForSpeech(joke.content),
                uiState.value.ttsSpeed,
                uiState.value.ttsPitch,
                uiState.value.ttsVoiceProfileId
            )
            localState.value = localState.value.copy(message = "开始朗读")
        }
    }

    fun stopSpeak() {
        viewModelScope.launch {
            ttsEngine.stop()
            localState.value = localState.value.copy(message = "已停止朗读")
        }
    }

    fun runManualUpdate() {
        viewModelScope.launch {
            sourceRepository.ensureBuiltinSourcesLoaded()
            runFetchAndProcess(prefix = "更新完成")
        }
    }

    fun clearLibraryAndRefetchChinese() {
        viewModelScope.launch {
            settingsStore.setContentLanguageMode(LanguageMode.MANUAL)
            settingsStore.setContentLanguage("zh-Hans")
            sourceRepository.ensureBuiltinSourcesLoaded()
            sourceRepository.clearAllFetchedData()
            playbackRepository.clearFavorites()
            playbackRepository.resetPlayed()
            localState.value = localState.value.copy(currentJoke = null, message = "已清空笑话库，开始重抓中文数据")
            runFetchAndProcess(prefix = "中文重抓完成")
            loadNext()
        }
    }

    fun runProcessOnly() {
        viewModelScope.launch {
            processPending("处理完成")
        }
    }

    fun resetPlayed() {
        viewModelScope.launch {
            playbackRepository.resetPlayed()
            localState.value = localState.value.copy(message = "已重置已播记录")
        }
    }

    fun setSourceEnabled(sourceId: String, enabled: Boolean) {
        viewModelScope.launch {
            sourceRepository.setSourceEnabled(sourceId, enabled)
            localState.value = localState.value.copy(message = "来源状态已更新")
        }
    }

    fun deleteSource(sourceId: String) {
        viewModelScope.launch {
            sourceRepository.deleteSourceById(sourceId)
            localState.value = localState.value.copy(message = "来源已删除")
        }
    }

    fun addUserOnlineSource(
        name: String,
        url: String,
        itemsPath: String,
        contentPath: String,
        languagePath: String,
        sourceUrlPath: String,
        licenseNote: String
    ) {
        viewModelScope.launch {
            sourceRepository.addUserOnlineSource(
                name = name,
                url = url,
                itemsPath = itemsPath,
                contentPath = contentPath,
                languagePath = languagePath.ifBlank { null },
                sourceUrlPath = sourceUrlPath.ifBlank { null },
                licenseNote = licenseNote.ifBlank { null }
            )
            localState.value = localState.value.copy(message = "已新增用户在线来源")
        }
    }

    fun updateUserOnlineSource(
        sourceId: String,
        name: String,
        url: String,
        itemsPath: String,
        contentPath: String,
        languagePath: String,
        sourceUrlPath: String,
        licenseNote: String
    ) {
        viewModelScope.launch {
            sourceRepository.updateUserOnlineSource(
                sourceId = sourceId,
                name = name,
                url = url,
                itemsPath = itemsPath,
                contentPath = contentPath,
                languagePath = languagePath.ifBlank { null },
                sourceUrlPath = sourceUrlPath.ifBlank { null },
                licenseNote = licenseNote.ifBlank { null }
            )
            localState.value = localState.value.copy(message = "来源已更新")
        }
    }

    fun testUserOnlineSource(url: String, itemsPath: String, contentPath: String) {
        viewModelScope.launch {
            val result = sourceRepository.testUserOnlineSource(url, itemsPath, contentPath)
            localState.value = if (result.isSuccess) {
                localState.value.copy(message = "连接测试成功，解析到${result.getOrDefault(0)}条")
            } else {
                localState.value.copy(message = "连接测试失败：${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun importOfflineText(format: String, text: String) {
        viewModelScope.launch {
            if (text.isBlank()) {
                localState.value = localState.value.copy(message = "离线内容为空")
                return@launch
            }
            val language = settingsStore.contentLanguageFlow.first()
            importer.importText(
                sourceId = "user-offline",
                text = text,
                language = language,
                format = format
            )
            processPending("离线导入完成")
        }
    }

    fun clearUserSourceData() {
        viewModelScope.launch {
            sourceRepository.clearUserSourcesAndData()
            settingsStore.resetPlaybackAndUserDataPreferences()
            localState.value = localState.value.copy(message = "已清空用户来源数据")
        }
    }

    fun setUiLanguageMode(mode: LanguageMode) {
        viewModelScope.launch { settingsStore.setUiLanguageMode(mode) }
    }

    fun setUiLanguage(value: String) {
        viewModelScope.launch { settingsStore.setUiLanguage(value) }
    }

    fun setContentLanguageMode(mode: LanguageMode) {
        viewModelScope.launch { settingsStore.setContentLanguageMode(mode) }
    }

    fun setContentLanguage(value: String) {
        viewModelScope.launch { settingsStore.setContentLanguage(value) }
    }

    fun setAutoUpdateEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setAutoUpdateEnabled(enabled) }
    }

    fun setAutoProcessEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setAutoProcessEnabled(enabled) }
    }

    fun setTtsSpeed(value: Float) {
        viewModelScope.launch { settingsStore.setTtsSpeed(value) }
    }

    fun setTtsPitch(value: Float) {
        viewModelScope.launch { settingsStore.setTtsPitch(value) }
    }

    fun setTtsVoiceProfileId(value: String) {
        viewModelScope.launch {
            settingsStore.setTtsVoiceProfileId(value)
            localState.value = localState.value.copy(
                message = if (value == "default") {
                    "已切换到系统默认音色"
                } else {
                    "已切换到火山引擎音色档位（当前设备未接入火山引擎 speakerId 时会回退系统音色）"
                }
            )
        }
    }

    private suspend fun processPending(prefix: String): Int {
        val ageGroup = settingsStore.getAgeGroup() ?: AgeGroup.ADULT
        val language = settingsStore.contentLanguageFlow.first()
        val threshold = settingsStore.getNearDedupThreshold()
        val processed = processor.processBatch(ageGroup, language, threshold)
        val inserted = processed.getOrDefault(0)
        localState.value = localState.value.copy(
            message = "$prefix，入库$inserted"
        )
        return inserted
    }

    private suspend fun runFetchAndProcess(prefix: String) {
        val fetch = fetcher.fetchOnce()
        if (fetch.isFailure) {
            localState.value = localState.value.copy(message = "抓取失败：${fetch.exceptionOrNull()?.message}")
            return
        }
        val fetched = fetch.getOrDefault(0)
        val processed = processPending("$prefix：抓取$fetched")
        if (fetched == 0 && processed == 0) {
            val language = settingsStore.contentLanguageFlow.first()
            importer.importText(
                sourceId = "builtin-fallback-zh",
                text = fallbackZhJokes().joinToString("\n"),
                language = language,
                format = "txt"
            )
            val fallbackProcessed = processPending("$prefix：抓取$fetched（已启用内置中文兜底）")
            if (fallbackProcessed == 0) {
                localState.value = localState.value.copy(message = "$prefix：暂无可用数据源，请检查来源配置")
            }
        }
    }
}

private fun fallbackZhJokes(): List<String> = listOf(
    "程序员去相亲，对方问会做饭吗？他说：会，煮异常最拿手。",
    "我问 AI 你会讲笑话吗？它说会，然后给我输出了一个 bug。",
    "同事说代码要优雅，我把 if 写成了诗，他让我重构。",
    "产品说这个需求很简单，我的 CPU 当场进入省电模式。",
    "老板问进度如何，我说已经 80%，剩下 80% 明天完成。",
    "测试提了 100 个问题，我说这证明系统覆盖率很高。",
    "我把注释删了，代码跑得更快了，因为没人敢改了。",
    "数据库说我很稳定，只是偶尔在周五晚上情绪化。",
    "今天修了一个线上 bug，奖励是再给我一个线上 bug。",
    "代码评审时我写了 todo，领导说这就是长期规划。"
)

internal fun sanitizeForSpeech(content: String): String {
    val trimmed = content.trim()
    if (trimmed.isBlank()) return content

    val inlineSanitized = trimmed.replace(
        Regex("\\s*[—-]{1,2}\\s*(作者|来源|by|author)[:：]?\\s*[^\\n]{1,60}$", RegexOption.IGNORE_CASE),
        ""
    )

    val lines = inlineSanitized.lines().toMutableList()
    while (lines.isNotEmpty()) {
        val last = lines.last().trim()
        val isAuthorLine = last.matches(Regex("^(作者|来源|by|author)[:：].*$", RegexOption.IGNORE_CASE))
        val isDashTail =
            last.matches(Regex("^[—-]{1,2}\\s*(作者|来源|by|author)?[:：]?\\s*[^\\n]{1,40}$", RegexOption.IGNORE_CASE))
        val isAnonymousTail = last.matches(
            Regex("^(?:[（(]\\s*)?(?:佚名|匿名|无名氏|unknown|anonymous)(?:\\s*[）)])?$", RegexOption.IGNORE_CASE)
        )
        val previousLineLength = lines.dropLast(1).lastOrNull()?.trim()?.length ?: 0
        val isLikelyStandaloneAuthorName = previousLineLength >= 8 && isLikelyAuthorName(last)
        if (isAuthorLine || isDashTail || isAnonymousTail || isLikelyStandaloneAuthorName) {
            lines.removeAt(lines.lastIndex)
        } else {
            break
        }
    }

    val result = lines.joinToString("\n").trim()
    return result.ifBlank { trimmed }
}

private fun isLikelyAuthorName(value: String): Boolean {
    if (value.length !in 2..32) return false
    if (value.any { it.isDigit() }) return false
    if (value.contains(Regex("[。！？!?；;，,、….:：]"))) return false
    val trimmed = value.trim('(', ')', '（', '）', '[', ']', '【', '】', ' ')
    if (trimmed.isBlank()) return false
    val zhName = Regex("^[\\u4E00-\\u9FFF·•]{2,8}$")
    val latinName = Regex("^[A-Za-z][A-Za-z .'-]{1,30}$")
    return zhName.matches(trimmed) || latinName.matches(trimmed)
}

private data class LanguageSettings(
    val uiMode: LanguageMode,
    val uiLanguage: String,
    val contentMode: LanguageMode,
    val contentLanguage: String
)

private data class FeatureSettings(
    val autoUpdate: Boolean,
    val autoProcess: Boolean,
    val ttsVoiceProfileId: String,
    val ttsSpeed: Float,
    val ttsPitch: Float
)

private data class DisplaySettings(
    val language: LanguageSettings,
    val feature: FeatureSettings
)

private data class PlaybackStats(
    val unplayed: Int,
    val pending: Int
)

class JokeBoxViewModelFactory(
    private val settingsStore: SettingsStore,
    private val sourceRepository: SourceRepository,
    private val playbackRepository: PlaybackRepository,
    private val fetcher: Fetcher,
    private val importer: Importer,
    private val processor: Processor,
    private val ttsEngine: TtsEngine,
    private val rawQueueDao: RawQueueDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return JokeBoxViewModel(
            settingsStore = settingsStore,
            sourceRepository = sourceRepository,
            playbackRepository = playbackRepository,
            fetcher = fetcher,
            importer = importer,
            processor = processor,
            ttsEngine = ttsEngine,
            rawQueueDao = rawQueueDao
        ) as T
    }
}

