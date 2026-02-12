package com.jokebox.app.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jokebox.app.data.model.AgeGroup
import com.jokebox.app.data.model.LanguageMode
import com.jokebox.app.data.model.SourceType
import com.jokebox.app.ui.state.MainUiState

private enum class HomeTab {
    MAIN,
    SOURCES,
    SETTINGS
}

@Composable
fun MainScreen(
    uiState: MainUiState,
    onCompleteOnboarding: (AgeGroup) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onFavorite: () -> Unit,
    onUpdate: () -> Unit,
    onResetPlayed: () -> Unit,
    onSpeak: () -> Unit,
    onStopSpeak: () -> Unit,
    onProcess: () -> Unit,
    onSetSourceEnabled: (String, Boolean) -> Unit,
    onDeleteSource: (String) -> Unit,
    onAddUserOnlineSource: (String, String, String, String, String, String, String) -> Unit,
    onUpdateUserOnlineSource: (String, String, String, String, String, String, String, String) -> Unit,
    onTestUserOnlineSource: (String, String, String) -> Unit,
    onImportOfflineText: (String, String) -> Unit,
    onClearUserSourceData: () -> Unit,
    onSetUiLanguageMode: (LanguageMode) -> Unit,
    onSetUiLanguage: (String) -> Unit,
    onSetContentLanguageMode: (LanguageMode) -> Unit,
    onSetContentLanguage: (String) -> Unit,
    onSetAutoUpdateEnabled: (Boolean) -> Unit,
    onSetAutoProcessEnabled: (Boolean) -> Unit,
    onSetTtsSpeed: (Float) -> Unit,
    onSetTtsPitch: (Float) -> Unit
) {
    val needOnboarding = !uiState.ageLocked || !uiState.complianceAccepted
    if (needOnboarding) {
        OnboardingScreen(onCompleteOnboarding = onCompleteOnboarding)
        return
    }

    var tab by remember { mutableStateOf(HomeTab.MAIN) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TabBar(tab = tab, onTabChange = { tab = it })

        when (tab) {
            HomeTab.MAIN -> MainTab(
                uiState = uiState,
                onPrev = onPrev,
                onNext = onNext,
                onFavorite = onFavorite,
                onUpdate = onUpdate,
                onResetPlayed = onResetPlayed,
                onSpeak = onSpeak,
                onStopSpeak = onStopSpeak,
                onProcess = onProcess
            )

            HomeTab.SOURCES -> SourcesTab(
                uiState = uiState,
                onSetSourceEnabled = onSetSourceEnabled,
                onDeleteSource = onDeleteSource,
                onAddUserOnlineSource = onAddUserOnlineSource,
                onUpdateUserOnlineSource = onUpdateUserOnlineSource,
                onTestUserOnlineSource = onTestUserOnlineSource,
                onImportOfflineText = onImportOfflineText,
                onClearUserSourceData = onClearUserSourceData
            )

            HomeTab.SETTINGS -> SettingsTab(
                uiState = uiState,
                onSetUiLanguageMode = onSetUiLanguageMode,
                onSetUiLanguage = onSetUiLanguage,
                onSetContentLanguageMode = onSetContentLanguageMode,
                onSetContentLanguage = onSetContentLanguage,
                onSetAutoUpdateEnabled = onSetAutoUpdateEnabled,
                onSetAutoProcessEnabled = onSetAutoProcessEnabled,
                onSetTtsSpeed = onSetTtsSpeed,
                onSetTtsPitch = onSetTtsPitch,
                onResetPlayed = onResetPlayed
            )
        }

        uiState.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
private fun TabBar(tab: HomeTab, onTabChange: (HomeTab) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        val items = listOf(
            HomeTab.MAIN to "主页",
            HomeTab.SOURCES to "来源",
            HomeTab.SETTINGS to "设置"
        )
        items.forEach { (item, text) ->
            val selected = item == tab
            if (selected) {
                Button(onClick = { onTabChange(item) }, modifier = Modifier.weight(1f)) { Text(text) }
            } else {
                OutlinedButton(onClick = { onTabChange(item) }, modifier = Modifier.weight(1f)) { Text(text) }
            }
        }
    }
}

@Composable
private fun MainTab(
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
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("年龄段：${uiState.selectedAgeGroup}", style = MaterialTheme.typography.titleMedium)

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

@Composable
private fun SourcesTab(
    uiState: MainUiState,
    onSetSourceEnabled: (String, Boolean) -> Unit,
    onDeleteSource: (String) -> Unit,
    onAddUserOnlineSource: (String, String, String, String, String, String, String) -> Unit,
    onUpdateUserOnlineSource: (String, String, String, String, String, String, String, String) -> Unit,
    onTestUserOnlineSource: (String, String, String) -> Unit,
    onImportOfflineText: (String, String) -> Unit,
    onClearUserSourceData: () -> Unit
) {
    val scroll = rememberScrollState()

    var srcName by remember { mutableStateOf("") }
    var srcUrl by remember { mutableStateOf("") }
    var srcItemsPath by remember { mutableStateOf("data.items") }
    var srcContentPath by remember { mutableStateOf("content") }
    var srcLanguagePath by remember { mutableStateOf("language") }
    var srcSourceUrlPath by remember { mutableStateOf("url") }
    var srcLicenseNote by remember { mutableStateOf("") }
    var editingSourceId by remember { mutableStateOf("") }

    var importFormat by remember { mutableStateOf("txt") }
    var importText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("来源状态", style = MaterialTheme.typography.titleSmall)
                Text("总数：${uiState.sources.size}，启用：${uiState.sources.count { it.enabled }}")
            }
        }

        uiState.sources.forEach { source ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(source.name, style = MaterialTheme.typography.titleSmall)
                        Switch(
                            checked = source.enabled,
                            onCheckedChange = { checked -> onSetSourceEnabled(source.sourceId, checked) }
                        )
                    }
                    Text("类型：${source.type}")
                    if (!source.licenseNote.isNullOrBlank()) Text("License: ${source.licenseNote}")
                    if (!source.toSNote.isNullOrBlank()) Text("ToS: ${source.toSNote}")
                    if (source.type != SourceType.BUILTIN) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = {
                                    editingSourceId = source.sourceId
                                    srcName = source.name
                                    srcLicenseNote = source.licenseNote.orEmpty()
                                    val cfg = source.configJson.orEmpty()
                                    val reqUrl = Regex("\"url\"\\s*:\\s*\"([^\"]+)\"").find(cfg)?.groupValues?.getOrNull(1).orEmpty()
                                    val itemsPath = Regex("\"itemsPath\"\\s*:\\s*\"([^\"]+)\"").find(cfg)?.groupValues?.getOrNull(1).orEmpty()
                                    val contentPath = Regex("\"content\"\\s*:\\s*\"([^\"]+)\"").find(cfg)?.groupValues?.getOrNull(1).orEmpty()
                                    val languagePath = Regex("\"language\"\\s*:\\s*\"([^\"]+)\"").find(cfg)?.groupValues?.getOrNull(1).orEmpty()
                                    val sourceUrlPath = Regex("\"sourceUrl\"\\s*:\\s*\"([^\"]+)\"").find(cfg)?.groupValues?.getOrNull(1).orEmpty()
                                    srcUrl = reqUrl
                                    srcItemsPath = itemsPath
                                    srcContentPath = contentPath
                                    srcLanguagePath = languagePath
                                    srcSourceUrlPath = sourceUrlPath
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("编辑")
                            }
                            OutlinedButton(
                                onClick = { onDeleteSource(source.sourceId) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("删除")
                            }
                        }
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("新增用户在线来源", style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(srcName, { srcName = it }, label = { Text("名称") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(srcUrl, { srcUrl = it }, label = { Text("URL") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(srcItemsPath, { srcItemsPath = it }, label = { Text("itemsPath") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(srcContentPath, { srcContentPath = it }, label = { Text("contentPath") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(srcLanguagePath, { srcLanguagePath = it }, label = { Text("languagePath(可空)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(srcSourceUrlPath, { srcSourceUrlPath = it }, label = { Text("sourceUrlPath(可空)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(srcLicenseNote, { srcLicenseNote = it }, label = { Text("licenseNote(可空)") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { onTestUserOnlineSource(srcUrl, srcItemsPath, srcContentPath) },
                        enabled = srcUrl.isNotBlank() && srcItemsPath.isNotBlank() && srcContentPath.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("连接测试")
                    }
                    OutlinedButton(
                        onClick = {
                            editingSourceId = ""
                            srcName = ""
                            srcUrl = ""
                            srcItemsPath = "data.items"
                            srcContentPath = "content"
                            srcLanguagePath = "language"
                            srcSourceUrlPath = "url"
                            srcLicenseNote = ""
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("清空表单")
                    }
                }
                Button(
                    onClick = {
                        if (editingSourceId.isBlank()) {
                            onAddUserOnlineSource(
                                srcName,
                                srcUrl,
                                srcItemsPath,
                                srcContentPath,
                                srcLanguagePath,
                                srcSourceUrlPath,
                                srcLicenseNote
                            )
                            srcName = ""
                            srcUrl = ""
                        } else {
                            onUpdateUserOnlineSource(
                                editingSourceId,
                                srcName,
                                srcUrl,
                                srcItemsPath,
                                srcContentPath,
                                srcLanguagePath,
                                srcSourceUrlPath,
                                srcLicenseNote
                            )
                            editingSourceId = ""
                        }
                    },
                    enabled = srcName.isNotBlank() && srcUrl.isNotBlank() && srcItemsPath.isNotBlank() && srcContentPath.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (editingSourceId.isBlank()) "保存并启用" else "保存修改")
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("离线导入", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("txt", "json", "csv", "html").forEach { fmt ->
                        if (fmt == importFormat) {
                            Button(onClick = { importFormat = fmt }, modifier = Modifier.weight(1f)) { Text(fmt.uppercase()) }
                        } else {
                            OutlinedButton(onClick = { importFormat = fmt }, modifier = Modifier.weight(1f)) {
                                Text(fmt.uppercase())
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = importText,
                    onValueChange = { importText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                    label = { Text("粘贴离线内容") },
                    minLines = 5,
                    maxLines = 12
                )
                Button(
                    onClick = { onImportOfflineText(importFormat, importText) },
                    enabled = importText.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("导入并处理")
                }
            }
        }

        OutlinedButton(onClick = onClearUserSourceData, modifier = Modifier.fillMaxWidth()) {
            Text("清空用户来源数据")
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("查看合规声明", style = MaterialTheme.typography.titleSmall)
                Text(COMPLIANCE_DECLARATION, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun SettingsTab(
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

@Composable
private fun OnboardingScreen(onCompleteOnboarding: (AgeGroup) -> Unit) {
    var selectedAgeGroup by remember { mutableStateOf<AgeGroup?>(null) }
    var agreed by remember { mutableStateOf(false) }
    val canContinue = selectedAgeGroup != null && agreed
    val complianceScrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("首次启动：请选择年龄段（确认后不可修改）")
                AgeOptionRow("少年", selectedAgeGroup == AgeGroup.TEEN) { selectedAgeGroup = AgeGroup.TEEN }
                AgeOptionRow("青年", selectedAgeGroup == AgeGroup.YOUTH) { selectedAgeGroup = AgeGroup.YOUTH }
                AgeOptionRow("成人", selectedAgeGroup == AgeGroup.ADULT) { selectedAgeGroup = AgeGroup.ADULT }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("合规声明（请完整阅读）", style = MaterialTheme.typography.titleMedium)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp, max = 260.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(complianceScrollState)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(COMPLIANCE_DECLARATION, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { agreed = !agreed },
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(checked = agreed, onCheckedChange = { agreed = it })
                    Text("我已阅读并同意合规声明")
                }
            }
        }

        Button(
            onClick = { onCompleteOnboarding(selectedAgeGroup!!) },
            enabled = canContinue,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("确认并进入主页面")
        }
    }
}

private const val COMPLIANCE_DECLARATION = """
《内容来源与使用合规声明》

在你启用自定义在线来源或离线导入内容前，请认真阅读并确认以下条款。你继续操作即表示你已理解并同意承担相应责任：

1. 你承诺对导入、抓取、配置并在本应用中使用的全部内容拥有合法授权，或该内容属于可依法使用的公开信息。

2. 你承诺遵守来源网站、平台或接口的服务条款（ToS）、robots 协议、访问频率限制、版权声明及相关法律法规。

3. 你确认本应用仅提供本地化的技术处理能力，不提供任何绕过权限、破解限制、规避付费、规避反爬机制或规避访问控制的能力。

4. 你应自行核验来源合法性与内容合规性，并对由你的来源配置、导入文件与后续使用行为产生的一切后果独立负责。

5. 若你发现内容存在侵权、违规、违法、误导、仇恨、极端暴力煽动或其他不当信息，你应立即停止使用相关来源并删除对应数据。

6. 本应用可根据本地策略对内容进行分龄过滤、去重和降级处理；该处理不构成法律审查、合规背书或内容真实性担保。

7. 你理解并同意：若因你的来源或内容引发投诉、争议、索赔、处罚或损失，责任由你自行承担；你应及时配合处置并移除相关数据。

8. 你可在设置中随时查看本声明，并可执行“清空用户来源数据”以删除用户来源配置及其导入/抓取内容。
"""

@Composable
private fun AgeOptionRow(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text)
    }
}
