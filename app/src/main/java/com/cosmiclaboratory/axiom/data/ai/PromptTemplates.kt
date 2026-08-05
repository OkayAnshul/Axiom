package com.cosmiclaboratory.axiom.data.ai

internal object PromptTemplates {

    /**
     * The questions the companion greets the user with.
     *
     * [memoryLines] is what makes these worth reading: without it the model can
     * only produce self-help filler ("what are you grateful for?"), which is
     * exactly what the curated pack already covers. With it, the opener can ask
     * about this person's actual life. The instructions push hard against
     * reciting the memory back — a question that quotes your own file at you
     * reads like surveillance, not like a friend.
     */
    fun initiatorPrompts(
        recentSummaries: List<String>,
        memoryLines: List<String>,
        count: Int
    ): String = buildString {
        appendLine("Write $count short questions to open a conversation with someone you know well.")
        appendLine("Each is a single sentence under 18 words. No numbering, no preamble.")
        appendLine()

        if (memoryLines.isNotEmpty()) {
            appendLine("What you know about them:")
            memoryLines.take(MAX_MEMORY_LINES).forEach { appendLine("- ${it.trim()}") }
            appendLine()
            appendLine("Ground most of the questions in the specifics above — their people, their")
            appendLine("goals, what keeps coming up for them. Rules:")
            appendLine("- Never quote a remembered detail back at them; ask from it, not about it.")
            appendLine("- One person or one thread per question. Never stack two.")
            appendLine("- Do not assume how something turned out, or that it is still true.")
            appendLine("- Leave at least one question open and general, for a day none of this fits.")
        } else {
            appendLine("You do not know them yet, so keep these open and gentle.")
        }

        if (recentSummaries.isNotEmpty()) {
            appendLine()
            appendLine("What they wrote about recently:")
            recentSummaries.forEach { appendLine("- ${it.trim()}") }
        }

        // Few-shot beats almost any instruction for this kind of task: the
        // difference between a good opener and a bad one is tone, and tone is
        // far easier to show than to describe.
        appendLine()
        appendLine("Good questions look like these:")
        appendLine("- Did you end up calling your mum back?")
        appendLine("- Is the flat still as loud as it was?")
        appendLine("- What's left on your plate before Friday?")
        appendLine()
        appendLine("Bad ones look like these, and you must not write anything like them:")
        appendLine("- What are you grateful for today?")
        appendLine("- How are you feeling on a scale of one to ten?")
        appendLine("- Reflect on your personal growth this week.")
        appendLine("- I remember you said your sister Riya lives in Pune — how is she?")

        appendLine()
        appendLine("Respond with JSON only, exactly this shape:")
        append("""{"prompts": ["question 1", "question 2", "..."]}""")
    }

    /** Enough to be specific, not so much that the model writes a biography quiz. */
    const val MAX_MEMORY_LINES = 12

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

    const val COMPACTION_SYSTEM = """You maintain a running summary of an ongoing conversation. You reply with the updated summary as plain prose and nothing else — no preamble, no headings, no commentary."""

    /**
     * Folds the part of a live conversation that no longer fits the verbatim
     * window into the running summary.
     *
     * This runs *during* a conversation, not after it. Without it, a long
     * explanation lost its own beginning: messages past the window were dropped
     * outright, and the only summary was written three hours after the user
     * stopped talking.
     */
    fun compactConversation(existingSummary: String, fallingOut: String): String = buildString {
        if (existingSummary.isNotBlank()) {
            appendLine("Summary so far:")
            appendLine(existingSummary.take(SUMMARY_INPUT_CAP))
            appendLine()
        }
        appendLine("Newer part of the same conversation, which is about to scroll out of view:")
        appendLine(fallingOut.takeLast(COMPACTION_INPUT_CAP))
        appendLine()
        append(
            "Rewrite the summary so it also covers this newer part. Keep it under 200 words, " +
                "third person, concrete. Keep specifics that would matter later — names, dates, " +
                "decisions, what they were working through — and drop pleasantries. This is " +
                "working memory for a conversation still in progress, so favour what is " +
                "unresolved over what is settled."
        )
    }

    private const val COMPACTION_INPUT_CAP = 5_000

    const val SESSION_SYSTEM = """You process one conversation between a user and their private journalling companion. You return a single JSON object and nothing else — no prose, no code fences, no commentary."""

    const val ENTRY_SYSTEM = """You read one journal entry and return a single JSON object and nothing else — no prose, no code fences, no commentary. You are reading someone's private writing: be accurate, never flattering, and never invent detail they did not write."""

    /**
     * One call replacing two.
     *
     * The entry used to be sent twice — once to summarize, once to extract
     * memories — over identical text. Merging them also lets the same read
     * produce a title and themes, which is why auto-titling costs nothing here.
     *
     * The old summarize prompt truncated at 1 800 characters, so a long entry
     * (exactly the kind worth summarizing) was summarized from its opening
     * third. Raised to [ENTRY_TEXT_CAP].
     */
    fun entryInsight(
        plainText: String,
        hasTitle: Boolean,
        existingItems: List<Triple<Long, String, String>>
    ): String = buildString {
        appendLine("Journal entry:")
        appendLine(plainText.take(ENTRY_TEXT_CAP))
        appendLine()
        if (existingItems.isNotEmpty()) {
            appendLine("Already remembered about this user (id | kind | text):")
            existingItems.take(MAX_MEMORY_LINES).forEach { (id, kind, text) ->
                appendLine("$id | $kind | $text")
            }
            appendLine()
        }
        appendLine(
            """
            Return exactly this JSON shape:
            {
              "summary": "one or two sentences, in the third person, about what they wrote",
              "follow_up": "one question a close friend would ask next — specific to this entry, never generic",
              "themes": ["one to three short lowercase topic words"],
              "mood": "one lowercase word for the feeling in the writing, or empty string",
              "title": ${if (hasTitle) "\"\"" else "\"a plain 2-5 word title in their own words, or empty string if nothing fits\""},
              "memory": {
                "new": [{"kind": "PERSON|FACT|GOAL|THEME|PREFERENCE|EVENT", "text": "one self-contained sentence under 20 words, third person", "follow_up_in_days": 0}],
                "reinforce": [id],
                "revise": [{"id": id, "text": "corrected sentence"}]
              }
            }

            Only durable things belong in memory. Never duplicate an existing
            memory — reinforce its id. If something CONTRADICTS an existing
            memory, revise that id rather than adding a second belief. Use
            follow_up_in_days only for a real dated thing worth asking about
            later; otherwise 0. Empty arrays are fine.
            """.trimIndent()
        )
    }

    /** Long entries are the ones worth summarizing; 1 800 chars cut them short. */
    private const val ENTRY_TEXT_CAP = 6_000

    /**
     * One call replacing three.
     *
     * The digest worker used to send the same transcript three separate times —
     * once to write the entry, once to extract memories, once to update the
     * rolling summary — which cost triple the input tokens and let the three
     * results disagree with each other about what the conversation was.
     *
     * Doing it in one pass on the strong model costs roughly what the old
     * three-way fan-out cost on the weak one, and the entry, the memories and
     * the summary are now guaranteed to be describing the same thing.
     */
    fun sessionDigest(
        transcript: String,
        existingSummary: String,
        existingItems: List<Triple<Long, String, String>>
    ): String = buildString {
        appendLine("Here is a conversation. The user's turns are marked User.")
        appendLine()
        appendLine(transcript.takeLast(SESSION_TRANSCRIPT_CAP))
        appendLine()
        if (existingSummary.isNotBlank()) {
            appendLine("The running summary of earlier conversations so far:")
            appendLine(existingSummary.take(SUMMARY_INPUT_CAP))
            appendLine()
        }
        if (existingItems.isNotEmpty()) {
            appendLine("Already remembered about this user (id | kind | text):")
            existingItems.take(MAX_MEMORY_LINES).forEach { (id, kind, text) ->
                appendLine("$id | $kind | $text")
            }
            appendLine()
        }
        appendLine(
            """
            Return exactly this JSON shape:
            {
              "entry": "a short first-person journal entry in the USER's own voice, past tense, 2-5 sentences, only what they actually said — never invent detail",
              "mood": "one lowercase word for how they sounded, or empty string if unclear",
              "summary": "the running summary updated with this conversation, under 200 words, third person",
              "memory": {
                "new": [{"kind": "PERSON|FACT|GOAL|THEME|PREFERENCE|EVENT", "text": "one self-contained sentence under 20 words, third person", "follow_up_in_days": 0}],
                "reinforce": [id],
                "revise": [{"id": id, "text": "corrected sentence"}]
              }
            }

            Rules for memory: only durable things worth recalling months from now.
            Never duplicate an existing memory — reinforce its id instead. If a
            new fact CONTRADICTS an existing one (they moved, changed job, ended
            something), emit a revise for that id rather than a new item, so the
            old belief is corrected instead of both being held at once. Use
            follow_up_in_days only for something with a real date the companion
            should ask about afterwards; otherwise 0. Empty arrays are fine.
            """.trimIndent()
        )
    }

    private const val SESSION_TRANSCRIPT_CAP = 6_000
    private const val SUMMARY_INPUT_CAP = 1_200

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
