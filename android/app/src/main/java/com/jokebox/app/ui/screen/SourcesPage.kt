package com.jokebox.app.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jokebox.app.data.model.SourceType
import com.jokebox.app.ui.state.MainUiState

private data class SourceFormErrors(
    val name: String? = null,
    val url: String? = null,
    val itemsPath: String? = null,
    val contentPath: String? = null,
    val languagePath: String? = null,
    val sourceUrlPath: String? = null,
    val licenseNote: String? = null
) {
    fun hasError(): Boolean {
        return name != null ||
            url != null ||
            itemsPath != null ||
            contentPath != null ||
            languagePath != null ||
            sourceUrlPath != null ||
            licenseNote != null
    }
}

private data class ImportFormErrors(
    val content: String? = null,
    val format: String? = null
) {
    fun hasError(): Boolean = content != null || format != null
}

@Composable
fun SourcesPage(
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
    var sourceErrors by remember { mutableStateOf(SourceFormErrors()) }

    var importFormat by remember { mutableStateOf("txt") }
    var importText by remember { mutableStateOf("") }
    var importErrors by remember { mutableStateOf(ImportFormErrors()) }

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
                                    srcUrl = Regex("\"url\"\\s*:\\s*\"([^\"]+)\"").find(cfg)?.groupValues?.getOrNull(1).orEmpty()
                                    srcItemsPath = Regex("\"itemsPath\"\\s*:\\s*\"([^\"]+)\"").find(cfg)?.groupValues?.getOrNull(1).orEmpty()
                                    srcContentPath = Regex("\"content\"\\s*:\\s*\"([^\"]+)\"").find(cfg)?.groupValues?.getOrNull(1).orEmpty()
                                    srcLanguagePath = Regex("\"language\"\\s*:\\s*\"([^\"]+)\"").find(cfg)?.groupValues?.getOrNull(1).orEmpty()
                                    srcSourceUrlPath = Regex("\"sourceUrl\"\\s*:\\s*\"([^\"]+)\"").find(cfg)?.groupValues?.getOrNull(1).orEmpty()
                                    sourceErrors = SourceFormErrors()
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

                OutlinedTextField(
                    value = srcName,
                    onValueChange = { srcName = it },
                    label = { Text("名称") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = sourceErrors.name != null,
                    supportingText = sourceErrors.name?.let { { Text(it) } }
                )
                OutlinedTextField(
                    value = srcUrl,
                    onValueChange = { srcUrl = it },
                    label = { Text("URL") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = sourceErrors.url != null,
                    supportingText = sourceErrors.url?.let { { Text(it) } }
                )
                OutlinedTextField(
                    value = srcItemsPath,
                    onValueChange = { srcItemsPath = it },
                    label = { Text("itemsPath") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = sourceErrors.itemsPath != null,
                    supportingText = sourceErrors.itemsPath?.let { { Text(it) } }
                )
                OutlinedTextField(
                    value = srcContentPath,
                    onValueChange = { srcContentPath = it },
                    label = { Text("contentPath") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = sourceErrors.contentPath != null,
                    supportingText = sourceErrors.contentPath?.let { { Text(it) } }
                )
                OutlinedTextField(
                    value = srcLanguagePath,
                    onValueChange = { srcLanguagePath = it },
                    label = { Text("languagePath(可空)") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = sourceErrors.languagePath != null,
                    supportingText = sourceErrors.languagePath?.let { { Text(it) } }
                )
                OutlinedTextField(
                    value = srcSourceUrlPath,
                    onValueChange = { srcSourceUrlPath = it },
                    label = { Text("sourceUrlPath(可空)") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = sourceErrors.sourceUrlPath != null,
                    supportingText = sourceErrors.sourceUrlPath?.let { { Text(it) } }
                )
                OutlinedTextField(
                    value = srcLicenseNote,
                    onValueChange = { srcLicenseNote = it },
                    label = { Text("licenseNote(可空)") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = sourceErrors.licenseNote != null,
                    supportingText = sourceErrors.licenseNote?.let { { Text(it) } }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            val errors = validateSourceForm(
                                name = srcName,
                                url = srcUrl,
                                itemsPath = srcItemsPath,
                                contentPath = srcContentPath,
                                languagePath = srcLanguagePath,
                                sourceUrlPath = srcSourceUrlPath,
                                licenseNote = srcLicenseNote
                            )
                            sourceErrors = errors
                            if (!errors.hasError()) {
                                onTestUserOnlineSource(srcUrl, srcItemsPath, srcContentPath)
                            }
                        },
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
                            sourceErrors = SourceFormErrors()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("清空表单")
                    }
                }

                Button(
                    onClick = {
                        val errors = validateSourceForm(
                            name = srcName,
                            url = srcUrl,
                            itemsPath = srcItemsPath,
                            contentPath = srcContentPath,
                            languagePath = srcLanguagePath,
                            sourceUrlPath = srcSourceUrlPath,
                            licenseNote = srcLicenseNote
                        )
                        sourceErrors = errors
                        if (errors.hasError()) {
                            return@Button
                        }

                        if (editingSourceId.isBlank()) {
                            onAddUserOnlineSource(
                                srcName.trim(),
                                srcUrl.trim(),
                                srcItemsPath.trim(),
                                srcContentPath.trim(),
                                srcLanguagePath.trim(),
                                srcSourceUrlPath.trim(),
                                srcLicenseNote.trim()
                            )
                            srcName = ""
                            srcUrl = ""
                        } else {
                            onUpdateUserOnlineSource(
                                editingSourceId,
                                srcName.trim(),
                                srcUrl.trim(),
                                srcItemsPath.trim(),
                                srcContentPath.trim(),
                                srcLanguagePath.trim(),
                                srcSourceUrlPath.trim(),
                                srcLicenseNote.trim()
                            )
                            editingSourceId = ""
                        }
                    },
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

                importErrors.format?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                OutlinedTextField(
                    value = importText,
                    onValueChange = { importText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                    label = { Text("粘贴离线内容") },
                    minLines = 5,
                    maxLines = 12,
                    isError = importErrors.content != null,
                    supportingText = importErrors.content?.let { { Text(it) } }
                )
                Button(
                    onClick = {
                        val errors = validateImportForm(importFormat, importText)
                        importErrors = errors
                        if (!errors.hasError()) {
                            onImportOfflineText(importFormat, importText)
                        }
                    },
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

private fun validateSourceForm(
    name: String,
    url: String,
    itemsPath: String,
    contentPath: String,
    languagePath: String,
    sourceUrlPath: String,
    licenseNote: String
): SourceFormErrors {
    return SourceFormErrors(
        name = when {
            name.isBlank() -> "名称不能为空"
            name.trim().length < 2 -> "名称至少 2 个字符"
            else -> null
        },
        url = when {
            url.isBlank() -> "URL 不能为空"
            !url.trim().matches(Regex("^https?://.+")) -> "URL 必须以 http:// 或 https:// 开头"
            else -> null
        },
        itemsPath = validateJsonPath(itemsPath, "itemsPath"),
        contentPath = validateJsonPath(contentPath, "contentPath"),
        languagePath = validateOptionalJsonPath(languagePath, "languagePath"),
        sourceUrlPath = validateOptionalJsonPath(sourceUrlPath, "sourceUrlPath"),
        licenseNote = when {
            licenseNote.length > 500 -> "licenseNote 不能超过 500 字"
            else -> null
        }
    )
}

private fun validateImportForm(format: String, content: String): ImportFormErrors {
    if (content.isBlank()) {
        return ImportFormErrors(content = "导入内容不能为空")
    }

    val trimmed = content.trim()
    val formatError = when (format) {
        "json" -> if (!trimmed.startsWith("[") && !trimmed.startsWith("{")) "JSON 内容需以 { 或 [ 开头" else null
        "csv" -> if (!trimmed.contains(",")) "CSV 内容至少应包含逗号分隔" else null
        "html" -> if (!trimmed.contains("<") || !trimmed.contains(">")) "HTML 内容应包含标签" else null
        else -> null
    }
    return ImportFormErrors(format = formatError)
}

private fun validateJsonPath(path: String, fieldName: String): String? {
    if (path.isBlank()) return "$fieldName 不能为空"
    return validatePathToken(path, fieldName)
}

private fun validateOptionalJsonPath(path: String, fieldName: String): String? {
    if (path.isBlank()) return null
    return validatePathToken(path, fieldName)
}

private fun validatePathToken(path: String, fieldName: String): String? {
    return if (path.trim().matches(Regex("^[A-Za-z0-9_.$\\[\\]-]+$"))) {
        null
    } else {
        "$fieldName 仅允许字母、数字、_、.、$、[]、-"
    }
}
