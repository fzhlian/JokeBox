package com.jokebox.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class SanitizeForSpeechTest {
    @Test
    fun sanitizeForSpeech_shouldDropAnonymousTail() {
        val input = "今天讲个笑话。\n佚名"
        val output = sanitizeForSpeech(input)
        assertEquals("今天讲个笑话。", output)
    }

    @Test
    fun sanitizeForSpeech_shouldDropLikelyAuthorNameTail() {
        val input = "程序员说：这个需求很简单，先加个 if。\n鲁迅"
        val output = sanitizeForSpeech(input)
        assertEquals("程序员说：这个需求很简单，先加个 if。", output)
    }

    @Test
    fun sanitizeForSpeech_shouldKeepNormalShortEnding() {
        val input = "你问我为什么这么写？\n因为这样更稳。"
        val output = sanitizeForSpeech(input)
        assertEquals(input, output)
    }
}
