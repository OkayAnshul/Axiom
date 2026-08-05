package com.cosmiclaboratory.axiom.data.companion

import com.cosmiclaboratory.axiom.domain.model.Emotion
import com.cosmiclaboratory.axiom.domain.model.MemoryItem
import com.cosmiclaboratory.axiom.domain.model.MemoryKind
import com.cosmiclaboratory.axiom.domain.model.MemorySource
import com.cosmiclaboratory.axiom.domain.style.LanguageMix
import com.cosmiclaboratory.axiom.domain.style.ReplyLength
import com.cosmiclaboratory.axiom.domain.safety.CareLevel
import com.cosmiclaboratory.axiom.domain.style.StyleProfile
import com.cosmiclaboratory.axiom.domain.voice.VoicePreset
import com.cosmiclaboratory.axiom.domain.voice.VoiceProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import com.cosmiclaboratory.axiom.domain.model.CompanionIdentity

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
        voice: VoiceProfile = VoiceProfile(),
        memories: Map<MemoryKind, List<MemoryItem>> = emptyMap(),
        rollingSummary: String = "",
        recentInsights: List<String> = emptyList(),
        excerpts: List<CompanionPromptBuilder.Excerpt> = emptyList(),
        moodToday: Int? = null,
        streakDays: Int = 0,
        emotionToday: Emotion? = null,
        noticed: String? = null,
        style: StyleProfile = StyleProfile(),
        care: CareLevel? = null,
        companionName: String = CompanionIdentity.DEFAULT_NAME
    ) = CompanionPromptBuilder.Context(
        displayName = displayName,
        voice = voice,
        memories = memories,
        rollingSummary = rollingSummary,
        recentInsights = recentInsights,
        excerpts = excerpts,
        now = now,
        moodToday = moodToday,
        streakDays = streakDays,
        style = style,
        emotionToday = emotionToday,
        noticed = noticed,
        care = care,
        companionName = companionName
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

    /**
     * The core of the voice rewrite: "Warm" used to be hardcoded as the first
     * register adjective in a block no persona could reach, so a blunt companion
     * was contradicted 600 characters before it spoke.
     */
    @Test
    fun `the chosen voice owns the register block`() {
        val prompt = builder.buildSystemPrompt(
            context(voice = VoicePreset.Unfiltered.profile())
        )
        assertTrue(prompt, prompt.contains("Say it straight"))
        assertTrue(prompt, prompt.contains("Swearing is entirely fine"))
        assertTrue(prompt, prompt.contains("Push back properly"))
    }

    @Test
    fun `warmth is no longer unconditional`() {
        val blunt = builder.buildSystemPrompt(context(voice = VoicePreset.Blunt.profile()))
        assertFalse(
            "a Blunt voice must not be told to be warm: $blunt",
            blunt.contains("Warm, specific")
        )
    }

    @Test
    fun `the persona is no longer demoted to a suggestion`() {
        val prompt = builder.buildSystemPrompt(context(voice = VoicePreset.Blunt.profile()))
        assertFalse(prompt, prompt.contains("a leaning, not a rule"))
        assertFalse(prompt, prompt.contains("These outrank the leaning"))
    }

    @Test
    fun `the user's own words land last and are said to outrank the dials`() {
        val prompt = builder.buildSystemPrompt(
            context(voice = VoicePreset.Warm.profile().copy(customInstruction = "Call me out."))
        )
        assertTrue(prompt, prompt.contains("Call me out."))
        assertTrue(prompt, prompt.contains("It outranks everything above"))
    }

    /** The one automatic exception, and only when the user left it on. */
    @Test
    fun `distress suspends the voice when softening is enabled`() {
        val on = builder.buildSystemPrompt(
            context(voice = VoicePreset.Unfiltered.profile(), care = CareLevel.Acute)
        )
        assertFalse("swearing must not be permitted mid-crisis: $on", on.contains("Swearing is entirely fine"))
        assertTrue(on, on.contains("Speak softly"))

        val off = builder.buildSystemPrompt(
            context(
                voice = VoicePreset.Unfiltered.profile().copy(softenWhenStruggling = false),
                care = CareLevel.Acute
            )
        )
        assertTrue("voice must stand when softening is off: $off", off.contains("Swearing is entirely fine"))
    }

    // ---- style block --------------------------------------------------------

    @Test
    fun `an unmeasured profile admits it does not know them yet`() {
        val prompt = builder.buildSystemPrompt(context())
        assertTrue(prompt.contains("How you talk:"))
        assertTrue(prompt.contains("You do not know them well yet."))
    }

    @Test
    fun `measured length and language become instructions`() {
        val prompt = builder.buildSystemPrompt(
            context(
                style = StyleProfile(
                    replyLength = ReplyLength.BRIEF,
                    languageMix = LanguageMix.HINGLISH,
                    measured = true
                )
            )
        )
        assertTrue(prompt.contains("One or two sentences back"))
        assertTrue(prompt.contains("Mix it back, naturally"))
        assertFalse(prompt.contains("do not know them well yet"))
    }

    @Test
    fun `stated preferences still render alongside the voice`() {
        val prompt = builder.buildSystemPrompt(
            context(
                voice = VoicePreset.Dry.profile(),
                style = StyleProfile(
                    statedPreferences = listOf("Do not give advice unless asked."),
                    measured = true
                )
            )
        )
        assertTrue(prompt, prompt.contains("Do not give advice unless asked."))
        assertTrue(prompt, prompt.contains("Deadpan wit is welcome"))
    }

    @Test
    fun `preferences do not leak into the biographical memory block`() {
        val prompt = builder.buildSystemPrompt(
            context(
                memories = mapOf(
                    MemoryKind.PREFERENCE to listOf(memory(MemoryKind.PREFERENCE, "Likes short replies")),
                    MemoryKind.PERSON to listOf(memory(MemoryKind.PERSON, "Riya, younger sister"))
                )
            )
        )
        // The memory block is about who they are, not how to speak to them.
        assertFalse(prompt.contains("Preferences:"))
        assertTrue(prompt.contains("People:"))
    }

    @Test
    fun `only the first few preferences are sent`() {
        val many = (1..10).map { "Preference number $it." }
        val prompt = builder.buildSystemPrompt(context(style = StyleProfile(statedPreferences = many)))
        assertTrue(prompt.contains("Preference number ${CompanionPromptBuilder.MAX_PREFERENCES}."))
        assertFalse(prompt.contains("Preference number ${CompanionPromptBuilder.MAX_PREFERENCES + 1}."))
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
        // Derived from the constant rather than hardcoded, so retuning the
        // window is a one-line change instead of a test rewrite.
        val max = CompanionPromptBuilder.MAX_HISTORY_MESSAGES
        val total = max + 8
        val messages = (1..total).map { "user" to "message $it" }
        val window = builder.windowHistory(messages)
        assertEquals(max, window.size)
        assertEquals("message $total", window.last().second)
        assertEquals("message ${total - max + 1}", window.first().second)
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
        val huge = "h".repeat(CompanionPromptBuilder.HISTORY_CHAR_BUDGET + 2_000)
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

    // ---- what the companion is called --------------------------------------

    @Test
    fun `the companion is told its own name`() {
        val prompt = builder.buildSystemPrompt(context(companionName = "Jarvis"))
        assertTrue(prompt.take(120), prompt.contains("You are Jarvis,"))
    }

    @Test
    fun `a renamed companion is never called Axiom in its own prompt`() {
        // The bottom bar says the new name. A prompt still saying "Axiom" would
        // have the companion answering to a name the user cannot see.
        val prompt = builder.buildSystemPrompt(context(companionName = "Sol"))
        assertFalse(prompt, prompt.contains("Axiom"))
    }

    @Test
    fun `a blank name falls back to the app's own`() {
        val prompt = builder.buildSystemPrompt(context(companionName = "   "))
        assertTrue(prompt.take(120), prompt.contains("You are Axiom,"))
    }

    @Test
    fun `the user's name and the companion's are both used, and not confused`() {
        val prompt = builder.buildSystemPrompt(
            context(displayName = "Anshul", companionName = "Sol")
        )
        assertTrue(prompt.take(160), prompt.contains("You are Sol, Anshul's companion"))
    }
}
