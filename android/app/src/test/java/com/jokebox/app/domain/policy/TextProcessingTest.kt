package com.jokebox.app.domain.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextProcessingTest {
    @Test
    fun normalize_shouldTrimCollapseAndLowercase() {
        val output = TextNormalizer.normalize("  A\u3000B  C\u200B ")
        assertEquals("a b c", output)
    }

    @Test
    fun contentPolicy_shouldRejectYouthUnsafeContent() {
        assertFalse(ContentPolicy.allow("this contains nsfw token", ageGroup = 1))
    }

    @Test
    fun contentPolicy_shouldAllowSafeAdultContent() {
        assertTrue(ContentPolicy.allow("just a normal short joke", ageGroup = 2))
    }

    @Test
    fun simHashHammingDistance_shouldBeStable() {
        val a = SimHash64.compute("hello world")
        val b = SimHash64.compute("hello world!")
        assertTrue(SimHash64.hammingDistance(a, b) < 20)
    }
}
