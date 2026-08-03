package com.cosmiclaboratory.axiom.data.companion

import com.cosmiclaboratory.axiom.domain.model.MemoryItem
import com.cosmiclaboratory.axiom.domain.model.MemoryKind
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

/**
 * Assembles the companion's system prompt and the verbatim history window.
 * Pure — no repositories, no clock, no I/O — so every budget and section rule
 * is unit-testable. Sections are omitted entirely when empty rather than sent
 * as blank headers; each is char-capped deterministically so the full prompt
 * stays inside Groq's free-tier token budget regardless of how much the user
 * has written.
 */
class CompanionPromptBuilder @Inject constructor() {

    data class Excerpt(val date: LocalDate, val mood: Int?, val text: String)

    data class Context(
        val displayName: String,
        val personaFragment: String,
        val memories: Map<MemoryKind, List<MemoryItem>>,
        val rollingSummary: String,
        val recentInsights: List<String>,
        val excerpts: List<Excerpt>,
        val now: LocalDateTime,
        val moodToday: Int?,
        val streakDays: Int
    )

    fun buildSystemPrompt(ctx: Context): String = buildString {
        val name = ctx.displayName.trim()
        if (name.isNotEmpty()) {
            append("You are $name's companion inside Axiom, their private journal. ")
        } else {
            append("You are the user's companion inside Axiom, their private journal. ")
        }
        appendLine("You are a trusted friend, not an assistant and not a therapist.")
        appendLine()
        appendLine("How you talk:")
        appendLine("- Warm, specific, and brief — usually one to four sentences. Match their energy and length.")
        appendLine(
            "- Plain, natural language. Never use canned phrases: \"Thank you for sharing\", " +
                "\"How does that make you feel?\", \"I'm here for you if you need anything\", " +
                "\"It sounds like\", or anything that describes yourself as an AI."
        )
        appendLine("- Weave in what you remember the way a friend would; never recite lists or dates mechanically.")
        appendLine(
            "- At most one question per reply, and only when it genuinely helps. Often the right move " +
                "is to simply acknowledge, or sit with them. No advice unless they ask or clearly want it."
        )
        appendLine("- If they write in Hindi or Hinglish, mirror them.")

        if (ctx.personaFragment.isNotBlank()) {
            appendLine()
            appendLine(ctx.personaFragment.trim())
        }

        val memorySection = renderMemories(ctx.memories)
        if (memorySection.isNotEmpty()) {
            appendLine()
            appendLine("What you remember about them (weave in only when relevant, never recite):")
            append(memorySection)
        }

        if (ctx.rollingSummary.isNotBlank()) {
            appendLine()
            appendLine("Where your conversations have been lately:")
            appendLine(ctx.rollingSummary.trim().take(SUMMARY_CAP))
        }

        val insights = ctx.recentInsights.filter { it.isNotBlank() }.take(MAX_INSIGHTS)
        val excerpts = ctx.excerpts.filter { it.text.isNotBlank() }.take(MAX_EXCERPTS)
        if (insights.isNotEmpty() || excerpts.isNotEmpty()) {
            appendLine()
            appendLine("From their recent journal (context, not a script):")
            insights.forEach { appendLine("- ${it.trim().take(INSIGHT_CAP)}") }
            excerpts.forEach { excerpt ->
                val mood = excerpt.mood?.let { " · mood $it/5" } ?: ""
                appendLine("[${excerpt.date}$mood] ${excerpt.text.trim().take(EXCERPT_CAP)}")
            }
        }

        appendLine()
        val weekday = ctx.now.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        val moodLine = ctx.moodToday?.let { "$it/5" } ?: "not recorded"
        appendLine("Right now it is $weekday ${timeOfDay(ctx.now.hour)}. Mood today: $moodLine.")
        if (ctx.streakDays > 0) {
            appendLine("Writing streak: ${ctx.streakDays} days — mention only if it comes up naturally.")
        }
        appendLine()
        append(
            "Everything here is private. Never claim you would share it, and if asked what you " +
                "remember, answer honestly and point them to the \"What I remember\" screen."
        )
    }

    /**
     * Trims the raw thread history to the verbatim window sent to the model:
     * newest [maxMessages], then oldest-first eviction until [charBudget] fits.
     * The newest message is never evicted — a lone oversize message is truncated
     * from the front instead, keeping its most recent tail.
     */
    fun windowHistory(
        messages: List<Pair<String, String>>,
        maxMessages: Int = MAX_HISTORY_MESSAGES,
        charBudget: Int = HISTORY_CHAR_BUDGET
    ): List<Pair<String, String>> {
        var window = messages.takeLast(maxMessages)
        while (window.size > 1 && window.sumOf { it.second.length } > charBudget) {
            window = window.drop(1)
        }
        if (window.size == 1 && window[0].second.length > charBudget) {
            val (role, content) = window[0]
            return listOf(role to content.takeLast(charBudget))
        }
        return window
    }

    private fun renderMemories(memories: Map<MemoryKind, List<MemoryItem>>): String = buildString {
        KIND_ORDER.forEach { kind ->
            val items = memories[kind].orEmpty()
            if (items.isEmpty()) return@forEach
            appendLine("${KIND_LABELS.getValue(kind)}:")
            items.forEach { appendLine("- ${it.text.trim().take(MEMORY_ITEM_CAP)}") }
        }
    }

    private fun timeOfDay(hour: Int): String = when (hour) {
        in 5..11 -> "morning"
        in 12..16 -> "afternoon"
        in 17..21 -> "evening"
        else -> "night"
    }

    companion object {
        const val MAX_HISTORY_MESSAGES = 12
        const val HISTORY_CHAR_BUDGET = 6_000
        const val MAX_EXCERPTS = 4
        const val EXCERPT_CAP = 600
        const val MAX_INSIGHTS = 3
        const val INSIGHT_CAP = 200
        const val SUMMARY_CAP = 1_200
        const val MEMORY_ITEM_CAP = 120

        private val KIND_ORDER = listOf(
            MemoryKind.PERSON,
            MemoryKind.GOAL,
            MemoryKind.THEME,
            MemoryKind.FACT,
            MemoryKind.EVENT,
            MemoryKind.PREFERENCE
        )

        private val KIND_LABELS = mapOf(
            MemoryKind.PERSON to "People",
            MemoryKind.GOAL to "Goals",
            MemoryKind.THEME to "Recurring themes",
            MemoryKind.FACT to "Facts",
            MemoryKind.EVENT to "Recent events",
            MemoryKind.PREFERENCE to "Preferences"
        )
    }
}
