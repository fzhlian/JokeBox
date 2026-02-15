package fzhlian.JokeBox.app.ui.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import fzhlian.JokeBox.app.ui.state.MainUiState
import org.junit.Rule
import org.junit.Test

class MainScreenNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun onboarding_shouldRequireAgeAndAgreementBeforeContinue() {
        composeRule.setContent {
            MainScreen(
                uiState = MainUiState(ageLocked = false, complianceAccepted = false),
                onCompleteOnboarding = {},
                onPrev = {},
                onNext = {},
                onFavorite = {},
                onUpdate = {},
                onResetPlayed = {},
                onSpeak = {},
                onStopSpeak = {},
                onProcess = {},
                onSetSourceEnabled = { _, _ -> },
                onDeleteSource = {},
                onAddUserOnlineSource = { _, _, _, _, _, _, _ -> },
                onUpdateUserOnlineSource = { _, _, _, _, _, _, _, _ -> },
                onTestUserOnlineSource = { _, _, _ -> },
                onImportOfflineText = { _, _ -> },
                onClearUserSourceData = {},
                onSetUiLanguageMode = {},
                onSetUiLanguage = {},
                onSetContentLanguageMode = {},
                onSetContentLanguage = {},
                onSetAutoUpdateEnabled = {},
                onSetAutoProcessEnabled = {},
                onSetTtsSpeed = {},
                onSetTtsPitch = {}
            )
        }

        composeRule.onNodeWithText("确认并进入主页面").assertIsNotEnabled()
        composeRule.onNodeWithText("成人").performClick()
        composeRule.onNodeWithText("我已阅读并同意合规声明").performClick()
        composeRule.onNodeWithText("确认并进入主页面").assertIsEnabled()
    }

    @Test
    fun navigation_shouldOpenSourcesAndSettingsPages() {
        composeRule.setContent {
            MainScreen(
                uiState = MainUiState(ageLocked = true, complianceAccepted = true),
                onCompleteOnboarding = {},
                onPrev = {},
                onNext = {},
                onFavorite = {},
                onUpdate = {},
                onResetPlayed = {},
                onSpeak = {},
                onStopSpeak = {},
                onProcess = {},
                onSetSourceEnabled = { _, _ -> },
                onDeleteSource = {},
                onAddUserOnlineSource = { _, _, _, _, _, _, _ -> },
                onUpdateUserOnlineSource = { _, _, _, _, _, _, _, _ -> },
                onTestUserOnlineSource = { _, _, _ -> },
                onImportOfflineText = { _, _ -> },
                onClearUserSourceData = {},
                onSetUiLanguageMode = {},
                onSetUiLanguage = {},
                onSetContentLanguageMode = {},
                onSetContentLanguage = {},
                onSetAutoUpdateEnabled = {},
                onSetAutoProcessEnabled = {},
                onSetTtsSpeed = {},
                onSetTtsPitch = {}
            )
        }

        composeRule.onNodeWithText("来源").performClick()
        composeRule.onNodeWithText("新增用户在线来源").assertIsDisplayed()

        composeRule.onNodeWithText("设置").performClick()
        composeRule.onNodeWithText("UI语言模式").assertIsDisplayed()
    }
}

