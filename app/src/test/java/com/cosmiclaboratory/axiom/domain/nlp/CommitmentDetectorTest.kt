package com.cosmiclaboratory.axiom.domain.nlp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CommitmentDetectorTest {

    /** A Monday, so "on Friday" and "this weekend" land in the same week. */
    private val monday = LocalDate.of(2026, 8, 3)

    private fun detect(text: String) = CommitmentDetector.detect(text, monday)

    @Test
    fun `tomorrow is found and followed up the day after`() {
        val found = detect("I have the visa interview tomorrow and I am dreading it.")
        assertEquals(1, found.size)
        assertEquals(monday.plusDays(1), found[0].happensOn)
        assertEquals(monday.plusDays(2), found[0].followUpOn)
        assertTrue(found[0].sentence.contains("visa interview"))
    }

    @Test
    fun `a named weekday resolves to the coming one`() {
        val found = detect("The scan is on Friday, finally.")
        assertEquals(LocalDate.of(2026, 8, 7), found.single().happensOn)
    }

    @Test
    fun `day after tomorrow beats tomorrow`() {
        assertEquals(monday.plusDays(2), detect("Results come out day after tomorrow.").single().happensOn)
    }

    @Test
    fun `in N days weeks and months all parse`() {
        assertEquals(monday.plusDays(3), detect("The appraisal is in 3 days apparently.").single().happensOn)
        assertEquals(monday.plusWeeks(2), detect("We move house in 2 weeks from now.").single().happensOn)
        assertEquals(monday.plusMonths(1), detect("The wedding is in 1 month, somehow.").single().happensOn)
    }

    @Test
    fun `next week and next month parse`() {
        assertEquals(monday.plusWeeks(1), detect("I present this to the board next week.").single().happensOn)
        assertEquals(monday.plusMonths(1), detect("Lease renewal comes up next month.").single().happensOn)
    }

    @Test
    fun `hinglish next week parses`() {
        assertEquals(monday.plusWeeks(1), detect("Presentation agle hafte hai, thoda nervous.").single().happensOn)
    }

    @Test
    fun `the weekend resolves to saturday`() {
        assertEquals(LocalDate.of(2026, 8, 8), detect("Driving to Pune this weekend.").single().happensOn)
    }

    // ---- the refusals, which matter more ------------------------------------

    @Test
    fun `kal is ambiguous in hindi and is never guessed`() {
        // "kal" means both yesterday and tomorrow; picking one would invent an event.
        assertTrue(detect("Kal interview hai, bahut nervous hoon.").isEmpty())
        assertTrue(detect("Parso doctor ke paas jana hai.").isEmpty())
    }

    @Test
    fun `past tense with a weekday is not a commitment`() {
        // No preposition, so a diary-style mention does not become a future event.
        assertTrue(detect("Friday was completely exhausting from start to end.").isEmpty())
    }

    @Test
    fun `tonight is today and yields nothing to follow up`() {
        assertTrue(detect("Dinner with the team tonight, should be fun.").isEmpty())
    }

    @Test
    fun `dates beyond the horizon are ignored`() {
        assertTrue(detect("The conference is in 12 months, ages away.").isEmpty())
    }

    @Test
    fun `text with no dates yields nothing`() {
        assertTrue(detect("Today was long and I am tired of all of it.").isEmpty())
    }

    @Test
    fun `only the sentence carrying the date is captured`() {
        val found = detect(
            "Work was a slog today and I barely ate. " +
                "The interview is tomorrow. " +
                "I should sleep early tonight."
        )
        assertEquals(1, found.size)
        assertEquals("The interview is tomorrow", found[0].sentence)
    }

    @Test
    fun `two different commitments are both found`() {
        val found = detect("Scan is on Wednesday. Then we fly out next month.")
        assertEquals(2, found.size)
    }

    @Test
    fun `the same sentence is not reported twice`() {
        val found = detect("Interview tomorrow. Interview tomorrow.")
        assertEquals(1, found.size)
    }

    @Test
    fun `very short fragments are ignored`() {
        assertTrue(detect("ok. tomorrow.").isEmpty())
    }
}
