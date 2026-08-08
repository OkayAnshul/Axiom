package com.cosmiclaboratory.axiom.domain.model

import java.time.Duration
import java.time.LocalDateTime

/**
 * How long a parked conversation stays resumable before it is written into the
 * journal and released.
 *
 * The conversation starts blank on every fresh launch either way — this governs
 * only the window in which "pick up where we left off" is still on offer. Past
 * it, the parked stretch is digested (so its memories and its journal entry are
 * kept) and the messages are cleared.
 *
 * [SAME_DAY] is the default because a day is the unit people already think in:
 * a conversation from this morning is still today's, and one from last night is
 * something that happened. Fixed durations exist for people who keep odd hours,
 * where "yesterday" and "six hours ago" are not the same claim at all.
 */
enum class ConversationRetention(
    val storageValue: String,
    val label: String,
    val blurb: String
) {
    SAME_DAY("same_day", "Until the day ends", "Pick it back up any time today"),
    HOURS_6("6h", "6 hours", "A long afternoon"),
    HOURS_24("24h", "24 hours", "A full day either side of now"),
    DAYS_3("3d", "3 days", "Long enough to come back to a hard week"),
    NEVER("never", "Until I clear it", "Nothing is ever cleared for you");

    /**
     * True when a conversation whose last message was at [lastSpokeAt] can no
     * longer be resumed and should be digested away.
     *
     * Measured from the last thing *said*, not from when the conversation was
     * parked or last looked at. Anchoring on the park time meant that opening
     * the app re-parked with a fresh timestamp, so a conversation glanced at
     * once a day survived indefinitely without a word being added to it.
     *
     * [SAME_DAY] compares calendar dates rather than elapsed hours on purpose: a
     * conversation at 23:50 belongs to that night, and a 24-hour rule would keep
     * it alive through the whole of the following day.
     */
    fun hasExpired(lastSpokeAt: LocalDateTime, now: LocalDateTime): Boolean = when (this) {
        NEVER -> false
        SAME_DAY -> lastSpokeAt.toLocalDate() != now.toLocalDate()
        HOURS_6 -> Duration.between(lastSpokeAt, now).toHours() >= 6
        HOURS_24 -> Duration.between(lastSpokeAt, now).toHours() >= 24
        DAYS_3 -> Duration.between(lastSpokeAt, now).toDays() >= 3
    }

    companion object {
        fun fromStorage(value: String?): ConversationRetention =
            entries.firstOrNull { it.storageValue == value } ?: SAME_DAY
    }
}
