package fzhlian.jokebox.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fzhlian.jokebox.app.data.model.AgeGroup
import fzhlian.jokebox.app.ui.state.MainUiState

@Composable
fun MainPage(
    uiState: MainUiState,
    uiLanguageTag: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onFavorite: () -> Unit,
    onUpdate: () -> Unit,
    onClearAndRefetchChinese: () -> Unit,
    onResetPlayed: () -> Unit,
    onSpeak: () -> Unit,
    onStopSpeak: () -> Unit,
    onProcess: () -> Unit
) {
    val isEn = uiLanguageTag.lowercase().startsWith("en")
    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "${if (isEn) "Age Group" else "年龄段"}: ${ageGroupDisplayName(uiState.selectedAgeGroup, uiLanguageTag)}",
            style = MaterialTheme.typography.titleMedium
        )
        Text("${if (isEn) "Language" else "语言"}: ${uiState.currentJoke?.language ?: "-"}")
        Text(
            if (isEn) {
                "Unplayed: ${uiState.unplayedCount}, Pending: ${uiState.processingCount}"
            } else {
                "未播：${uiState.unplayedCount}，待处理：${uiState.processingCount}"
            }
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onPrev, modifier = Modifier.weight(1f)) { Text(if (isEn) "Prev" else "上一个") }
            Button(onClick = onNext, modifier = Modifier.weight(1f)) { Text(if (isEn) "Next" else "下一个") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onFavorite, modifier = Modifier.weight(1f)) {
                Text(if (isEn) "Favorite" else "收藏/取消")
            }
            OutlinedButton(onClick = onUpdate, modifier = Modifier.weight(1f)) { Text(if (isEn) "Update" else "更新") }
        }

        Button(onClick = onClearAndRefetchChinese, modifier = Modifier.fillMaxWidth()) {
            Text(if (isEn) "Clear & Refetch CN" else "清库并重抓中文")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onSpeak, modifier = Modifier.weight(1f)) { Text(if (isEn) "Speak" else "朗读") }
            OutlinedButton(onClick = onStopSpeak, modifier = Modifier.weight(1f)) { Text(if (isEn) "Stop" else "停止") }
            OutlinedButton(onClick = onProcess, modifier = Modifier.weight(1f)) { Text(if (isEn) "Process" else "仅处理") }
        }

        OutlinedButton(onClick = onResetPlayed, modifier = Modifier.fillMaxWidth()) {
            Text(if (isEn) "Reset Played" else "重置已播")
        }

        Spacer(modifier = Modifier.weight(1f))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    uiState.currentJoke?.content
                        ?: if (isEn) "No jokes available. Tap Update first." else "暂无可播放笑话，先点击“更新”"
                )
            }
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


