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
    onSetTtsSpeed: (Float) -> Unit,
    onSetTtsPitch: (Float) -> Unit,
    onResetPlayed: () -> Unit
) {
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("UI语言模式")
                LanguageModeRow(current = uiState.uiLanguageMode, onChange = onSetUiLanguageMode)
                LanguageChoiceRow(current = uiState.uiLanguage, onChange = onSetUiLanguage)

                Text("内容语言模式")
                LanguageModeRow(current = uiState.contentLanguageMode, onChange = onSetContentLanguageMode)
                LanguageChoiceRow(current = uiState.contentLanguage, onChange = onSetContentLanguage)
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("自动更新")
                    Switch(checked = uiState.autoUpdateEnabled, onCheckedChange = onSetAutoUpdateEnabled)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("后台处理")
                    Switch(checked = uiState.autoProcessEnabled, onCheckedChange = onSetAutoProcessEnabled)
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("TTS语速：${String.format("%.2f", uiState.ttsSpeed)}")
                Slider(value = uiState.ttsSpeed, onValueChange = onSetTtsSpeed, valueRange = 0.5f..2.0f)
                Text("TTS音调：${String.format("%.2f", uiState.ttsPitch)}")
                Slider(value = uiState.ttsPitch, onValueChange = onSetTtsPitch, valueRange = 0.5f..2.0f)
            }
        }

        OutlinedButton(onClick = onResetPlayed, modifier = Modifier.fillMaxWidth()) {
            Text("清空已播")
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("查看合规声明")
                Text(COMPLIANCE_DECLARATION)
            }
        }
    }
}

@Composable
private fun LanguageModeRow(current: LanguageMode, onChange: (LanguageMode) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        LanguageMode.entries.forEach { mode ->
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onChange(mode) },
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                RadioButton(selected = current == mode, onClick = { onChange(mode) })
                Text(mode.name)
            }
        }
    }
}

@Composable
private fun LanguageChoiceRow(current: String, onChange: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        listOf("zh-Hans", "zh-Hant", "en").forEach { lang ->
            val selected = current == lang
            if (selected) {
                Button(onClick = { onChange(lang) }, modifier = Modifier.weight(1f)) { Text(lang) }
            } else {
                OutlinedButton(onClick = { onChange(lang) }, modifier = Modifier.weight(1f)) { Text(lang) }
            }
        }
    }
}
