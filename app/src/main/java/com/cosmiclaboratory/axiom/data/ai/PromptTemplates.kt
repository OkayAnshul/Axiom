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

    const val MEMORY_EXTRACTION_SYSTEM = """You maintain the long-term memory of a private journaling companion. From the text you are given, extract only durable facts worth remembering months from now: important people and relationships (PERSON), stable facts about the user's life (FACT), goals and ambitions (GOAL), recurring struggles or topics (THEME), how they like to be talked to (PREFERENCE), and significant one-time events (EVENT). Ignore small talk, transient moods, and anything already covered by an existing memory — reference existing memories by id instead of duplicating them. Each memory text must be one self-contained sentence under 20 words, in third person about the user.

When something is coming up that a good friend would circle back on — an interview, a scan, a hard conversation, a deadline — set follow_up_in_days to how many days from today that friend would naturally ask how it went. Use it sparingly: only when there is a real outcome to ask about, never for routine or recurring things.

Pay particular attention to PREFERENCE: anything the user says about how they want to be talked to. "Stop giving me advice", "just listen", "keep it short", "don't be so cheerful", "ask me more questions", "don't call me buddy". These are instructions about your own behaviour, they are easy to miss among the content, and getting them wrong is what makes a companion feel like a stranger. Record them in the imperative, as a rule you will follow."""

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
            {"new": [{"kind": "PERSON|FACT|GOAL|THEME|PREFERENCE|EVENT", "text": "...", "confidence": 0.0, "follow_up_in_days": null}],
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

    const val PROACTIVE_SYSTEM = """You are the user's companion in their private journal — a trusted friend, not an assistant. You are writing a single short message to open a conversation, the way a friend texts first. Warm, specific, unhurried. Never use canned phrases ("Just checking in!", "Don't forget to journal today", "How does that make you feel?"), never mention streaks as targets, never guilt them for not writing, never describe yourself as an AI. Write only the message itself, no quotes and no preamble."""

    /**
     * The daily check-in. [openLoops] are things the companion promised itself
     * it would ask about; when one exists it should lead, because that is what
     * makes the companion feel like it was actually listening.
     */
    fun dailyCheckIn(
        displayName: String,
        timeOfDay: String,
        openLoops: List<String>,
        recentThemes: List<String>,
        daysSinceLastEntry: Int?
    ): String = buildString {
        appendLine("Write one message of at most 25 words to start a conversation.")
        appendLine()
        if (displayName.isNotBlank()) appendLine("Their name: $displayName")
        appendLine("It is $timeOfDay.")
        if (openLoops.isNotEmpty()) {
            appendLine()
            appendLine("Ask about ONE of these — they came up earlier and you said you'd circle back:")
            openLoops.forEach { appendLine("- $it") }
        }
        if (recentThemes.isNotEmpty()) {
            appendLine()
            appendLine("Recently on their mind (context only, do not list back):")
            recentThemes.forEach { appendLine("- $it") }
        }
        if (daysSinceLastEntry != null && daysSinceLastEntry > 2) {
            appendLine()
            appendLine("You haven't heard from them in $daysSinceLastEntry days. Be glad to hear from them, not disappointed.")
        }
    }

    /** The Sunday week-in-review — a friend noticing, not a dashboard reporting. */
    fun weeklyRecap(
        displayName: String,
        entryCount: Int,
        moodNote: String,
        highlights: List<String>
    ): String = buildString {
        appendLine("Write a warm 2-4 sentence look back on their week. No lists, no statistics, no headings.")
        appendLine("Notice something real, and end with one gentle question about the week ahead.")
        appendLine()
        if (displayName.isNotBlank()) appendLine("Their name: $displayName")
        appendLine("They wrote $entryCount ${if (entryCount == 1) "time" else "times"} this week.")
        appendLine("Mood through the week: $moodNote")
        if (highlights.isNotEmpty()) {
            appendLine()
            appendLine("What they wrote about:")
            highlights.forEach { appendLine("- $it") }
        }
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
