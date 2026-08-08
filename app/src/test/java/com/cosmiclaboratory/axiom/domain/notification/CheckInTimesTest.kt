package com.cosmiclaboratory.axiom.domain.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * When an unprompted message is allowed out.
 *
 * The failure this guards against is not a crash: it is a companion that says
 * "how was today?" at twenty to midnight because the device was dozing at the
 * hour the user actually chose.
 */
class CheckInTimesTest {

    private val night = 21 * 60

    @Test
    fun `fires inside the window either side of the chosen time`() {
        assertTrue(CheckInTimes.shouldSendAt(night, night))
        assertTrue(CheckInTimes.shouldSendAt(night - 30, night))
        assertTrue(CheckInTimes.shouldSendAt(night + 30, night))
    }

    @Test
    fun `stays quiet before the window opens`() {
        assertFalse(CheckInTimes.shouldSendAt(night - 31, night))
        assertFalse(CheckInTimes.shouldSendAt(9 * 60, night))
    }

    @Test
    fun `stays quiet after the window closes, until last call`() {
        // 21:31 through 22:59 is the dead zone: late enough to have missed the
        // window, early enough that waiting for last call is still better than
        // arriving at a time nobody asked for.
        assertFalse(CheckInTimes.shouldSendAt(night + 31, night))
        assertFalse(CheckInTimes.shouldSendAt(22 * 60 + 59, night))
        assertTrue(CheckInTimes.shouldSendAt(CheckInTimes.LAST_CALL_MINUTE, night))
    }

    @Test
    fun `a morning check-in still gets its last call at night`() {
        val morning = 9 * 60
        assertTrue(CheckInTimes.shouldSendAt(morning, morning))
        assertFalse(CheckInTimes.shouldSendAt(14 * 60, morning))
        assertTrue(CheckInTimes.shouldSendAt(23 * 60 + 30, morning))
    }

    /**
     * The window is exactly as wide as the worker's period, so an undeferred
     * hourly tick always finds it. Narrower and a check-in could be skipped
     * entirely on a device that ticks at an unlucky offset.
     */
    @Test
    fun `window spans a full hour so an hourly tick cannot miss it`() {
        assertEquals(60, CheckInTimes.WINDOW_MINUTES * 2)
        val hits = (0 until 60).count { offset ->
            CheckInTimes.shouldSendAt(night - 30 + offset, night)
        }
        assertEquals(60, hits)
    }

    @Test
    fun `every offered time is a real minute of the day`() {
        CheckInTimes.ALL.forEach { choice ->
            assertTrue(choice.label, choice.minuteOfDay in 0 until 24 * 60)
        }
        assertTrue(
            CheckInTimes.ALL.any { it.minuteOfDay == CheckInTimes.DEFAULT_MINUTE_OF_DAY }
        )
    }
}
