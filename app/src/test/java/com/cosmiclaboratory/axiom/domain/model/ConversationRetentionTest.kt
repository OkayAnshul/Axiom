package com.cosmiclaboratory.axiom.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/**
 * The rule that decides whether a conversation is still yours to pick back up
 * or has become something you wrote. Getting it wrong in either direction is
 * bad in a way the user notices: too eager and it eats a conversation they were
 * mid-way through, too slow and "starting fresh" stops meaning anything.
 */
class ConversationRetentionTest {

    private val lateMonday = LocalDateTime.of(2026, 8, 3, 23, 50)

    @Test
    fun `same day keeps a conversation from earlier the same day`() {
        val morning = LocalDateTime.of(2026, 8, 3, 8, 0)
        val evening = LocalDateTime.of(2026, 8, 3, 22, 0)
        assertFalse(ConversationRetention.SAME_DAY.hasExpired(morning, evening))
    }

    @Test
    fun `same day releases across midnight even minutes later`() {
        // Ten minutes elapsed, but it is a different day, and that is the claim
        // the setting makes. An hours-based rule would keep last night's
        // conversation alive through the whole of today.
        val justAfterMidnight = LocalDateTime.of(2026, 8, 4, 0, 0)
        assertTrue(ConversationRetention.SAME_DAY.hasExpired(lateMonday, justAfterMidnight))
    }

    @Test
    fun `hour windows measure elapsed time, not calendar days`() {
        val tenMinutesLater = lateMonday.plusMinutes(10)
        assertFalse(ConversationRetention.HOURS_6.hasExpired(lateMonday, tenMinutesLater))
        assertTrue(ConversationRetention.HOURS_6.hasExpired(lateMonday, lateMonday.plusHours(6)))
        assertFalse(ConversationRetention.HOURS_24.hasExpired(lateMonday, lateMonday.plusHours(23)))
        assertTrue(ConversationRetention.HOURS_24.hasExpired(lateMonday, lateMonday.plusHours(24)))
    }

    @Test
    fun `three days is three days`() {
        assertFalse(ConversationRetention.DAYS_3.hasExpired(lateMonday, lateMonday.plusDays(2)))
        assertTrue(ConversationRetention.DAYS_3.hasExpired(lateMonday, lateMonday.plusDays(3)))
    }

    @Test
    fun `never expires never expires`() {
        assertFalse(ConversationRetention.NEVER.hasExpired(lateMonday, lateMonday.plusDays(400)))
    }

    /**
     * The bug that motivated anchoring on the last message rather than on the
     * park: viewing a conversation must not buy it another window.
     */
    @Test
    fun `expiry does not move when only the clock advances`() {
        val spoke = LocalDateTime.of(2026, 8, 3, 12, 0)
        val nextDay = LocalDateTime.of(2026, 8, 4, 12, 0)
        val dayAfter = LocalDateTime.of(2026, 8, 5, 12, 0)
        // Expired on the 4th, and still expired on the 5th — opening the app in
        // between cannot make it un-expire, because nothing here depends on when
        // it was looked at.
        assertTrue(ConversationRetention.SAME_DAY.hasExpired(spoke, nextDay))
        assertTrue(ConversationRetention.SAME_DAY.hasExpired(spoke, dayAfter))
    }

    @Test
    fun `storage round trips and unknown values fall back to the default`() {
        ConversationRetention.entries.forEach { value ->
            assert(ConversationRetention.fromStorage(value.storageValue) == value)
        }
        assert(ConversationRetention.fromStorage(null) == ConversationRetention.SAME_DAY)
        assert(ConversationRetention.fromStorage("nonsense") == ConversationRetention.SAME_DAY)
    }
}
