package com.jokebox.app.data.repo

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jokebox.app.data.model.AgeGroup
import com.jokebox.app.data.model.LanguageMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "jokebox_settings")

class SettingsStore(private val context: Context) {
    private object Keys {
        val ageGroup = intPreferencesKey("ageGroup")
        val ageLocked = booleanPreferencesKey("ageLocked")
        val complianceAccepted = booleanPreferencesKey("complianceAccepted")
        val uiLanguageMode = stringPreferencesKey("uiLanguageMode")
        val uiLanguage = stringPreferencesKey("uiLanguage")
        val contentLanguageMode = stringPreferencesKey("contentLanguageMode")
        val contentLanguage = stringPreferencesKey("contentLanguage")
        val autoUpdateEnabled = booleanPreferencesKey("autoUpdateEnabled")
        val autoProcessEnabled = booleanPreferencesKey("autoProcessEnabled")
        val lastUpdateAt = longPreferencesKey("lastUpdateAt")
        val ttsVoiceProfileId = stringPreferencesKey("ttsVoiceProfileId")
        val ttsSpeed = floatPreferencesKey("ttsSpeed")
        val ttsPitch = floatPreferencesKey("ttsPitch")
        val nearDedupThreshold = intPreferencesKey("nearDedupThreshold")
    }

    val ageLockedFlow: Flow<Boolean> = context.settingsDataStore.data.map { it[Keys.ageLocked] ?: false }
    val complianceAcceptedFlow: Flow<Boolean> =
        context.settingsDataStore.data.map { it[Keys.complianceAccepted] ?: false }
    val selectedAgeGroupFlow: Flow<AgeGroup?> = context.settingsDataStore.data.map { pref ->
        pref[Keys.ageGroup]?.let { AgeGroup.fromValue(it) }
    }
    val uiLanguageModeFlow: Flow<LanguageMode> = context.settingsDataStore.data.map {
        LanguageMode.valueOf(it[Keys.uiLanguageMode] ?: LanguageMode.SYSTEM.name)
    }
    val uiLanguageFlow: Flow<String> = context.settingsDataStore.data.map { it[Keys.uiLanguage] ?: "zh-Hans" }
    val contentLanguageModeFlow: Flow<LanguageMode> = context.settingsDataStore.data.map {
        LanguageMode.valueOf(it[Keys.contentLanguageMode] ?: LanguageMode.SYSTEM.name)
    }
    val contentLanguageSelectedFlow: Flow<String> =
        context.settingsDataStore.data.map { it[Keys.contentLanguage] ?: "zh-Hans" }
    val autoUpdateEnabledFlow: Flow<Boolean> = context.settingsDataStore.data.map { it[Keys.autoUpdateEnabled] ?: true }
    val autoProcessEnabledFlow: Flow<Boolean> = context.settingsDataStore.data.map { it[Keys.autoProcessEnabled] ?: true }
    val ttsSpeedFlow: Flow<Float> = context.settingsDataStore.data.map { it[Keys.ttsSpeed] ?: 1f }
    val ttsPitchFlow: Flow<Float> = context.settingsDataStore.data.map { it[Keys.ttsPitch] ?: 1f }
    val ttsVoiceProfileIdFlow: Flow<String> =
        context.settingsDataStore.data.map { it[Keys.ttsVoiceProfileId] ?: "ICL_zh_female_keainvsheng_tob" }

    val contentLanguageFlow: Flow<String> = context.settingsDataStore.data.map { pref ->
        val mode = LanguageMode.valueOf(pref[Keys.contentLanguageMode] ?: LanguageMode.SYSTEM.name)
        if (mode == LanguageMode.SYSTEM) context.resources.configuration.locales[0].toLanguageTag()
        else pref[Keys.contentLanguage] ?: "en"
    }

    suspend fun lockAgeGroup(ageGroup: AgeGroup) {
        context.settingsDataStore.edit {
            it[Keys.ageGroup] = ageGroup.value
            it[Keys.ageLocked] = true
        }
    }

    suspend fun setComplianceAccepted(value: Boolean) {
        context.settingsDataStore.edit { it[Keys.complianceAccepted] = value }
    }

    suspend fun getComplianceAccepted(): Boolean = complianceAcceptedFlow.first()

    suspend fun getAgeGroup(): AgeGroup? = selectedAgeGroupFlow.first()

    suspend fun getNearDedupThreshold(): Int =
        context.settingsDataStore.data.map { it[Keys.nearDedupThreshold] ?: 4 }.first()

    suspend fun setLastUpdateAt(ts: Long) {
        context.settingsDataStore.edit { it[Keys.lastUpdateAt] = ts }
    }

    suspend fun setUiLanguageMode(mode: LanguageMode) {
        context.settingsDataStore.edit { it[Keys.uiLanguageMode] = mode.name }
    }

    suspend fun setContentLanguageMode(mode: LanguageMode) {
        context.settingsDataStore.edit { it[Keys.contentLanguageMode] = mode.name }
    }

    suspend fun setUiLanguage(language: String) {
        context.settingsDataStore.edit { it[Keys.uiLanguage] = language }
    }

    suspend fun setContentLanguage(language: String) {
        context.settingsDataStore.edit { it[Keys.contentLanguage] = language }
    }

    suspend fun setAutoUpdateEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.autoUpdateEnabled] = enabled }
    }

    suspend fun setAutoProcessEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.autoProcessEnabled] = enabled }
    }

    suspend fun setTtsSpeed(value: Float) {
        context.settingsDataStore.edit { it[Keys.ttsSpeed] = value.coerceIn(0.5f, 2.0f) }
    }

    suspend fun setTtsPitch(value: Float) {
        context.settingsDataStore.edit { it[Keys.ttsPitch] = value.coerceIn(0.5f, 2.0f) }
    }

    suspend fun setTtsVoiceProfileId(value: String) {
        context.settingsDataStore.edit { it[Keys.ttsVoiceProfileId] = value.ifBlank { "default" } }
    }

    suspend fun resetPlaybackAndUserDataPreferences() {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.lastUpdateAt] = 0L
        }
    }

    suspend fun setDefaultsIfMissing() {
        context.settingsDataStore.edit { pref: MutablePreferences ->
            if (pref[Keys.uiLanguageMode] == null) pref[Keys.uiLanguageMode] = LanguageMode.SYSTEM.name
            if (pref[Keys.uiLanguage] == null) pref[Keys.uiLanguage] = "zh-Hans"
            if (pref[Keys.contentLanguageMode] == null) pref[Keys.contentLanguageMode] = LanguageMode.SYSTEM.name
            if (pref[Keys.contentLanguage] == null) pref[Keys.contentLanguage] = "zh-Hans"
            if (pref[Keys.autoUpdateEnabled] == null) pref[Keys.autoUpdateEnabled] = true
            if (pref[Keys.autoProcessEnabled] == null) pref[Keys.autoProcessEnabled] = true
            if (pref[Keys.ttsSpeed] == null) pref[Keys.ttsSpeed] = 1f
            if (pref[Keys.ttsPitch] == null) pref[Keys.ttsPitch] = 1f
            if (pref[Keys.ttsVoiceProfileId] == null) pref[Keys.ttsVoiceProfileId] = "ICL_zh_female_keainvsheng_tob"
            if (pref[Keys.nearDedupThreshold] == null) pref[Keys.nearDedupThreshold] = 4
        }
    }
}
