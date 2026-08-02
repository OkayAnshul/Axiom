package com.cosmiclaboratory.axiom.data.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JournalWorkScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager: WorkManager get() = WorkManager.getInstance(context)

    fun schedulePeriodicInitiatorBatch() {
        val request = PeriodicWorkRequestBuilder<InitiatorPromptBatchWorker>(
            repeatInterval = 1, repeatIntervalTimeUnit = TimeUnit.DAYS
        )
            .setConstraints(networkConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .build()
        workManager.enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC_INITIATOR,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun requestImmediateInitiatorBatch() {
        val request = OneTimeWorkRequestBuilder<InitiatorPromptBatchWorker>()
            .setConstraints(networkConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork(
            UNIQUE_ONESHOT_INITIATOR,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    fun enqueueSummarize(answerEntryId: Long) {
        val request = OneTimeWorkRequestBuilder<SummarizeEntryWorker>()
            .setConstraints(networkConstraints())
            .setInputData(workDataOf(SummarizeEntryWorker.KEY_ENTRY_ID to answerEntryId))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .build()
        workManager.enqueueUniqueWork(
            "ai-summarize-$answerEntryId",
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    private fun networkConstraints() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    private companion object {
        const val UNIQUE_PERIODIC_INITIATOR = "journal-initiator-periodic"
        const val UNIQUE_ONESHOT_INITIATOR = "journal-initiator-oneshot"
    }
}
