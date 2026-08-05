package com.cosmiclaboratory.axiom.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cosmiclaboratory.axiom.data.repository.CompanionRepository
import com.cosmiclaboratory.axiom.data.repository.JournalRepository
import com.cosmiclaboratory.axiom.domain.model.EntryKind
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay

/**
 * Catches up on everything written before a key existed.
 *
 * Conversations already waited correctly — the digest watermark stays put
 * without a key, so connecting one later digests the backlog. Entries had no
 * such thing: anything written before the key was permanently un-summarized,
 * with no memories extracted, no themes, no title. The corpus the companion
 * drew on was silently missing its own history.
 *
 * Deliberately paced rather than fast. A user connecting a key with two hundred
 * entries would otherwise fire two hundred requests at a free tier in a few
 * seconds and get rate-limited into a retry storm. [BATCH] per run with a pause
 * between each, then re-enqueued if more remain.
 */
@HiltWorker
class BackfillWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val journalRepo: JournalRepository,
    private val companionRepo: CompanionRepository,
    private val scheduler: JournalWorkScheduler
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        // Oldest first: the far past is what the companion is most missing, and
        // it is the part the user is least likely to revisit themselves.
        val pending = runCatching { journalRepo.allForIndexing() }
            .getOrDefault(emptyList())
            .filter { it.kind != EntryKind.CONVERSATION }
            .sortedBy { it.createdAt }

        var enqueued = 0
        for (entry in pending) {
            if (enqueued >= BATCH) break
            if (runCatching { journalRepo.getInsight(entry.id) }.getOrNull() != null) continue
            scheduler.enqueueSummarize(entry.id)
            enqueued++
            // Spacing the *enqueue* rather than the request keeps this worker
            // short-lived while still smoothing the burst downstream.
            delay(SPACING_MS)
        }

        // Nudge the conversation digest too: without a key its summary watermark
        // stayed behind while entries were written locally, so there is usually
        // a stretch of conversation the model has never seen.
        runCatching {
            val state = companionRepo.threadState(THREAD_ID)
            if (state.summarizedUpToMessageId < state.digestedUpToMessageId) {
                companionRepo.saveThreadState(
                    state.copy(digestedUpToMessageId = state.summarizedUpToMessageId)
                )
                scheduler.scheduleConversationDigest(THREAD_ID, delayMinutes = 1)
            }
        }

        // More left over: come back rather than doing it all at once.
        if (enqueued >= BATCH) scheduler.enqueueBackfill(delayMinutes = NEXT_RUN_MINUTES)
        return Result.success()
    }

    companion object {
        const val THREAD_ID = "companion"

        /** Entries per run. Free tiers are measured in requests per minute. */
        const val BATCH = 30
        const val SPACING_MS = 3_000L
        const val NEXT_RUN_MINUTES = 15L
    }
}
