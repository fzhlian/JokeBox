package com.jokebox.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class SanitizeForSpeechTest {
    @Test
    fun stripsAnonymousTail() {
        val input = "A short joke line.\nanonymous"
        val result = sanitizeForSpeech(input)
        assertEquals("A short joke line.", result)
    }

    @Test
    fun stripsStandaloneAuthorNameAtTail() {
        val input = "This is a longer joke sentence for testing.\nLu Xun"
        val result = sanitizeForSpeech(input)
        assertEquals("This is a longer joke sentence for testing.", result)
    }

    @Test
    fun stripsAuthorPrefixLineAtTail() {
        val input = "Another joke sentence for testing.\nAuthor: Wang Xiaoming"
        val result = sanitizeForSpeech(input)
        assertEquals("Another joke sentence for testing.", result)
    }

    @Test
    fun keepsNormalShortEnding() {
        val input = "Why write it this way?\nBecause it is stable."
        val result = sanitizeForSpeech(input)
        assertEquals(input, result)
    }
}
