package com.jokebox.app.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.jokebox.app.data.model.LanguageMode
import com.jokebox.app.ui.state.MainUiState

@Composable
fun SettingsPage(
    uiState: MainUiState,
    onSetUiLanguageMode: (LanguageMode) -> Unit,
    onSetUiLanguage: (String) -> Unit,
    onSetContentLanguageMode: (LanguageMode) -> Unit,
    onSetContentLanguage: (String) -> Unit,
    onSetAutoUpdateEnabled: (Boolean) -> Unit,
    onSetAutoProcessEnabled: (Boolean) -> Unit,
    onSetTtsVoiceProfileId: (String) -> Unit,
    onSetTtsSpeed: (Float) -> Unit,
    onSetTtsPitch: (Float) -> Unit,
    onResetPlayed: () -> Unit
) {
    val scroll = rememberScrollState()
    val localeTag = LocalConfiguration.current.locales[0]?.toLanguageTag().orEmpty().lowercase()
    val zh = localeTag.startsWith("zh")
    val uiLanguageModeText = if (zh) ZhText.uiLanguageMode else EnText.uiLanguageMode
    val contentLanguageModeText = if (zh) ZhText.contentLanguageMode else EnText.contentLanguageMode
    val autoUpdateText = if (zh) ZhText.autoUpdate else EnText.autoUpdate
    val autoProcessText = if (zh) ZhText.autoProcess else EnText.autoProcess
    val ttsVoiceText = if (zh) ZhText.ttsVoice else EnText.ttsVoice
    val ttsVoiceHintText = if (zh) ZhText.ttsVoiceHint else EnText.ttsVoiceHint
    val ttsSpeedText = if (zh) ZhText.ttsSpeed else EnText.ttsSpeed
    val ttsPitchText = if (zh) ZhText.ttsPitch else EnText.ttsPitch
    val clearPlayedText = if (zh) ZhText.clearPlayed else EnText.clearPlayed

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(uiLanguageModeText)
                LanguageModeRow(current = uiState.uiLanguageMode, onChange = onSetUiLanguageMode, zh = zh)
                LanguageChoiceRow(current = uiState.uiLanguage, onChange = onSetUiLanguage, zh = zh)

                Text(contentLanguageModeText)
                LanguageModeRow(current = uiState.contentLanguageMode, onChange = onSetContentLanguageMode, zh = zh)
                LanguageChoiceRow(current = uiState.contentLanguage, onChange = onSetContentLanguage, zh = zh)
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(autoUpdateText)
                    Switch(checked = uiState.autoUpdateEnabled, onCheckedChange = onSetAutoUpdateEnabled)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(autoProcessText)
                    Switch(checked = uiState.autoProcessEnabled, onCheckedChange = onSetAutoProcessEnabled)
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(ttsVoiceText)
                TtsVoiceProfileRow(current = uiState.ttsVoiceProfileId, onChange = onSetTtsVoiceProfileId, zh = zh)
                Text(ttsVoiceHintText)
                Text("${ttsSpeedText}${String.format("%.2f", uiState.ttsSpeed)}")
                Slider(value = uiState.ttsSpeed, onValueChange = onSetTtsSpeed, valueRange = 0.5f..2.0f)
                Text("${ttsPitchText}${String.format("%.2f", uiState.ttsPitch)}")
                Slider(value = uiState.ttsPitch, onValueChange = onSetTtsPitch, valueRange = 0.5f..2.0f)
            }
        }

        OutlinedButton(onClick = onResetPlayed, modifier = Modifier.fillMaxWidth()) {
            Text(clearPlayedText)
        }
    }
}

@Composable
private fun LanguageModeRow(current: LanguageMode, onChange: (LanguageMode) -> Unit, zh: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        LanguageMode.entries.forEach { mode ->
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onChange(mode) },
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                RadioButton(selected = current == mode, onClick = { onChange(mode) })
                Text(
                    when (mode) {
                        LanguageMode.SYSTEM -> if (zh) "跟随系统" else "System"
                        LanguageMode.MANUAL -> if (zh) "手动" else "Manual"
                    }
                )
            }
        }
    }
}

@Composable
private fun LanguageChoiceRow(current: String, onChange: (String) -> Unit, zh: Boolean) {
    val labels = mapOf(
        "zh-Hans" to if (zh) "简体中文" else "Chinese (Simplified)",
        "zh-Hant" to if (zh) "繁體中文" else "Chinese (Traditional)",
        "en" to if (zh) "英语" else "English"
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        listOf("zh-Hans", "zh-Hant", "en").forEach { lang ->
            val selected = current == lang
            if (selected) {
                Button(onClick = { onChange(lang) }, modifier = Modifier.weight(1f)) { Text(labels[lang].orEmpty()) }
            } else {
                OutlinedButton(onClick = { onChange(lang) }, modifier = Modifier.weight(1f)) {
                    Text(labels[lang].orEmpty())
                }
            }
        }
    }
}

@Composable
private fun TtsVoiceProfileRow(current: String, onChange: (String) -> Unit, zh: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        VoiceProfileOption.entries.forEach { option ->
            val selected = current == option.id
            val label = option.label(zh)
            if (selected) {
                Button(onClick = { onChange(option.id) }, modifier = Modifier.fillMaxWidth()) {
                    Text(label)
                }
            } else {
                OutlinedButton(onClick = { onChange(option.id) }, modifier = Modifier.fillMaxWidth()) {
                    Text(label)
                }
            }
        }
    }
}

private enum class VoiceProfileOption(val id: String) {
    SYSTEM_DEFAULT("default"),
    DOUBAO_XKMS_LING("doubao_xkms_ling"),
    DOUBAO_XKMS_YUN("doubao_xkms_yun"),
    DOUBAO_XKMS_KIRA("doubao_xkms_kira");

    fun label(zh: Boolean): String {
        return when (this) {
            SYSTEM_DEFAULT -> if (zh) "系统默认" else "System Default"
            DOUBAO_XKMS_LING -> if (zh) "豆包·星卡梦少女·铃" else "Doubao XKMS Ling"
            DOUBAO_XKMS_YUN -> if (zh) "豆包·星卡梦少女·云" else "Doubao XKMS Yun"
            DOUBAO_XKMS_KIRA -> if (zh) "豆包·星卡梦少女·琪拉" else "Doubao XKMS Kira"
        }
    }
}

private data object ZhText {
    const val uiLanguageMode = "界面语言模式"
    const val contentLanguageMode = "内容语言模式"
    const val autoUpdate = "自动更新"
    const val autoProcess = "后台处理"
    const val ttsVoice = "TTS 音色"
    const val ttsVoiceHint = "星卡梦少女角色音色需在豆包/火山引擎控制台绑定真实 speakerId；未接入时将回退系统音色。"
    const val ttsSpeed = "TTS 语速："
    const val ttsPitch = "TTS 音调："
    const val clearPlayed = "清空已播"
}

private data object EnText {
    const val uiLanguageMode = "UI Language Mode"
    const val contentLanguageMode = "Content Language Mode"
    const val autoUpdate = "Auto Update"
    const val autoProcess = "Background Processing"
    const val ttsVoice = "TTS Voice"
    const val ttsVoiceHint = "XKMS character voices require real speakerId binding in Doubao/Volcengine console. Fallback to system voice when unavailable."
    const val ttsSpeed = "TTS Speed: "
    const val ttsPitch = "TTS Pitch: "
    const val clearPlayed = "Clear Played"
}
