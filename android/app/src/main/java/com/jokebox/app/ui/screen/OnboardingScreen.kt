package com.jokebox.app.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jokebox.app.data.model.AgeGroup

@Composable
fun OnboardingScreen(onCompleteOnboarding: (AgeGroup) -> Unit) {
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
