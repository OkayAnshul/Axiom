package com.cosmiclaboratory.axiom.data.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The daily opener is the first thing the user reads, and it used to be
 * generated with no knowledge of them at all — which is why it produced
 * self-help filler the curated pack already covered.
 */
class InitiatorPromptTest {

    private val memories = listOf(
        "Riya is your younger sister and lives in Pune.",
        "Keeps meaning to start running again but has not begun."
    )

    @Test
    fun `memory is included and the model is told to ask from it, not about it`() {
        val prompt = PromptTemplates.initiatorPrompts(emptyList(), memories, 5)
        assertTrue(prompt.contains("What you know about them:"))
        assertTrue(prompt.contains("Riya is your younger sister"))
        assertTrue(prompt.contains("ask from it, not about it"))
        assertTrue(prompt.contains("One person or one thread per question"))
    }

    @Test
    fun `it is told not to assume outcomes and to keep one question general`() {
        val prompt = PromptTemplates.initiatorPrompts(emptyList(), memories, 5)
        // A companion that assumes the interview went well is worse than one that asks.
        assertTrue(prompt.contains("Do not assume how something turned out"))
        assertTrue(prompt.contains("at least one question open and general"))
    }

    @Test
    fun `a stranger gets gentle open questions instead of an empty memory section`() {
        val prompt = PromptTemplates.initiatorPrompts(emptyList(), emptyList(), 5)
        assertFalse(prompt.contains("What you know about them:"))
        assertTrue(prompt.contains("You do not know them yet"))
    }

    @Test
    fun `memory lines are capped so the model cannot write a biography quiz`() {
        val many = (1..30).map { "Remembered detail number $it." }
        val prompt = PromptTemplates.initiatorPrompts(emptyList(), many, 5)
        assertTrue(prompt.contains("Remembered detail number ${PromptTemplates.MAX_MEMORY_LINES}."))
        assertFalse(prompt.contains("Remembered detail number ${PromptTemplates.MAX_MEMORY_LINES + 1}."))
    }

    @Test
    fun `recent entries still contribute and the count is honoured`() {
        val prompt = PromptTemplates.initiatorPrompts(listOf("Wrote about a hard week."), memories, 7)
        assertTrue(prompt.contains("Write 7 short questions"))
        assertTrue(prompt.contains("What they wrote about recently:"))
        assertTrue(prompt.contains("Wrote about a hard week."))
    }

    @Test
    fun `the response shape is still requested as strict JSON`() {
        val prompt = PromptTemplates.initiatorPrompts(emptyList(), memories, 5)
        assertTrue(prompt.contains("""{"prompts": ["question 1", "question 2", "..."]}"""))
    }
}
