package fzhlian.jokebox.app.ui.state

import fzhlian.jokebox.app.data.model.AgeGroup
import fzhlian.jokebox.app.data.model.LanguageMode
import fzhlian.jokebox.app.data.model.JokeUiItem
import fzhlian.jokebox.app.data.db.SourceConfigEntity

data class MainUiState(
    val initialized: Boolean = false,
    val ageLocked: Boolean = false,
    val selectedAgeGroup: AgeGroup? = null,
    val complianceAccepted: Boolean = false,
    val currentJoke: JokeUiItem? = null,
    val unplayedCount: Int = 0,
    val processingCount: Int = 0,
    val sources: List<SourceConfigEntity> = emptyList(),
    val uiLanguageMode: LanguageMode = LanguageMode.SYSTEM,
    val uiLanguage: String = "zh-Hans",
    val contentLanguageMode: LanguageMode = LanguageMode.SYSTEM,
    val contentLanguage: String = "zh-Hans",
    val autoUpdateEnabled: Boolean = true,
    val autoProcessEnabled: Boolean = true,
    val ttsVoiceProfileId: String = "default",
    val ttsSpeed: Float = 1f,
    val ttsPitch: Float = 1f,
    val message: String? = null
)


