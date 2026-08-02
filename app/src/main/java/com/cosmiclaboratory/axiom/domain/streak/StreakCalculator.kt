package com.cosmiclaboratory.axiom.domain.streak

import java.time.LocalDate

/**
 * Pure-function streak math. Operates on the set of dates the user wrote on,
 * relative to a reference date (today). Tested in isolation — no DB, no clock.
 *
 * Definition:
 *  - Current streak = count of consecutive days ending today OR yesterday with at
 *    least one entry. (We allow yesterday so "wrote yesterday but not yet today"
 *    still counts as a streak — opening the app today shouldn't punish the user.)
 *  - Longest streak = max consecutive run anywhere in history.
 */
object StreakCalculator {

    data class Result(val current: Int, val longest: Int, val lastSevenDays: List<Boolean>)

    fun compute(entryDates: Collection<LocalDate>, today: LocalDate = LocalDate.now()): Result {
        if (entryDates.isEmpty()) {
            return Result(0, 0, List(7) { false })
        }
        val days = entryDates.toSortedSet()
        val anchor = if (today in days) today else today.minusDays(1)
        var current = 0
        if (anchor in days) {
            var d = anchor
            while (d in days) {
                current++
                d = d.minusDays(1)
            }
        }
        var longest = 0
        var run = 0
        var prev: LocalDate? = null
        for (d in days) {
            run = if (prev != null && prev.plusDays(1) == d) run + 1 else 1
            if (run > longest) longest = run
            prev = d
        }
        val lastSeven = (6 downTo 0).map { offset -> today.minusDays(offset.toLong()) in days }
        return Result(current, longest, lastSeven)
    }
}
