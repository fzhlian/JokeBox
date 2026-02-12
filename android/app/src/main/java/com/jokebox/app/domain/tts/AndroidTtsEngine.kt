package com.jokebox.app.domain.tts

import android.content.Context
import android.speech.tts.TextToSpeech
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

    override suspend fun speak(text: String, speed: Float, pitch: Float) {
        tts.setSpeechRate(speed)
        tts.setPitch(pitch)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jokebox-tts")
    }

    override suspend fun stop() {
        tts.stop()
    }
}
