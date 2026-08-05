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

    /**
     * Catch-up pass after a key is connected. Unique and KEEP, so connecting,
     * disconnecting and reconnecting does not stack several backfills.
     */
    fun enqueueBackfill(delayMinutes: Long = 0) {
        val request = OneTimeWorkRequestBuilder<BackfillWorker>()
            .setConstraints(networkConstraints())
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .build()
        workManager.enqueueUniqueWork(UNIQUE_BACKFILL, ExistingWorkPolicy.REPLACE, request)
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

    fun enqueueSummarize(entryId: Long) {
        val request = OneTimeWorkRequestBuilder<SummarizeEntryWorker>()
            .setConstraints(networkConstraints())
            .setInputData(workDataOf(SummarizeEntryWorker.KEY_ENTRY_ID to entryId))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .build()
        workManager.enqueueUniqueWork(
            "ai-summarize-$entryId",
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    /**
     * Rolling session-end timer: called on every companion message with
     * REPLACE, so the digest fires exactly once, [delayMinutes] after the last
     * message of a session. A new message resets the clock.
     */
    fun scheduleConversationDigest(threadId: String, delayMinutes: Long = DIGEST_DELAY_MINUTES) {
        val request = OneTimeWorkRequestBuilder<ConversationDigestWorker>()
            .setConstraints(networkConstraints())
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .setInputData(workDataOf(ConversationDigestWorker.KEY_THREAD_ID to threadId))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
            .build()
        workManager.enqueueUniqueWork(
            UNIQUE_CONVERSATION_DIGEST,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    /**
     * On-device mood and commitment reading. No network constraint and no key
     * required — the whole point is that it works for someone who never
     * connects one.
     */
    fun enqueueLocalInsight(entryId: Long) {
        val request = OneTimeWorkRequestBuilder<LocalInsightWorker>()
            .setInputData(workDataOf(LocalInsightWorker.KEY_ENTRY_ID to entryId))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .build()
        workManager.enqueueUniqueWork("local-insight-$entryId", ExistingWorkPolicy.KEEP, request)
    }

    /**
     * The companion's hourly tick. Deliberately has NO network constraint: the
     * keyless check-in is composed locally and must still arrive, and a message
     * that waits for wifi is a message that arrives at the wrong moment.
     */
    fun scheduleProactiveCheckIns() {
        val request = PeriodicWorkRequestBuilder<ProactiveCheckInWorker>(1, TimeUnit.HOURS).build()
        workManager.enqueueUniquePeriodicWork(
            ProactiveCheckInWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun cancelProactiveCheckIns() {
        workManager.cancelUniqueWork(ProactiveCheckInWorker.UNIQUE_NAME)
    }

    /** Weekly memory tidy-up. On-device, so no network constraint. */
    fun scheduleMemoryConsolidation() {
        val request = PeriodicWorkRequestBuilder<MemoryConsolidationWorker>(7, TimeUnit.DAYS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            MemoryConsolidationWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun networkConstraints() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    private companion object {
        const val UNIQUE_PERIODIC_INITIATOR = "journal-initiator-periodic"
        const val UNIQUE_ONESHOT_INITIATOR = "journal-initiator-oneshot"
        const val UNIQUE_BACKFILL = "journal-backfill"
        const val UNIQUE_CONVERSATION_DIGEST = "conversation-digest"
        const val DIGEST_DELAY_MINUTES = 180L
    }
}
