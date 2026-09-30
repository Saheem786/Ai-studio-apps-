package com.example.audio

import java.util.Locale

/**
 * Supported spoken and recognition languages for Sam AI / Arushi voice assistant.
 * Dynamically configures Android's SpeechRecognizer and TextToSpeech engine.
 */
enum class AssistantLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val flag: String,
    val speechLocale: String,
    val ttsLanguage: String,
    val ttsCountry: String,
    val additionalLocales: List<String>
) {
    HINGLISH(
        code = "hinglish",
        displayName = "Hinglish",
        nativeName = "Hindi + English",
        flag = "🇮🇳",
        speechLocale = "hi-IN",
        ttsLanguage = "hi",
        ttsCountry = "IN",
        additionalLocales = listOf("en-IN", "en-US", "hi")
    ),
    ENGLISH_IN(
        code = "en_in",
        displayName = "English (India)",
        nativeName = "Indian English",
        flag = "🇮🇳",
        speechLocale = "en-IN",
        ttsLanguage = "en",
        ttsCountry = "IN",
        additionalLocales = listOf("hi-IN", "en-US")
    ),
    ENGLISH_US(
        code = "en_us",
        displayName = "English (US)",
        nativeName = "US English",
        flag = "🇺🇸",
        speechLocale = "en-US",
        ttsLanguage = "en",
        ttsCountry = "US",
        additionalLocales = listOf("en-GB", "en-IN")
    ),
    HINDI(
        code = "hi_in",
        displayName = "हिन्दी",
        nativeName = "Hindi",
        flag = "🇮🇳",
        speechLocale = "hi-IN",
        ttsLanguage = "hi",
        ttsCountry = "IN",
        additionalLocales = listOf("hi")
    ),
    SPANISH(
        code = "es_es",
        displayName = "Español",
        nativeName = "Spanish",
        flag = "🇪🇸",
        speechLocale = "es-ES",
        ttsLanguage = "es",
        ttsCountry = "ES",
        additionalLocales = listOf("es-US", "es-MX")
    ),
    FRENCH(
        code = "fr_fr",
        displayName = "Français",
        nativeName = "French",
        flag = "🇫🇷",
        speechLocale = "fr-FR",
        ttsLanguage = "fr",
        ttsCountry = "FR",
        additionalLocales = listOf("fr-CA")
    ),
    GERMAN(
        code = "de_de",
        displayName = "Deutsch",
        nativeName = "German",
        flag = "🇩🇪",
        speechLocale = "de-DE",
        ttsLanguage = "de",
        ttsCountry = "DE",
        additionalLocales = listOf("de-AT", "de-CH")
    ),
    JAPANESE(
        code = "ja_jp",
        displayName = "日本語",
        nativeName = "Japanese",
        flag = "🇯🇵",
        speechLocale = "ja-JP",
        ttsLanguage = "ja",
        ttsCountry = "JP",
        additionalLocales = listOf("ja")
    );

    val ttsLocale: Locale
        get() = Locale.forLanguageTag(speechLocale)

    companion object {
        fun fromCode(code: String?): AssistantLanguage {
            return values().firstOrNull { it.code.equals(code, ignoreCase = true) } ?: HINGLISH
        }
    }
}
