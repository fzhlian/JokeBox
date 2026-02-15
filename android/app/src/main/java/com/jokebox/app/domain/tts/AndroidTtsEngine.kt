package fzhlian.jokebox.app.domain.tts

import android.content.Context
import android.media.MediaPlayer
import android.util.Base64
import android.util.Log
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import fzhlian.jokebox.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

class AndroidTtsEngine(context: Context) : TtsEngine {
    private val appContext = context.applicationContext
    private val tts: TextToSpeech
    private val httpClient = OkHttpClient.Builder()
        .callTimeout(20, TimeUnit.SECONDS)
        .build()
    private var mediaPlayer: MediaPlayer? = null

    init {
        tts = TextToSpeech(context) {}
        tts.language = Locale.getDefault()
    }

    override suspend fun listVoices(): List<String> = tts.voices?.map { it.name } ?: emptyList()

    override suspend fun setVoice(voiceId: String) {
        val target = tts.voices?.firstOrNull { it.name == voiceId } ?: return
        tts.voice = target
    }

    override suspend fun speak(text: String, speed: Float, pitch: Float, voiceProfileId: String) {
        val configuredVoiceId = BuildConfig.VOLCENGINE_TTS_VOICE_ID
        val resolvedProfileId = when {
            configuredVoiceId.isBlank() -> voiceProfileId
            voiceProfileId == CUTE_GIRL_VOICE_ID -> configuredVoiceId
            else -> voiceProfileId
        }
        Log.i(TAG, "Speak profile=$resolvedProfileId textLen=${text.length}")
        tts.stop()
        stopMediaPlayback()
        if (shouldUseVolcengine(resolvedProfileId) && speakViaVolcengine(text, speed, pitch, resolvedProfileId)) {
            return
        }
        chooseVoiceByProfile(resolvedProfileId)
        tts.setSpeechRate(speed)
        tts.setPitch(pitch)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jokebox-tts")
    }

    override suspend fun stop() {
        tts.stop()
        stopMediaPlayback()
    }

    private suspend fun speakViaVolcengine(
        text: String,
        speed: Float,
        pitch: Float,
        voiceProfileId: String
    ): Boolean = withContext(Dispatchers.IO) {
        val appId = BuildConfig.VOLCENGINE_TTS_APP_ID.ifBlank {
            BuildConfig.VOLCENGINE_TTS_APPKEY
        }.ifBlank {
            BuildConfig.VOLCENGINE_TTS_API_KEY
        }
        val token = BuildConfig.VOLCENGINE_TTS_TOKEN.ifBlank {
            BuildConfig.VOLCENGINE_TTS_API_KEY
        }
        if (appId.isBlank() || token.isBlank()) {
            Log.w(TAG, "Volcengine TTS skipped: appId/token missing.")
            return@withContext false
        }

        val payload = JSONObject().apply {
            put("app", JSONObject().apply {
                put("appid", appId)
                put("token", token)
                put("cluster", "volcano_tts")
            })
            put("user", JSONObject().apply {
                put("uid", "jokebox-${UUID.randomUUID()}")
            })
            put("audio", JSONObject().apply {
                put("voice_type", voiceProfileId)
                put("encoding", "mp3")
                put("speed_ratio", speed.coerceIn(0.5f, 2.0f))
                put("pitch_ratio", pitch.coerceIn(0.5f, 2.0f))
            })
            put("request", JSONObject().apply {
                put("reqid", UUID.randomUUID().toString())
                put("text", text.take(MAX_TEXT_LEN))
                put("text_type", "plain")
                put("operation", "query")
            })
        }

        val request = Request.Builder()
            .url(VOLCENGINE_TTS_URL)
            .addHeader("Authorization", "Bearer;$token")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        runCatching {
            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Log.w(TAG, "Volcengine TTS http=${response.code}, body=${body.take(300)}")
                    return@use false
                }
                val json = JSONObject(body)
                val code = json.optInt("code", -1)
                if (code != 3000) {
                    Log.w(TAG, "Volcengine TTS rejected: code=$code, body=${body.take(300)}")
                    return@use false
                }
                val audioBase64 = json.optString("data", "")
                if (audioBase64.isBlank()) {
                    Log.w(TAG, "Volcengine TTS no audio data.")
                    return@use false
                }
                Log.i(TAG, "Volcengine TTS success, audio bytes(base64)=${audioBase64.length}")
                playDecodedMp3(audioBase64)
                true
            }
        }.getOrElse { ex ->
            Log.w(TAG, "Volcengine TTS failed: ${ex.message}", ex)
            false
        }
    }

    private fun playDecodedMp3(base64Audio: String) {
        val data = Base64.decode(base64Audio, Base64.DEFAULT)
        val cacheFile = File.createTempFile("jokebox_tts_", ".mp3", appContext.cacheDir)
        cacheFile.writeBytes(data)

        val player = MediaPlayer().apply {
            setDataSource(cacheFile.absolutePath)
            setOnCompletionListener { mp ->
                mp.release()
                cacheFile.delete()
            }
            setOnErrorListener { mp, _, _ ->
                mp.release()
                cacheFile.delete()
                true
            }
            prepare()
            start()
        }
        mediaPlayer = player
    }

    private fun stopMediaPlayback() {
        mediaPlayer?.let { player ->
            runCatching {
                if (player.isPlaying) player.stop()
                player.release()
            }
        }
        mediaPlayer = null
    }

    private fun chooseVoiceByProfile(profileId: String) {
        val voices = tts.voices?.toList().orEmpty()
        if (voices.isEmpty()) return

        val selected = when (profileId) {
            CUTE_GIRL_VOICE_ID -> pickVoice(voices, listOf("zh", "female"))
            else -> null
        } ?: pickVoice(voices, listOf("zh", "female"))
            ?: pickVoice(voices, listOf("zh"))

        if (selected != null) {
            tts.voice = selected
            return
        }

        val locale = when {
            profileId == CUTE_GIRL_VOICE_ID -> Locale.SIMPLIFIED_CHINESE
            else -> Locale.getDefault()
        }
        tts.language = locale
    }

    companion object {
        private const val TAG = "JokeBoxTTS"
        private const val VOLCENGINE_TTS_URL = "https://openspeech.bytedance.com/api/v1/tts"
        private const val MAX_TEXT_LEN = 300
        private const val CUTE_GIRL_VOICE_ID = "ICL_zh_female_keainvsheng_tob"
    }

    private fun shouldUseVolcengine(profileId: String): Boolean {
        if (profileId.isBlank() || profileId == "default") return false
        if (profileId == BuildConfig.VOLCENGINE_TTS_VOICE_ID) return true
        return profileId.startsWith("ICL_") || profileId.startsWith("zh_")
    }
}

private fun pickVoice(voices: List<Voice>, keywords: List<String>): Voice? {
    val lowerKeywords = keywords.map { it.lowercase() }
    return voices.firstOrNull { voice ->
        val bag = "${voice.name} ${voice.locale} ${voice.features}".lowercase()
        lowerKeywords.all { bag.contains(it) }
    }
}


