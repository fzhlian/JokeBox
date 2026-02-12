package com.jokebox.app.domain.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import java.util.Locale

class AndroidTtsEngine(context: Context) : TtsEngine {
    private val tts: TextToSpeech

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
        chooseVoiceByProfile(voiceProfileId)
        tts.setSpeechRate(speed)
        tts.setPitch(pitch)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jokebox-tts")
    }

    override suspend fun stop() {
        tts.stop()
    }

    private fun chooseVoiceByProfile(profileId: String) {
        val voices = tts.voices?.toList().orEmpty()
        if (voices.isEmpty()) return

        val selected = when (profileId) {
            "doubao_xkms_ling",
            "doubao_xkms_yun",
            "doubao_xkms_kira" -> pickVoice(voices, listOf("zh", "female"))
            else -> null
        } ?: pickVoice(voices, listOf("zh", "female"))
            ?: pickVoice(voices, listOf("zh"))

        if (selected != null) {
            tts.voice = selected
            return
        }

        val locale = when {
            profileId.startsWith("doubao_") -> Locale.SIMPLIFIED_CHINESE
            else -> Locale.getDefault()
        }
        tts.language = locale
    }
}

private fun pickVoice(voices: List<Voice>, keywords: List<String>): Voice? {
    val lowerKeywords = keywords.map { it.lowercase() }
    return voices.firstOrNull { voice ->
        val bag = "${voice.name} ${voice.locale} ${voice.features}".lowercase()
        lowerKeywords.all { bag.contains(it) }
    }
}
