package com.jokebox.app.domain.tts

interface TtsEngine {
    suspend fun listVoices(): List<String>
    suspend fun setVoice(voiceId: String)
    suspend fun speak(text: String, speed: Float, pitch: Float)
    suspend fun stop()
}
