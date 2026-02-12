package com.jokebox.app.domain.policy

import java.security.MessageDigest

object TextNormalizer {
    private val zeroWidthRegex = Regex("[\\u200B-\\u200D\\uFEFF]")
    private val whitespaceRegex = Regex("\\s+")

    fun normalize(input: String): String {
        return input
            .trim()
            .lowercase()
            .replace('\u3000', ' ')
            .replace(zeroWidthRegex, "")
            .replace(whitespaceRegex, " ")
    }
}

object Hashing {
    fun sha256Hex(input: String, takeChars: Int = 32): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        val hex = digest.joinToString("") { "%02x".format(it) }
        return hex.take(takeChars.coerceIn(16, 64))
    }
}

object SimHash64 {
    fun compute(contentNorm: String): String {
        val grams = if (contentNorm.contains(' ')) {
            contentNorm.split(' ').filter { it.isNotBlank() }
        } else {
            contentNorm.windowed(size = 3, step = 1, partialWindows = true).filter { it.isNotBlank() }
        }
        val bits = IntArray(64)
        grams.forEach { token ->
            val digest = MessageDigest.getInstance("SHA-256").digest(token.toByteArray())
            for (i in 0 until 64) {
                val byteIdx = i / 8
                val bitIdx = i % 8
                val isSet = (digest[byteIdx].toInt() shr bitIdx) and 1 == 1
                bits[i] += if (isSet) 1 else -1
            }
        }
        var result = 0UL
        for (i in bits.indices) {
            if (bits[i] >= 0) {
                result = result or (1UL shl i)
            }
        }
        return result.toString(16).padStart(16, '0')
    }

    fun hammingDistance(hexA: String, hexB: String): Int {
        val a = java.lang.Long.parseUnsignedLong(hexA, 16).toULong()
        val b = java.lang.Long.parseUnsignedLong(hexB, 16).toULong()
        return (a xor b).countOneBits()
    }

    fun bucket(simHash: String, prefixLen: Int = 4): String = simHash.take(prefixLen.coerceAtLeast(1))
}

object ContentPolicy {
    private val strictBanWords = setOf("hate", "terror", "kill", "毒品", "仇恨", "极端")
    private val youthExtraBanWords = setOf("sex", "nsfw", "暴力", "色情")

    fun allow(contentNorm: String, ageGroup: Int): Boolean {
        val hardRejected = strictBanWords.any { contentNorm.contains(it) }
        if (hardRejected) return false

        if (ageGroup <= 1) {
            val youthRejected = youthExtraBanWords.any { contentNorm.contains(it) }
            if (youthRejected) return false
            if (contentNorm.length < 8) return false
        }

        return true
    }
}
