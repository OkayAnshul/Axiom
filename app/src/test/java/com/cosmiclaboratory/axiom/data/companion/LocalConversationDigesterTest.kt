package com.cosmiclaboratory.axiom.data.companion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the pure extraction heuristics. The repository-touching parts are
 * exercised on device; what matters here is that the crude detectors are crude
 * in the right direction — a wrong PERSON memory is a stranger's name in your
 * own journal, which is worse than missing one.
 */
class LocalConversationDigesterTest {

    private val digester = LocalConversationDigester.Companion

    // ---- names -------------------------------------------------------------

    @Test
    fun `a repeated name is a candidate`() {
        val names = digester.candidateNames(
            listOf(
                "I had a long call with Riya today",
                "Riya seemed happier than last time"
            )
        )
        assertTrue(names.toString(), names.contains("Riya"))
    }

    @Test
    fun `a name mentioned once is not enough`() {
        val names = digester.candidateNames(listOf("I had a call with Riya today"))
        assertTrue(names.toString(), names.isEmpty())
    }

    @Test
    fun `sentence-initial capitals are not names`() {
        // "Work" and "Tomorrow" lead sentences; grammar capitalised them.
        val names = digester.candidateNames(
            listOf(
                "Work was exhausting. Tomorrow will be worse.",
                "Work is still exhausting. Tomorrow is here."
            )
        )
        assertFalse(names.toString(), names.contains("Work"))
        assertFalse(names.toString(), names.contains("Tomorrow"))
    }

    @Test
    fun `weekdays and months are never names`() {
        val names = digester.candidateNames(
            listOf(
                "I saw them on Monday and again in August",
                "then on Monday again, and August was busy"
            )
        )
        assertFalse(names.toString(), names.contains("Monday"))
        assertFalse(names.toString(), names.contains("August"))
    }

    @Test
    fun `acronyms are not names`() {
        val names = digester.candidateNames(
            listOf("the EMI is due soon", "that EMI keeps bothering me")
        )
        assertFalse(names.toString(), names.contains("EMI"))
    }

    // ---- themes ------------------------------------------------------------

    @Test
    fun `a word across enough turns becomes a theme`() {
        val themes = digester.candidateThemes(
            listOf(
                "the interview keeps going round my head",
                "I keep replaying the interview",
                "maybe the interview went better than I thought"
            )
        )
        assertTrue(themes.toString(), themes.contains("interview"))
    }

    @Test
    fun `a short session yields no themes`() {
        val themes = digester.candidateThemes(
            listOf("the interview went fine", "the interview really did")
        )
        assertTrue(themes.toString(), themes.isEmpty())
    }

    @Test
    fun `stopwords never become themes`() {
        val themes = digester.candidateThemes(
            listOf(
                "that was the thing with the day",
                "and that was the day with the thing",
                "the day and the thing, that was that"
            )
        )
        listOf("that", "was", "the", "with", "and").forEach {
            assertFalse("$it should not be a theme: $themes", themes.contains(it))
        }
    }

    /**
     * Found on device: chat turns often have no full stops, so frequency alone
     * decided the user's recurring theme was "long" and "message".
     */
    @Test
    fun `filler words never become themes`() {
        val themes = digester.candidateThemes(
            listOf(
                "I think that thing is really going to be a problem",
                "still thinking about that thing, feeling odd",
                "the thing is I really think it going to be fine",
                "maybe that thing again today"
            )
        )
        listOf("thing", "think", "really", "going", "today", "feeling").forEach {
            assertFalse("$it should not be a theme: $themes", themes.contains(it))
        }
    }

    @Test
    fun `a theme must appear in a real share of the conversation, not just three times`() {
        // "interview" in 3 of 8 turns is not what the conversation was about.
        val turns = List(5) { "talked about the weather and the commute here" } +
            List(3) { "the interview came up briefly" }
        assertFalse(
            digester.candidateThemes(turns).toString(),
            digester.candidateThemes(turns).contains("interview")
        )
    }

    @Test
    fun `a genuine subject still surfaces`() {
        val themes = digester.candidateThemes(
            listOf(
                "the interview is on my mind",
                "I keep replaying the interview",
                "the interview went better than expected"
            )
        )
        assertTrue(themes.toString(), themes.contains("interview"))
    }

    @Test
    fun `empty input is handled`() {
        assertEquals(emptyList<String>(), digester.candidateNames(emptyList()))
        assertEquals(emptyList<String>(), digester.candidateThemes(emptyList()))
    }
}
