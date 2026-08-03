package com.cosmiclaboratory.axiom.data.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The proactive prompts carry the restraint rules for unprompted messages, so
 * their shape is worth pinning: an open loop must lead, and the "haven't heard
 * from you" line must only appear after a real absence.
 */
class ProactivePromptTest {

    @Test
    fun `open loops lead the check-in`() {
        val prompt = PromptTemplates.dailyCheckIn(
            displayName = "Anshul",
            timeOfDay = "evening",
            openLoops = listOf("Has a visa interview on Tuesday."),
            recentThemes = listOf("work stress"),
            daysSinceLastEntry = 1
        )
        assertTrue(prompt.contains("you said you'd circle back"))
        assertTrue(prompt.contains("Has a visa interview on Tuesday."))
        assertTrue(prompt.indexOf("circle back") < prompt.indexOf("Recently on their mind"))
    }

    @Test
    fun `no open loops means no follow-up section`() {
        val prompt = PromptTemplates.dailyCheckIn(
            displayName = "Anshul",
            timeOfDay = "morning",
            openLoops = emptyList(),
            recentThemes = emptyList(),
            daysSinceLastEntry = null
        )
        assertFalse(prompt.contains("circle back"))
        assertFalse(prompt.contains("Recently on their mind"))
        assertTrue(prompt.contains("It is morning."))
    }

    @Test
    fun `absence is only mentioned after several days`() {
        fun promptForGap(days: Int?) = PromptTemplates.dailyCheckIn(
            displayName = "", timeOfDay = "evening",
            openLoops = emptyList(), recentThemes = emptyList(), daysSinceLastEntry = days
        )
        assertFalse(promptForGap(null).contains("haven't heard from them"))
        assertFalse(promptForGap(1).contains("haven't heard from them"))
        assertFalse(promptForGap(2).contains("haven't heard from them"))
        assertTrue(promptForGap(5).contains("haven't heard from them in 5 days"))
        // And never as a reprimand.
        assertTrue(promptForGap(5).contains("glad to hear from them, not disappointed"))
    }

    @Test
    fun `system prompt bans the usual reminder-app phrasing`() {
        assertTrue(PromptTemplates.PROACTIVE_SYSTEM.contains("Just checking in!"))
        assertTrue(PromptTemplates.PROACTIVE_SYSTEM.contains("never guilt them"))
        assertTrue(PromptTemplates.PROACTIVE_SYSTEM.contains("never mention streaks as targets"))
    }

    @Test
    fun `weekly recap asks for prose, not a dashboard`() {
        val prompt = PromptTemplates.weeklyRecap(
            displayName = "Anshul",
            entryCount = 3,
            moodNote = "Mon 4/5, Wed 2/5",
            highlights = listOf("Interview prep", "Argument with a friend")
        )
        assertTrue(prompt.contains("No lists, no statistics, no headings"))
        assertTrue(prompt.contains("wrote 3 times this week"))
        assertTrue(prompt.contains("Mon 4/5, Wed 2/5"))
    }

    @Test
    fun `singular entry count reads naturally`() {
        val prompt = PromptTemplates.weeklyRecap("", 1, "not recorded", emptyList())
        assertTrue(prompt.contains("wrote 1 time this week"))
    }

    @Test
    fun `extraction prompt asks for sparing follow-ups`() {
        assertTrue(PromptTemplates.MEMORY_EXTRACTION_SYSTEM.contains("follow_up_in_days"))
        assertTrue(PromptTemplates.MEMORY_EXTRACTION_SYSTEM.contains("Use it sparingly"))
        assertTrue(PromptTemplates.memoryExtraction("text", emptyList()).contains("follow_up_in_days"))
    }
}
