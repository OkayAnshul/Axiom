package com.cosmiclaboratory.axiom.data.work

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.cosmiclaboratory.axiom.MainActivity
import com.cosmiclaboratory.axiom.R
import com.cosmiclaboratory.axiom.data.database.dao.EntryDao
import com.cosmiclaboratory.axiom.data.notification.AxiomNotifications
import com.cosmiclaboratory.axiom.data.preferences.UserPreferences
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.concurrent.TimeUnit

@HiltWorker
class DailyNudgeWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val prefs: UserPreferences,
    private val entryDao: EntryDao
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (!prefs.dailyNudgeEnabled.first()) return Result.success()
        val today = LocalDate.now()
        val wroteToday = entryDao.forDay(today.atStartOfDay(), today.plusDays(1).atStartOfDay()).isNotEmpty()
        if (wroteToday) return Result.success()
        post(applicationContext)
        return Result.success()
    }

    private fun post(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        AxiomNotifications.ensureChannels(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(context, AxiomNotifications.CHANNEL_REFLECTION)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("How was today?")
            .setContentText("Take a minute to reflect — your companion is listening.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(NUDGE_ID, notif) }
    }

    companion object {
        const val UNIQUE_NAME = "daily-nudge"
        const val NUDGE_ID = 4201

        fun schedule(context: Context, hourOfDay: Int, minute: Int) {
            // Approximate: WorkManager periodic minimum is 15 min, so we run hourly
            // and the worker checks the current time. Simpler than computing initial
            // delay with DST/timezone edge cases.
            val request = PeriodicWorkRequestBuilder<DailyNudgeWorker>(1, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_NAME)
        }
    }
}
