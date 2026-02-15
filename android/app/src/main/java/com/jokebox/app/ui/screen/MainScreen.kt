package fzhlian.jokebox.app.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import fzhlian.jokebox.app.data.model.AgeGroup
import fzhlian.jokebox.app.data.model.LanguageMode
import fzhlian.jokebox.app.ui.state.MainUiState

enum class AppRoute(val route: String) {
    MAIN("main"),
    SOURCES("sources"),
    SETTINGS("settings")
}

@Composable
private fun routeIcon(route: AppRoute, label: String) {
    when (route) {
        AppRoute.MAIN -> Icon(Icons.Outlined.Home, contentDescription = label)
        AppRoute.SOURCES -> Icon(Icons.Outlined.Storage, contentDescription = label)
        AppRoute.SETTINGS -> Icon(Icons.Outlined.Settings, contentDescription = label)
    }
}

private fun resolveUiLanguageTag(
    systemLocaleTag: String,
    mode: LanguageMode,
    selected: String
): String {
    return when (mode) {
        LanguageMode.SYSTEM -> systemLocaleTag
        LanguageMode.MANUAL -> selected
    }.ifBlank { "zh-Hans" }
}

@Composable
fun MainScreen(
    uiState: MainUiState,
    onCompleteOnboarding: (AgeGroup) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onFavorite: () -> Unit,
    onUpdate: () -> Unit,
    onClearAndRefetchChinese: () -> Unit,
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
    onSetTtsVoiceProfileId: (String) -> Unit,
    onSetTtsSpeed: (Float) -> Unit,
    onSetTtsPitch: (Float) -> Unit
) {
    val needOnboarding = !uiState.ageLocked || !uiState.complianceAccepted
    if (needOnboarding) {
        OnboardingScreen(onCompleteOnboarding = onCompleteOnboarding)
        return
    }
    val systemLocaleTag = LocalConfiguration.current.locales[0]?.toLanguageTag().orEmpty()
    val uiLanguageTag = resolveUiLanguageTag(systemLocaleTag, uiState.uiLanguageMode, uiState.uiLanguage)
    val isEn = uiLanguageTag.lowercase().startsWith("en")

    LaunchedEffect(uiState.currentJoke?.id, uiState.unplayedCount, needOnboarding) {
        if (!needOnboarding && uiState.currentJoke == null && uiState.unplayedCount > 0) {
            onNext()
        }
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        bottomBar = {
            NavigationBar {
                AppRoute.entries.forEach { page ->
                    val selected = currentDestination?.hierarchy?.any { it.route == page.route } == true
                    val label = when (page) {
                        AppRoute.MAIN -> if (isEn) "Home" else "主页"
                        AppRoute.SOURCES -> if (isEn) "Sources" else "来源"
                        AppRoute.SETTINGS -> if (isEn) "Settings" else "设置"
                    }
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(page.route) {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        },
                        icon = { routeIcon(page, label) },
                        label = null,
                        alwaysShowLabel = false
                    )
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            uiState.message?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            NavHost(
                navController = navController,
                startDestination = AppRoute.MAIN.route,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                composable(AppRoute.MAIN.route) {
                    MainPage(
                        uiState = uiState,
                        uiLanguageTag = uiLanguageTag,
                        onPrev = onPrev,
                        onNext = onNext,
                        onFavorite = onFavorite,
                        onUpdate = onUpdate,
                        onClearAndRefetchChinese = onClearAndRefetchChinese,
                        onResetPlayed = onResetPlayed,
                        onSpeak = onSpeak,
                        onStopSpeak = onStopSpeak,
                        onProcess = onProcess
                    )
                }
                composable(AppRoute.SOURCES.route) {
                    SourcesPage(
                        uiState = uiState,
                        onSetSourceEnabled = onSetSourceEnabled,
                        onDeleteSource = onDeleteSource,
                        onAddUserOnlineSource = onAddUserOnlineSource,
                        onUpdateUserOnlineSource = onUpdateUserOnlineSource,
                        onTestUserOnlineSource = onTestUserOnlineSource,
                        onImportOfflineText = onImportOfflineText,
                        onClearUserSourceData = onClearUserSourceData
                    )
                }
                composable(AppRoute.SETTINGS.route) {
                    SettingsPage(
                        uiState = uiState,
                        onSetUiLanguageMode = onSetUiLanguageMode,
                        onSetUiLanguage = onSetUiLanguage,
                        onSetContentLanguageMode = onSetContentLanguageMode,
                        onSetContentLanguage = onSetContentLanguage,
                        onSetAutoUpdateEnabled = onSetAutoUpdateEnabled,
                        onSetAutoProcessEnabled = onSetAutoProcessEnabled,
                        onSetTtsVoiceProfileId = onSetTtsVoiceProfileId,
                        onSetTtsSpeed = onSetTtsSpeed,
                        onSetTtsPitch = onSetTtsPitch,
                        onResetPlayed = onResetPlayed
                    )
                }
            }
        }
    }
}


