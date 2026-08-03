package com.cosmiclaboratory.axiom.domain.style

import com.cosmiclaboratory.axiom.domain.model.MemoryItem
import com.cosmiclaboratory.axiom.domain.model.MemoryKind
import com.cosmiclaboratory.axiom.domain.model.MemorySource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class StyleProfilerTest {

    private fun preference(text: String, userEdited: Boolean = false) = MemoryItem(
        id = 1, kind = MemoryKind.PREFERENCE, text = text, weight = 0.7f, timesSeen = 1,
        createdAt = LocalDateTime.now(), lastSeenAt = LocalDateTime.now(),
        source = MemorySource.CONVERSATION, sourceId = null, userEdited = userEdited
    )

    // ---- reply length -------------------------------------------------------

    @Test
    fun `short messages ask for short replies`() {
        val profile = StyleProfiler.profile(listOf("rough day", "ugh", "not great", "same as always"))
        assertEquals(ReplyLength.BRIEF, profile.replyLength)
        assertTrue(profile.measured)
    }

    @Test
    fun `long messages allow longer replies`() {
        val long = List(4) { (1..60).joinToString(" ") { n -> "word$n" } }
        assertEquals(ReplyLength.EXPANSIVE, StyleProfiler.profile(long).replyLength)
    }

    @Test
    fun `one outlier does not set the tone`() {
        // Three terse messages and one enormous vent: the median holds.
        val messages = listOf(
            "tired",
            "ok",
            "not much to say",
            (1..300).joinToString(" ") { "word$it" }
        )
        assertEquals(ReplyLength.BRIEF, StyleProfiler.replyLengthFor(messages))
    }

    @Test
    fun `too little writing leaves the profile unmeasured`() {
        val profile = StyleProfiler.profile(listOf("hello", "hi"))
        assertFalse(profile.measured)
        assertEquals(ReplyLength.MODERATE, profile.replyLength)
        assertEquals(LanguageMix.ENGLISH, profile.languageMix)
    }

    @Test
    fun `blank messages do not count toward the minimum`() {
        assertFalse(StyleProfiler.profile(listOf("hi", "  ", "", "there", "  ")).measured)
    }

    // ---- language mix -------------------------------------------------------

    @Test
    fun `plain english stays english`() {
        assertEquals(
            LanguageMix.ENGLISH,
            StyleProfiler.languageMixFor(listOf("Work was hard today but I got through it."))
        )
    }

    @Test
    fun `devanagari only is hindi`() {
        assertEquals(
            LanguageMix.HINDI,
            StyleProfiler.languageMixFor(listOf("आज का दिन बहुत मुश्किल था।", "मैं थक गया हूँ।"))
        )
    }

    @Test
    fun `devanagari beside english is hinglish`() {
        assertEquals(
            LanguageMix.HINGLISH,
            StyleProfiler.languageMixFor(listOf("Meeting ठीक रही but I was बहुत tired the whole time."))
        )
    }

    @Test
    fun `romanised hinglish is caught without any devanagari`() {
        assertEquals(
            LanguageMix.HINGLISH,
            StyleProfiler.languageMixFor(
                listOf("Aaj bahut tired hoon yaar", "kal ka meeting theek tha lekin thoda stressful")
            )
        )
    }

    @Test
    fun `an english sentence with one foreign-looking word stays english`() {
        assertEquals(
            LanguageMix.ENGLISH,
            StyleProfiler.languageMixFor(
                listOf("We went to the cafe and the food was genuinely excellent all evening.")
            )
        )
    }

    // ---- stated preferences -------------------------------------------------

    @Test
    fun `preferences the user wrote themselves come first`() {
        val profile = StyleProfiler.profile(
            userMessages = listOf("a", "b", "c", "d"),
            preferences = listOf(
                preference("Prefers being asked questions."),
                preference("Does not want advice unless they ask.", userEdited = true)
            )
        )
        assertEquals("Does not want advice unless they ask.", profile.statedPreferences.first())
        assertEquals(2, profile.statedPreferences.size)
    }

    @Test
    fun `preferences survive even when there is too little writing to measure`() {
        val profile = StyleProfiler.profile(
            userMessages = listOf("hi"),
            preferences = listOf(preference("Hates being called buddy."))
        )
        assertFalse(profile.measured)
        assertEquals(listOf("Hates being called buddy."), profile.statedPreferences)
    }

    @Test
    fun `every length and language carries a usable instruction`() {
        assertTrue(ReplyLength.entries.all { it.instruction.isNotBlank() })
        assertTrue(LanguageMix.entries.all { it.instruction.isNotBlank() })
    }
}
