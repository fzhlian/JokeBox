package com.jokebox.app.data.model

enum class AgeGroup(val value: Int) {
    TEEN(0),
    YOUTH(1),
    ADULT(2);

    companion object {
        fun fromValue(value: Int): AgeGroup = entries.firstOrNull { it.value == value } ?: ADULT
    }
}

enum class SourceType {
    BUILTIN,
    USER_ONLINE,
    USER_OFFLINE
}

enum class RawStatus {
    PENDING,
    PROCESSING,
    DONE,
    DROPPED,
    FAILED
}

enum class LanguageMode {
    SYSTEM,
    MANUAL
}

data class JokeUiItem(
    val id: String,
    val content: String,
    val language: String,
    val favorite: Boolean
)
