package com.cosmiclaboratory.axiom.domain.notification

/**
 * When it is welcome to hear from the companion.
 *
 * Four named times rather than a clock face, because this is a preference about
 * the shape of someone's day and not an alarm — delivery is approximate either
 * way, since the check-in rides a WorkManager tick and not an exact alarm.
 *
 * Lives here rather than inside a screen because onboarding and settings both
 * ask the question, and they used to disagree: onboarding offered a yes/no with
 * no time at all, so anyone who said yes there silently got 21:00 and no way to
 * discover it. One list, asked twice.
 */
object CheckInTimes {

    data class Choice(val minuteOfDay: Int, val label: String)

    val ALL: List<Choice> = listOf(
        Choice(9 * 60, "Morning"),
        Choice(13 * 60, "Midday"),
        Choice(18 * 60, "Evening"),
        Choice(21 * 60, "Night")
    )

    /** What a user gets if they enable check-ins without expressing a preference. */
    const val DEFAULT_MINUTE_OF_DAY = 21 * 60

    /**
     * How far either side of the chosen time a check-in may still arrive.
     *
     * Sixty minutes wide in total, matching the worker's hourly period, so
     * exactly one undeferred tick lands inside it.
     */
    const val WINDOW_MINUTES = 30

    /** The last tick of the day, for a check-in the window already missed. */
    const val LAST_CALL_MINUTE = 23 * 60

    /**
     * Whether a check-in may go out now, given the user's chosen time.
     *
     * The old rule was "at or after the chosen minute", which sounds forgiving
     * and is: a 21:00 check-in deferred by Doze could land at 23:40, and "how
     * was today?" at twenty to midnight reads as an app that has not been
     * paying attention. A window keeps the lateness inside the meaning of the
     * word they picked.
     *
     * [LAST_CALL_MINUTE] is the concession: a check-in that never arrives is
     * worse than a late one, so a missed window still gets one attempt before
     * the day rolls over. The one-a-day cap in ProactiveMessenger is what keeps
     * that from becoming a second message.
     *
     * Lives here rather than in the worker so it can be tested without a
     * WorkManager, and beside the times it is a rule about.
     */
    fun shouldSendAt(minuteOfDay: Int, target: Int): Boolean = when {
        minuteOfDay < target - WINDOW_MINUTES -> false
        minuteOfDay <= target + WINDOW_MINUTES -> true
        else -> minuteOfDay >= LAST_CALL_MINUTE
    }
}
