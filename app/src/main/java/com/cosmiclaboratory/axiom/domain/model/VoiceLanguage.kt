package com.cosmiclaboratory.axiom.domain.model

/**
 * Voice-input language preference.
 *
 * On-device options call into Android's `SpeechRecognizer` with a fixed BCP-47 hint.
 * `HINGLISH_AUTO` records audio locally and ships it to Groq's Whisper-large-v3-turbo,
 * which handles code-mixed Hindi/English natively (Android's STT does not).
 */
enum class VoiceLanguage(val displayLabel: String, val bcp47: String?, val onDevice: Boolean) {
    ENGLISH_US("English (US)", "en-US", true),
    ENGLISH_IN("English (India)", "en-IN", true),
    HINDI("हिंदी", "hi-IN", true),
    HINGLISH_AUTO("Hinglish (best accuracy, uses internet)", null, false);

    companion object {
        fun fromStorage(raw: String?): VoiceLanguage =
            runCatching { valueOf(raw ?: ENGLISH_IN.name) }.getOrDefault(ENGLISH_IN)
    }
}
