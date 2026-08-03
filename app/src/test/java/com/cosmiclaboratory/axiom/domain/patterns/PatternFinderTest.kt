package com.cosmiclaboratory.axiom.domain.patterns

import com.cosmiclaboratory.axiom.domain.model.Entry
import com.cosmiclaboratory.axiom.domain.model.MemoryItem
import com.cosmiclaboratory.axiom.domain.model.MemoryKind
import com.cosmiclaboratory.axiom.domain.model.MemorySource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters

/**
 * The failure mode these guard against is not a missed pattern — it is
 * confidently telling someone something untrue about their own life. Every
 * test therefore pins a *refusal* as tightly as a detection.
 */
class PatternFinderTest {

    private val today = LocalDate.of(2026, 8, 3) // a Monday

    private fun entry(date: LocalDate, mood: Int?, id: Long = 0): Entry = Entry(
        id = id,
        content = "x",
        createdAt = date.atTime(20, 0),
        updatedAt = date.atTime(20, 0),
        mood = mood
    )

    private fun onWeekday(day: DayOfWeek, weeksBack: Int, mood: Int): Entry {
        val date = today.with(TemporalAdjusters.previousOrSame(day)).minusWeeks(weeksBack.toLong())
        return entry(date, mood)
    }

    // ---- day of week --------------------------------------------------------

    @Test
    fun `a consistently harder weekday is reported`() {
        val entries = (0..3).flatMap { week ->
            listOf(
                onWeekday(DayOfWeek.MONDAY, week, 2),
                onWeekday(DayOfWeek.WEDNESDAY, week, 4),
                onWeekday(DayOfWeek.FRIDAY, week, 4)
            )
        }
        val finding = PatternFinder.hardestDayOfWeek(entries)
        assertTrue(finding != null && finding.text.contains("Mondays"))
    }

    @Test
    fun `too few samples for a day yields nothing`() {
        val entries = listOf(
            onWeekday(DayOfWeek.MONDAY, 0, 1),
            onWeekday(DayOfWeek.WEDNESDAY, 0, 5),
            onWeekday(DayOfWeek.WEDNESDAY, 1, 5),
            onWeekday(DayOfWeek.WEDNESDAY, 2, 5)
        )
        // Monday has one observation — one bad Monday is not a pattern.
        assertNull(PatternFinder.hardestDayOfWeek(entries))
    }

    @Test
    fun `a flat week reports no hardest day`() {
        val entries = (0..3).flatMap { week ->
            listOf(
                onWeekday(DayOfWeek.MONDAY, week, 3),
                onWeekday(DayOfWeek.THURSDAY, week, 3)
            )
        }
        assertNull(PatternFinder.hardestDayOfWeek(entries))
    }

    @Test
    fun `entries without moods cannot make a day pattern`() {
        val entries = (0..3).flatMap { week ->
            listOf(onWeekday(DayOfWeek.MONDAY, week, 2).copy(mood = null))
        }
        assertNull(PatternFinder.hardestDayOfWeek(entries))
    }

    // ---- mood trend ---------------------------------------------------------

    @Test
    fun `a lighter week is reported as lighter`() {
        val entries = (0..3).map { entry(today.minusDays(it.toLong()), 5) } +
            (7..10).map { entry(today.minusDays(it.toLong()), 2) }
        val finding = PatternFinder.moodTrend(entries, today)
        assertTrue(finding != null && finding.text.contains("lighter this week"))
    }

    @Test
    fun `a heavier week is reported as heavier`() {
        val entries = (0..3).map { entry(today.minusDays(it.toLong()), 2) } +
            (7..10).map { entry(today.minusDays(it.toLong()), 5) }
        val finding = PatternFinder.moodTrend(entries, today)
        assertTrue(finding != null && finding.text.contains("heavier"))
    }

    @Test
    fun `a missing comparison week yields no trend`() {
        val entries = (0..5).map { entry(today.minusDays(it.toLong()), 5) }
        assertNull(PatternFinder.moodTrend(entries, today))
    }

    @Test
    fun `a small drift is not worth mentioning`() {
        val entries = (0..3).map { entry(today.minusDays(it.toLong()), 4) } +
            (7..10).map { entry(today.minusDays(it.toLong()), 4) }
        assertNull(PatternFinder.moodTrend(entries, today))
    }

    // ---- recurring themes ---------------------------------------------------

    private fun memory(
        text: String,
        timesSeen: Int,
        kind: MemoryKind = MemoryKind.THEME
    ) = MemoryItem(
        id = 1, kind = kind, text = text, weight = 0.5f, timesSeen = timesSeen,
        createdAt = LocalDateTime.now(), lastSeenAt = LocalDateTime.now(),
        source = MemorySource.CONVERSATION, sourceId = null, userEdited = false
    )

    @Test
    fun `a repeatedly reinforced memory becomes a finding`() {
        val findings = PatternFinder.recurringThemes(
            listOf(memory("Wants to start running again.", 6), memory("Mentioned once.", 1))
        )
        assertEquals(1, findings.size)
        assertTrue(findings[0].text.contains("Wants to start running again"))
        assertTrue(findings[0].text.contains("6 times"))
    }

    @Test
    fun `a memory seen twice is not yet recurring`() {
        assertTrue(PatternFinder.recurringThemes(listOf(memory("Twice only.", 2))).isEmpty())
    }

    @Test
    fun `stable facts about people never count as recurring`() {
        // Reinforced constantly, but "your sister lives in Pune" is not a pattern
        // — and saying so duplicates whatever the person finding already said.
        val findings = PatternFinder.recurringThemes(
            listOf(
                memory("Riya is the user's younger sister.", 9, MemoryKind.PERSON),
                memory("Works at a hospital in Bangalore.", 7, MemoryKind.FACT)
            )
        )
        assertTrue(findings.isEmpty())
    }

    @Test
    fun `goals recur and are addressed to the reader, not about them`() {
        val findings = PatternFinder.recurringThemes(
            listOf(memory("The user's plan to start running has stalled.", 4, MemoryKind.GOAL))
        )
        assertEquals(1, findings.size)
        assertTrue(findings[0].text.contains("Your plan to start running"))
        assertTrue(!findings[0].text.contains("the user"))
    }

    // ---- person correlation -------------------------------------------------

    @Test
    fun `a person tied to brighter days is reported warmly`() {
        val all = (1..8).map { entry(today.minusDays(it.toLong()), if (it <= 4) 5 else 2) }
        val mentioned = all.take(4) // the mood-5 days
        val findings = PatternFinder.personCorrelations(mapOf("Riya" to mentioned), all)
        assertEquals(1, findings.size)
        assertTrue(findings[0].text.contains("brighter when Riya comes up"))
    }

    @Test
    fun `a person tied to harder days is reported plainly`() {
        val all = (1..8).map { entry(today.minusDays(it.toLong()), if (it <= 4) 1 else 4) }
        val findings = PatternFinder.personCorrelations(mapOf("Dad" to all.take(4)), all)
        assertTrue(findings.any { it.text.contains("Dad comes up most on your harder days") })
    }

    @Test
    fun `two mentions are an anecdote, not a pattern`() {
        val all = (1..8).map { entry(today.minusDays(it.toLong()), 3) }
        val findings = PatternFinder.personCorrelations(mapOf("Sam" to all.take(2)), all)
        assertTrue(findings.isEmpty())
    }

    // ---- ordering -----------------------------------------------------------

    @Test
    fun `findings come back strongest first and an empty corpus finds nothing`() {
        assertTrue(PatternFinder.find(emptyList(), emptyList(), emptyMap(), today).isEmpty())

        val entries = (0..3).map { entry(today.minusDays(it.toLong()), 5) } +
            (7..10).map { entry(today.minusDays(it.toLong()), 2) }
        val findings = PatternFinder.find(
            entries = entries,
            memories = listOf(memory("Keeps putting off the dentist.", 4)),
            today = today
        )
        assertTrue(findings.size >= 2)
        assertTrue(findings.zipWithNext().all { (a, b) -> a.confidence >= b.confidence })
    }
}
