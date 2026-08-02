package com.cosmiclaboratory.axiom.data.ai

internal object PromptTemplates {

    fun initiatorPrompts(recentSummaries: List<String>, count: Int): String {
        val context = if (recentSummaries.isEmpty()) {
            "The user has no past entries yet."
        } else {
            "Recent entry themes:\n" + recentSummaries.joinToString("\n") { "- $it" }
        }
        return """
            Generate $count short, gentle journaling questions to help the user start writing.
            Each question must be a single sentence under 18 words. No numbering. No preamble.
            $context
            Respond with JSON only, exactly this shape:
            {"prompts": ["question 1", "question 2", "..."]}
        """.trimIndent()
    }

    fun summarizeEntry(plainText: String): String {
        val truncated = if (plainText.length > 1800) plainText.take(1800) + "…" else plainText
        return """
            Summarize this journal entry in 1-2 sentences, suggest one short follow-up question
            (under 16 words), tag 1-3 themes, and one mood word.
            Entry:
            ---
            $truncated
            ---
            Respond with JSON only, exactly this shape:
            {"summary": "...", "follow_up": "...", "themes": ["..."], "mood": "..."}
        """.trimIndent()
    }
}
