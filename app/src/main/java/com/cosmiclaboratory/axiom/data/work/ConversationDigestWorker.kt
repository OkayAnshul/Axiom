package com.cosmiclaboratory.axiom.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cosmiclaboratory.axiom.data.companion.ConversationDigester
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Fires once per conversation session — scheduled with a rolling delay that
 * resets on every message, so it runs ~3h after the user goes quiet.
 *
 * The work itself lives in [ConversationDigester], because clearing a
 * conversation has to do exactly the same thing and cannot wait for a worker.
 * This class is now only the schedule: when to digest, and how to report the
 * outcome back to WorkManager.
 */
@HiltWorker
class ConversationDigestWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val digester: ConversationDigester
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val threadId = inputData.getString(KEY_THREAD_ID) ?: ConversationDigester.THREAD_ID_DEFAULT
        return when (digester.digest(threadId)) {
            ConversationDigester.Outcome.Done,
            ConversationDigester.Outcome.NothingToDo -> Result.success()
            // Rate limit or network: the watermark has not moved, so retrying
            // picks up exactly the same stretch.
            ConversationDigester.Outcome.Retry -> Result.retry()
        }
    }

    companion object {
        const val KEY_THREAD_ID = "thread_id"
    }
}
