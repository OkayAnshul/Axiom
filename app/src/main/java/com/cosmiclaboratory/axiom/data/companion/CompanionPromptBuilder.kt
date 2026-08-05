package com.cosmiclaboratory.axiom.data.companion

import com.cosmiclaboratory.axiom.domain.model.Emotion
import com.cosmiclaboratory.axiom.domain.model.MemoryItem
import com.cosmiclaboratory.axiom.domain.safety.CareLevel
import com.cosmiclaboratory.axiom.domain.voice.Register
import com.cosmiclaboratory.axiom.domain.voice.VoiceProfile
import com.cosmiclaboratory.axiom.domain.model.MemoryKind
import com.cosmiclaboratory.axiom.domain.style.ReplyLength
import com.cosmiclaboratory.axiom.domain.style.StyleProfile
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
        /** How the companion talks. Replaces the old one-sentence persona. */
        val voice: VoiceProfile = VoiceProfile(),
        val memories: Map<MemoryKind, List<MemoryItem>>,
        val rollingSummary: String,
        val recentInsights: List<String>,
        val excerpts: List<Excerpt>,
        val now: LocalDateTime,
        val moodToday: Int?,
        val streakDays: Int,
        /** How to talk to this person — measured from their writing, plus what they've said. */
        val style: StyleProfile = StyleProfile(),
        /** Named feeling read from today's writing, when the user chose nothing. */
        val emotionToday: Emotion? = null,
        /** At most one pattern, phrased as the finder already phrased it. */
        val noticed: String? = null,
        /** Set when this turn plainly disclosed distress. */
        val care: CareLevel? = null
    )

    /**
     * How to answer someone who has just said something painful.
     *
     * Placed first and last in the prompt so it survives whatever else is in
     * context. It tells the model what NOT to do as much as what to do: the
     * failure mode here is not silence, it is a brisk pivot to a hotline number
     * that reads as being handed off rather than heard. The app shows the
     * numbers itself, in its own card — the companion's job is to stay.
     */
    private fun careInstruction(level: CareLevel): String = when (level) {
        CareLevel.Acute ->
            "IMPORTANT — they have just told you they are thinking about dying or hurting " +
                "themselves. Take it seriously and stay with them. Acknowledge what they said " +
                "in your first sentence; do not skip past it to something lighter. Do not " +
                "recite a hotline script, do not list resources, do not tell them to seek " +
                "professional help as though ending the conversation — Axiom is already " +
                "showing them where to call. Do not panic, moralise, or promise it gets " +
                "better. Be warm, plain and unhurried, and ask one gentle question that " +
                "invites them to keep talking."
        CareLevel.Struggling ->
            "They are having a genuinely hard time and have said so plainly. Slow down. " +
                "Acknowledge it directly rather than reframing it as a positive, do not offer " +
                "advice or solutions, and do not change the subject. One short, kind question " +
                "at most."
    }

    fun buildSystemPrompt(ctx: Context): String = buildString {
        val name = ctx.displayName.trim()
        if (name.isNotEmpty()) {
            append("You are $name's companion inside Axiom, their private journal. ")
        } else {
            append("You are the user's companion inside Axiom, their private journal. ")
        }
        appendLine("You are a trusted friend, not an assistant and not a therapist.")
        appendLine()
        // First, before anything else can dilute it.
        ctx.care?.let {
            appendLine(careInstruction(it))
            appendLine()
        }

        /*
         * The voice OWNS this block.
         *
         * It used to open with a hardcoded "Warm, specific, and brief" that no
         * persona could reach, with the persona appended below under the header
         * "a leaning, not a rule" and a closing line telling the model it lost
         * every conflict. A blunt companion was structurally impossible: it was
         * contradicted 600 characters before it spoke.
         *
         * When care has fired and the user left "soften when I'm struggling" on,
         * the dials are suspended for this turn only — the care instruction
         * above is the register, and a comedy voice must not answer someone
         * saying they don't want to be here.
         */
        val voice = ctx.voice
        val suspended = ctx.care != null && voice.softenWhenStruggling
        appendLine("How you talk:")
        if (suspended) {
            appendLine("- ${Register.Gentle.instruction}")
        } else {
            voice.instructionLines().forEach { appendLine("- $it") }
        }

        // Measured from their own writing — how long to be, and in which
        // language. Orthogonal to register, so it survives a suspended voice.
        val style = ctx.style
        appendLine("- ${style.languageMix.instruction}")
        if (style.measured) {
            appendLine("- ${style.replyLength.instruction}")
        } else {
            appendLine("- ${ReplyLength.MODERATE.instruction} You do not know them well yet.")
        }
        style.statedPreferences.take(MAX_PREFERENCES).forEach { preference ->
            appendLine("- ${preference.trim().take(MEMORY_ITEM_CAP)}")
        }

        // Their own words, last and highest. Nothing the app inferred should
        // beat what they asked for outright.
        if (voice.hasCustomInstruction && !suspended) {
            appendLine(
                "- ${voice.customInstruction.trim().take(VoiceProfile.MAX_CUSTOM_LENGTH)} " +
                    "(They asked for this directly. It outranks everything above.)"
            )
        }

        appendLine()
        appendLine("True whatever your mood:")
        appendLine(
            "- Plain, natural language. Never use canned phrases: \"Thank you for sharing\", " +
                "\"How does that make you feel?\", \"I'm here for you if you need anything\", " +
                "\"It sounds like\", or anything that describes yourself as an AI."
        )
        appendLine("- Weave in what you remember the way a friend would; never recite lists or dates mechanically.")
        appendLine("- Never invent something they didn't tell you, however well it would fit.")

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

        if (ctx.noticed != null) {
            appendLine()
            appendLine("Something you've noticed over time:")
            appendLine(ctx.noticed.trim())
            appendLine(
                "Only bring this up if it fits what they're saying right now, and say it " +
                    "as an observation you could be wrong about — never as a diagnosis or a statistic."
            )
        }

        appendLine()
        val weekday = ctx.now.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        val moodLine = when {
            ctx.moodToday != null && ctx.emotionToday != null ->
                "reads as ${ctx.emotionToday.label} (${ctx.moodToday}/5), which they have not confirmed"
            ctx.moodToday != null -> "${ctx.moodToday}/5, their own choice"
            else -> "not recorded"
        }
        appendLine("Right now it is $weekday ${timeOfDay(ctx.now.hour)}. Mood today: $moodLine.")
        if (ctx.streakDays > 0) {
            appendLine("Writing streak: ${ctx.streakDays} days — mention only if it comes up naturally.")
        }
        appendLine()
        append(
            "Everything here is private. Never claim you would share it, and if asked what you " +
                "remember, answer honestly and point them to the \"What I remember\" screen."
        )
        // And again at the end. Long prompts lose their middle, and this is the
        // one instruction that must not be the part that gets lost.
        ctx.care?.let {
            appendLine()
            appendLine()
            append(careInstruction(it))
        }
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

    /** Preferences are excluded here — they live in the style block, not among facts. */
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
        /**
         * Widened from 12. A twenty-turn explanation used to lose its own
         * opening while the user was still typing — anything past the window was
         * dropped, not summarized, because the rolling summary was only written
         * three hours after they went quiet.
         */
        const val MAX_HISTORY_MESSAGES = 24
        const val HISTORY_CHAR_BUDGET = 12_000
        const val MAX_EXCERPTS = 4
        const val EXCERPT_CAP = 600
        const val MAX_INSIGHTS = 3
        const val INSIGHT_CAP = 200
        const val SUMMARY_CAP = 1_200
        const val MEMORY_ITEM_CAP = 120
        const val MAX_PREFERENCES = 4

        private val KIND_ORDER = listOf(
            MemoryKind.PERSON,
            MemoryKind.GOAL,
            MemoryKind.THEME,
            MemoryKind.FACT,
            MemoryKind.EVENT
        )

        private val KIND_LABELS = mapOf(
            MemoryKind.PERSON to "People",
            MemoryKind.GOAL to "Goals",
            MemoryKind.THEME to "Recurring themes",
            MemoryKind.FACT to "Facts",
            MemoryKind.EVENT to "Recent events"
        )
    }
}
