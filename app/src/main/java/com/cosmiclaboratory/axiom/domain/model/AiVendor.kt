package com.cosmiclaboratory.axiom.domain.model

/**
 * Who the user's AI features talk to.
 *
 * Both have a free tier that a personal journal will not exhaust, which is the
 * bar for being offered here at all — a companion that stops answering when a
 * trial ends is worse than one that never spoke.
 */
enum class AiVendor(
    val displayName: String,
    val keyUrl: String,
    val keyHint: String,
    val summary: String
) {
    GROQ(
        displayName = "Groq",
        keyUrl = "https://console.groq.com/keys",
        keyHint = "gsk_…",
        summary = "Fast, and the only one here that can transcribe Hinglish voice notes."
    ),
    GEMINI(
        displayName = "Google Gemini",
        keyUrl = "https://aistudio.google.com/apikey",
        keyHint = "AIza…",
        summary = "Stronger with Hindi and Hinglish writing. No voice transcription."
    );

    companion object {
        fun fromStorage(raw: String?): AiVendor =
            runCatching { valueOf(raw ?: GROQ.name) }.getOrDefault(GROQ)
    }
}
