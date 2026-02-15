package fzhlian.jokebox.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import fzhlian.jokebox.app.data.model.AgeGroup
import fzhlian.jokebox.app.data.model.LanguageMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DebugActionsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REFETCH_ZH) {
            return
        }

        val app = context.applicationContext as? JokeBoxApp ?: return
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                app.container.settingsStore.setContentLanguageMode(LanguageMode.MANUAL)
                app.container.settingsStore.setContentLanguage("zh-Hans")
                app.container.sourceRepository.ensureBuiltinSourcesLoaded()
                val sources = app.container.sourceRepository.observeSources().first()
                val cfgSummary = sources.joinToString { source ->
                    "${source.sourceId}:${if (source.configJson.isNullOrBlank()) "null" else "ok"}"
                }
                Log.i(
                    TAG,
                    "debug_refetch_zh sources=${sources.size} enabled=${sources.count { it.enabled }} " +
                        "cfg=$cfgSummary"
                )
                val fetchable = app.container.sourceRepository.enabledFetchableSources()
                Log.i(TAG, "debug_refetch_zh fetchable=${fetchable.size} ids=${fetchable.joinToString { it.sourceId }}")

                val fetchResult = app.container.fetcher.fetchOnce(limit = 20).getOrThrow()
                val age = app.container.settingsStore.getAgeGroup() ?: AgeGroup.ADULT
                var processResult = app.container.processor.processBatch(
                    targetAgeGroup = age,
                    defaultLanguage = "zh-Hans",
                    nearThreshold = app.container.settingsStore.getNearDedupThreshold()
                ).getOrThrow()
                Log.i(TAG, "debug_refetch_zh first pass: fetched=$fetchResult processed=$processResult")

                if (fetchResult == 0 && processResult == 0) {
                    Log.i(TAG, "debug_refetch_zh fallback import start")
                    val fallbackContent = buildFallbackZhJokes().joinToString(separator = "\n")
                    app.container.importer.importText(
                        sourceId = "debug-fallback-zh",
                        text = fallbackContent,
                        language = "zh-Hans",
                        format = "txt"
                    )
                    processResult = app.container.processor.processBatch(
                        targetAgeGroup = age,
                        defaultLanguage = "zh-Hans",
                        nearThreshold = app.container.settingsStore.getNearDedupThreshold()
                    ).getOrThrow()
                    Log.i(TAG, "debug_refetch_zh fallback import done: processed=$processResult")
                }
                Log.i(TAG, "debug_refetch_zh done: fetched=$fetchResult processed=$processResult age=$age")
            }.onFailure { err ->
                Log.e(TAG, "debug_refetch_zh failed", err)
            }
        }
    }

    companion object {
        const val ACTION_REFETCH_ZH = "fzhlian.jokebox.app.DEBUG_REFETCH_ZH"
        private const val TAG = "JokeBoxDebug"
    }
}

private fun buildFallbackZhJokes(): List<String> = listOf(
    "程序员去相亲，对方问会做饭吗？他说：会，煮异常最拿手。",
    "我问 AI 你会讲笑话吗？它说会，然后给我输出了一个 bug。",
    "同事说代码要优雅，我把 if 写成了诗，他让我重构。",
    "产品说这个需求很简单，我的 CPU 当场进入省电模式。",
    "老板问进度如何，我说已经 80%，剩下 80% 明天完成。",
    "测试提了 100 个问题，我说这证明系统覆盖率很高。",
    "我把注释删了，代码跑得更快了，因为没人敢改了。",
    "数据库说我很稳定，只是偶尔在周五晚上情绪化。",
    "今天修了一个线上 bug，奖励是再给我一个线上 bug。",
    "代码评审时我写了 todo，领导说这就是长期规划。"
)


