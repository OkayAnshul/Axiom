package com.cosmiclaboratory.axiom.data.companion

import com.cosmiclaboratory.axiom.domain.model.Emotion
import com.cosmiclaboratory.axiom.domain.model.MemoryItem
import com.cosmiclaboratory.axiom.domain.model.MemoryKind
import com.cosmiclaboratory.axiom.domain.model.MemorySource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class CompanionPromptBuilderTest {

    private val builder = CompanionPromptBuilder()
    private val now = LocalDateTime.of(2026, 8, 3, 20, 30) // Monday evening

    private fun memory(kind: MemoryKind, text: String) = MemoryItem(
        id = 1, kind = kind, text = text, weight = 0.8f, timesSeen = 2,
        createdAt = now.minusDays(10), lastSeenAt = now.minusDays(1),
        source = MemorySource.CONVERSATION, sourceId = null, userEdited = false
    )

    private fun context(
        displayName: String = "Anshul",
        personaFragment: String = "",
        memories: Map<MemoryKind, List<MemoryItem>> = emptyMap(),
        rollingSummary: String = "",
        recentInsights: List<String> = emptyList(),
        excerpts: List<CompanionPromptBuilder.Excerpt> = emptyList(),
        moodToday: Int? = null,
        streakDays: Int = 0,
        emotionToday: Emotion? = null,
        noticed: String? = null
    ) = CompanionPromptBuilder.Context(
        displayName, personaFragment, memories, rollingSummary,
        recentInsights, excerpts, now, moodToday, streakDays, emotionToday, noticed
    )

    // ---- system prompt sections --------------------------------------------

    @Test
    fun `bare context omits every optional section`() {
        val prompt = builder.buildSystemPrompt(context())
        assertFalse(prompt.contains("What you remember"))
        assertFalse(prompt.contains("Where your conversations have been lately"))
        assertFalse(prompt.contains("From their recent journal"))
        assertFalse(prompt.contains("Writing streak"))
        assertTrue(prompt.contains("Mood today: not recorded."))
    }

    @Test
    fun `display name is woven into identity line`() {
        val prompt = builder.buildSystemPrompt(context(displayName = "Anshul"))
        assertTrue(prompt.contains("Anshul's companion"))
    }

    @Test
    fun `blank name falls back without crashing`() {
        val prompt = builder.buildSystemPrompt(context(displayName = "  "))
        assertTrue(prompt.contains("the user's companion"))
    }

    @Test
    fun `anti-robotic instructions are always present`() {
        val prompt = builder.buildSystemPrompt(context())
        assertTrue(prompt.contains("Thank you for sharing"))
        assertTrue(prompt.contains("How does that make you feel?"))
        assertTrue(prompt.contains("not an assistant and not a therapist"))
        assertTrue(prompt.contains("At most one question per reply"))
    }

    @Test
    fun `privacy footer is always present`() {
        val prompt = builder.buildSystemPrompt(context())
        assertTrue(prompt.contains("What I remember"))
    }

    @Test
    fun `memories render grouped under kind labels in stable order`() {
        val prompt = builder.buildSystemPrompt(
            context(
                memories = mapOf(
                    MemoryKind.GOAL to listOf(memory(MemoryKind.GOAL, "Wants to run a half marathon")),
                    MemoryKind.PERSON to listOf(memory(MemoryKind.PERSON, "Riya — younger sister, lives in Pune"))
                )
            )
        )
        assertTrue(prompt.contains("People:\n- Riya — younger sister, lives in Pune"))
        assertTrue(prompt.contains("Goals:\n- Wants to run a half marathon"))
        assertTrue(prompt.indexOf("People:") < prompt.indexOf("Goals:"))
    }

    @Test
    fun `memory item text is capped`() {
        val longText = "x".repeat(500)
        val prompt = builder.buildSystemPrompt(
            context(memories = mapOf(MemoryKind.FACT to listOf(memory(MemoryKind.FACT, longText))))
        )
        assertTrue(prompt.contains("x".repeat(CompanionPromptBuilder.MEMORY_ITEM_CAP)))
        assertFalse(prompt.contains("x".repeat(CompanionPromptBuilder.MEMORY_ITEM_CAP + 1)))
    }

    @Test
    fun `rolling summary is capped`() {
        val prompt = builder.buildSystemPrompt(
            context(rollingSummary = "s".repeat(CompanionPromptBuilder.SUMMARY_CAP + 500))
        )
        assertTrue(prompt.contains("s".repeat(CompanionPromptBuilder.SUMMARY_CAP)))
        assertFalse(prompt.contains("s".repeat(CompanionPromptBuilder.SUMMARY_CAP + 1)))
    }

    @Test
    fun `excerpts are capped in count and length`() {
        val excerpts = (1..6).map {
            CompanionPromptBuilder.Excerpt(LocalDate.of(2026, 8, it), 4, "e".repeat(1000))
        }
        val prompt = builder.buildSystemPrompt(context(excerpts = excerpts))
        assertTrue(prompt.contains("[2026-08-0${CompanionPromptBuilder.MAX_EXCERPTS}"))
        assertFalse(prompt.contains("[2026-08-0${CompanionPromptBuilder.MAX_EXCERPTS + 1}"))
        assertFalse(prompt.contains("e".repeat(CompanionPromptBuilder.EXCERPT_CAP + 1)))
    }

    @Test
    fun `insights are capped at three`() {
        val prompt = builder.buildSystemPrompt(
            context(recentInsights = listOf("first", "second", "third", "fourth"))
        )
        assertTrue(prompt.contains("- third"))
        assertFalse(prompt.contains("- fourth"))
    }

    @Test
    fun `streak and mood render when present`() {
        val prompt = builder.buildSystemPrompt(context(moodToday = 4, streakDays = 5))
        // Phase 11 qualifies the mood by where it came from, so the companion
        // can hedge about a reading and not about a choice.
        assertTrue(prompt.contains("Mood today: 4/5, their own choice."))
        assertTrue(prompt.contains("Writing streak: 5 days"))
        assertTrue(prompt.contains("Monday evening"))
    }

    @Test
    fun `persona fragment is included verbatim`() {
        val prompt = builder.buildSystemPrompt(
            context(personaFragment = "Your natural register is playful.")
        )
        assertTrue(prompt.contains("Your natural register is playful."))
    }

    // ---- inferred feeling and noticed patterns -----------------------------

    @Test
    fun `an inferred mood is marked unconfirmed, a chosen one is not`() {
        val inferred = builder.buildSystemPrompt(context(moodToday = 2, emotionToday = Emotion.ANXIETY))
        assertTrue(inferred.contains("reads as anxiety (2/5), which they have not confirmed"))

        val chosen = builder.buildSystemPrompt(context(moodToday = 4))
        assertTrue(chosen.contains("4/5, their own choice"))
        assertFalse(chosen.contains("not confirmed"))
    }

    @Test
    fun `a noticed pattern comes with instructions to hold it lightly`() {
        val prompt = builder.buildSystemPrompt(
            context(noticed = "Mondays tend to be harder than the rest of your week.")
        )
        assertTrue(prompt.contains("Something you've noticed over time:"))
        assertTrue(prompt.contains("Mondays tend to be harder"))
        assertTrue(prompt.contains("could be wrong about"))
        assertTrue(prompt.contains("never as a diagnosis or a statistic"))
    }

    @Test
    fun `no pattern means no section`() {
        val prompt = builder.buildSystemPrompt(context())
        assertFalse(prompt.contains("Something you've noticed"))
    }

    // ---- history window ----------------------------------------------------

    @Test
    fun `window keeps newest messages within count limit`() {
        val messages = (1..20).map { "user" to "message $it" }
        val window = builder.windowHistory(messages)
        assertEquals(CompanionPromptBuilder.MAX_HISTORY_MESSAGES, window.size)
        assertEquals("message 20", window.last().second)
        assertEquals("message 9", window.first().second)
    }

    @Test
    fun `window evicts oldest first when over char budget`() {
        val big = "b".repeat(2_500)
        val messages = listOf(
            "user" to big, "assistant" to big, "user" to big, "assistant" to "small tail"
        )
        val window = builder.windowHistory(messages)
        assertTrue(window.sumOf { it.second.length } <= CompanionPromptBuilder.HISTORY_CHAR_BUDGET)
        assertEquals("small tail", window.last().second)
    }

    @Test
    fun `newest message is never evicted even when alone it busts the budget`() {
        val huge = "h".repeat(10_000)
        val window = builder.windowHistory(listOf("user" to "old", "user" to huge))
        assertEquals(1, window.size)
        assertEquals(CompanionPromptBuilder.HISTORY_CHAR_BUDGET, window[0].second.length)
        // Truncated from the front — the most recent tail survives.
        assertEquals("h", window[0].second.take(1))
    }

    @Test
    fun `empty history returns empty window`() {
        assertTrue(builder.windowHistory(emptyList()).isEmpty())
    }
}
