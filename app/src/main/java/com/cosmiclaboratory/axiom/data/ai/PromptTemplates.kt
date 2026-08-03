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

    const val MEMORY_EXTRACTION_SYSTEM = """You maintain the long-term memory of a private journaling companion. From the text you are given, extract only durable facts worth remembering months from now: important people and relationships (PERSON), stable facts about the user's life (FACT), goals and ambitions (GOAL), recurring struggles or topics (THEME), how they like to be talked to (PREFERENCE), and significant one-time events (EVENT). Ignore small talk, transient moods, and anything already covered by an existing memory — reference existing memories by id instead of duplicating them. Each memory text must be one self-contained sentence under 20 words, in third person about the user."""

    fun memoryExtraction(text: String, existingItems: List<Triple<Long, String, String>>): String {
        val existing = if (existingItems.isEmpty()) {
            "(no existing memories yet)"
        } else {
            existingItems.joinToString("\n") { (id, kind, memoryText) -> """{"id": $id, "kind": "$kind", "text": "${memoryText.replace("\"", "'")}"}""" }
        }
        val truncated = if (text.length > 5000) text.take(5000) + "…" else text
        return """
            Existing memories:
            $existing

            Text to extract from:
            ---
            $truncated
            ---
            Respond with JSON only, exactly this shape (empty arrays are fine):
            {"new": [{"kind": "PERSON|FACT|GOAL|THEME|PREFERENCE|EVENT", "text": "...", "confidence": 0.0}],
             "reinforce": [existing ids seen again in this text],
             "revise": [{"id": existing id, "text": "updated wording"}]}
        """.trimIndent()
    }

    fun conversationDigest(transcript: String): String {
        val truncated = if (transcript.length > 5000) transcript.takeLast(5000) else transcript
        return """
            Below is a conversation between a user and their journaling companion.
            Write a short journal entry (3-6 sentences) in the user's own first-person voice,
            capturing what they talked about, what happened, and how they felt.
            Write only the entry itself: no headings, no preamble, no mention of the companion or the conversation.
            Then on the last line, alone, write: MOOD: followed by one word for the user's overall mood.
            ---
            $truncated
            ---
        """.trimIndent()
    }

    fun rollingSummary(existingSummary: String, transcript: String): String {
        val existing = existingSummary.ifBlank { "(none yet)" }
        val truncated = if (transcript.length > 4000) transcript.takeLast(4000) else transcript
        return """
            You keep a running summary of everything a user and their companion have talked about.
            Fold the new messages below into the running summary. Keep it under 200 words.
            Keep emotionally significant facts, open threads, and anything the user might expect
            a friend to remember. Drop pleasantries. Write plain prose, no headings.

            Running summary so far:
            $existing

            New messages:
            ---
            $truncated
            ---
        """.trimIndent()
    }
}
