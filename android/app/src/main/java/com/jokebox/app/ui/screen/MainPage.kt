package com.jokebox.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.jokebox.app.data.model.AgeGroup
import com.jokebox.app.ui.state.MainUiState

@Composable
fun MainPage(
    uiState: MainUiState,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onFavorite: () -> Unit,
    onUpdate: () -> Unit,
    onResetPlayed: () -> Unit,
    onSpeak: () -> Unit,
    onStopSpeak: () -> Unit,
    onProcess: () -> Unit
) {
    val language = LocalConfiguration.current.locales[0]?.toLanguageTag().orEmpty()
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "年龄段：${ageGroupDisplayName(uiState.selectedAgeGroup, language)}",
            style = MaterialTheme.typography.titleMedium
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(uiState.currentJoke?.content ?: "暂无可播放笑话，先点击“更新”")
                Text("语言：${uiState.currentJoke?.language ?: "-"}")
                Text("未播：${uiState.unplayedCount}，待处理：${uiState.processingCount}")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onPrev, modifier = Modifier.weight(1f)) { Text("上一个") }
            Button(onClick = onNext, modifier = Modifier.weight(1f)) { Text("下一个") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onFavorite, modifier = Modifier.weight(1f)) { Text("收藏/取消") }
            OutlinedButton(onClick = onUpdate, modifier = Modifier.weight(1f)) { Text("更新") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onSpeak, modifier = Modifier.weight(1f)) { Text("朗读") }
            OutlinedButton(onClick = onStopSpeak, modifier = Modifier.weight(1f)) { Text("停止") }
            OutlinedButton(onClick = onProcess, modifier = Modifier.weight(1f)) { Text("仅处理") }
        }

        OutlinedButton(onClick = onResetPlayed, modifier = Modifier.fillMaxWidth()) {
            Text("重置已播")
        }
    }
}

private fun ageGroupDisplayName(ageGroup: AgeGroup?, uiLanguage: String): String {
    val lang = uiLanguage.lowercase()
    val isEn = lang.startsWith("en")
    return when (ageGroup) {
        AgeGroup.TEEN -> if (isEn) "Teen" else "少年"
        AgeGroup.YOUTH -> if (isEn) "Youth" else "青年"
        AgeGroup.ADULT -> if (isEn) "Adult" else "成人"
        null -> "-"
    }
}
