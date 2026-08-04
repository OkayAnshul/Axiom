package com.cosmiclaboratory.axiom.domain.nlp

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Finds things the user has coming up, so the companion can circle back
 * afterwards — "how did Tuesday go?" — without an API key.
 *
 * This is the keyless half of open loops. With a key the extractor asks a model
 * for follow_up_in_days and does a better job; without one, a small set of date
 * expressions still catches the cases that matter most, which are the ones
 * people actually write: tomorrow, a named weekday, next week, in a few days.
 *
 * It is built to under-report. A missed appointment costs nothing, while a
 * companion asking how a thing went when no such thing existed is unsettling —
 * so anything ambiguous is dropped rather than guessed. That includes the Hindi
 * "kal" and "parso", which mean both directions in time and are unresolvable
 * without context the parser does not have.
 */
object CommitmentDetector {

    data class Commitment(
        /** The sentence it was found in — what the companion will quote back. */
        val sentence: String,
        /** The day the thing happens. */
        val happensOn: LocalDate,
        /** When to ask about it: the day after, so the asking is never premature. */
        val followUpOn: LocalDate
    )

    /** Beyond a season, a "commitment" is really a plan and does not need chasing. */
    const val MAX_HORIZON_DAYS = 90L

    fun detect(text: String, today: LocalDate = LocalDate.now()): List<Commitment> =
        splitSentences(text).mapNotNull { sentence ->
            val lower = sentence.lowercase()
            val date = findFutureDate(lower, today) ?: return@mapNotNull null
            if (date <= today || date > today.plusDays(MAX_HORIZON_DAYS)) return@mapNotNull null
            // A sentence that is nothing but the date names no event, and
            // "Earlier you mentioned: tomorrow. How did that go?" is nonsense.
            if (!namesSomething(lower)) return@mapNotNull null
            Commitment(
                sentence = sentence.trim(),
                happensOn = date,
                followUpOn = date.plusDays(1)
            )
        }.distinctBy { it.sentence }

    /** True when something remains once the date vocabulary is taken out. */
    internal fun namesSomething(lower: String): Boolean =
        lower.split(Regex("[^\\p{L}\\p{M}\\p{Nd}]+"))
            .any { word -> word.length >= 3 && word !in DATE_WORDS }

    /**
     * First matching expression wins, most specific first. Everything here must
     * be unambiguously in the future — "on Monday" is read as the coming Monday,
     * which is how people use it when writing about plans.
     */
    internal fun findFutureDate(lower: String, today: LocalDate): LocalDate? {
        RELATIVE_DAYS.forEach { (pattern, offset) ->
            if (pattern.containsMatchIn(lower)) return today.plusDays(offset)
        }

        IN_N_UNITS.find(lower)?.let { match ->
            val amount = match.groupValues[1].toLongOrNull() ?: return@let
            return when (match.groupValues[2]) {
                "day", "days" -> today.plusDays(amount)
                "week", "weeks" -> today.plusWeeks(amount)
                "month", "months" -> today.plusMonths(amount)
                else -> null
            }
        }

        WEEKDAYS.forEach { (name, day) ->
            // "next friday" and "on friday" both mean the coming one; requiring a
            // preposition or "next" avoids matching a diary heading like "Friday:".
            if (Regex("\\b(on|next|this coming|coming)\\s+$name\\b").containsMatchIn(lower)) {
                return today.with(TemporalAdjusters.next(day))
            }
        }

        if (NEXT_WEEK.containsMatchIn(lower)) return today.plusWeeks(1)
        if (NEXT_MONTH.containsMatchIn(lower)) return today.plusMonths(1)
        if (WEEKEND.containsMatchIn(lower)) return today.with(TemporalAdjusters.next(DayOfWeek.SATURDAY))

        return null
    }

    /** Sentence-ish split that also treats newlines as boundaries. */
    internal fun splitSentences(text: String): List<String> =
        text.split(SENTENCE_END).map { it.trim() }.filter { it.length >= MIN_SENTENCE_LENGTH }

    private const val MIN_SENTENCE_LENGTH = 8

    /** Words that only locate an event in time; they cannot BE the event. */
    private val DATE_WORDS = setOf(
        "tomorrow", "day", "after", "next", "this", "coming", "the",
        "week", "weeks", "month", "months", "days", "weekend",
        "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday",
        "agle", "hafte"
    )

    private val SENTENCE_END = Regex("[.!?।\\n]+")

    // "tonight" is deliberately absent: it resolves to today, and there is
    // nothing to circle back to on a day that has not ended.
    private val RELATIVE_DAYS: List<Pair<Regex, Long>> = listOf(
        Regex("\\bday after tomorrow\\b") to 2L,
        Regex("\\btomorrow\\b") to 1L
    )

    private val IN_N_UNITS = Regex("\\bin (\\d{1,2}) (day|days|week|weeks|month|months)\\b")

    private val NEXT_WEEK = Regex("\\b(next week|agle hafte)\\b")
    private val NEXT_MONTH = Regex("\\bnext month\\b")
    private val WEEKEND = Regex("\\b(this|the) weekend\\b")

    private val WEEKDAYS: List<Pair<String, DayOfWeek>> = listOf(
        "monday" to DayOfWeek.MONDAY,
        "tuesday" to DayOfWeek.TUESDAY,
        "wednesday" to DayOfWeek.WEDNESDAY,
        "thursday" to DayOfWeek.THURSDAY,
        "friday" to DayOfWeek.FRIDAY,
        "saturday" to DayOfWeek.SATURDAY,
        "sunday" to DayOfWeek.SUNDAY
    )
}
