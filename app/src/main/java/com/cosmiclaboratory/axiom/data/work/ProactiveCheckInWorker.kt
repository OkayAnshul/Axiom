package com.cosmiclaboratory.axiom.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cosmiclaboratory.axiom.data.companion.ProactiveMessenger
import com.cosmiclaboratory.axiom.data.preferences.UserPreferences
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDateTime

/**
 * The companion's clock. Runs hourly and decides whether there is anything
 * worth saying — WorkManager's 15-minute floor makes exact-time delivery
 * impossible anyway, so the worker gates on time itself.
 *
 * The gate is "at or after the chosen time", not "in the chosen hour": if the
 * device was dozing at 21:00 the message should still arrive at 22:30 rather
 * than be silently skipped. Sending at most one unprompted message a day
 * (enforced in [ProactiveMessenger]) is what keeps that from becoming spam.
 *
 * Replaces DailyNudgeWorker, which was never scheduled by anything, ignored
 * the hour and minute passed to its own schedule(), and would therefore have
 * posted the same static "How was today?" every hour of every day.
 */
@HiltWorker
class ProactiveCheckInWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val prefs: UserPreferences,
    private val messenger: ProactiveMessenger
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val checkInEnabled = prefs.dailyNudgeEnabled.first()
        val recapEnabled = prefs.weeklyRecapEnabled.first()
        if (!checkInEnabled && !recapEnabled) return Result.success()

        val now = LocalDateTime.now()
        val minuteOfDay = now.hour * 60 + now.minute
        if (minuteOfDay < prefs.dailyNudgeMinuteOfDay.first()) return Result.success()

        // Sunday belongs to the recap; the two never arrive on the same day.
        val isRecapDay = now.dayOfWeek == DayOfWeek.SUNDAY
        runCatching {
            when {
                isRecapDay && recapEnabled -> messenger.sendWeeklyRecap(now)
                checkInEnabled -> messenger.sendDailyCheckIn(now)
                else -> false
            }
        }
        // Never retry: a missed check-in is yesterday's news, and the hourly
        // tick will reconsider on its own terms.
        return Result.success()
    }

    companion object {
        const val UNIQUE_NAME = "companion-check-in"
    }
}
