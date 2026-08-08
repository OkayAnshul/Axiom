package com.cosmiclaboratory.axiom.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cosmiclaboratory.axiom.data.companion.CompanionEngine
import com.cosmiclaboratory.axiom.data.companion.CompanionReplyEvent
import com.cosmiclaboratory.axiom.data.companion.ProactiveMessenger
import com.cosmiclaboratory.axiom.data.repository.CompanionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.toList

/**
 * Answers a reply that arrived from the notification shade.
 *
 * The user's message is already persisted by
 * [com.cosmiclaboratory.axiom.data.notification.CompanionReplyReceiver], so this
 * runs the engine with `persistUserTurn = false` and posts whatever comes back.
 * A receiver cannot do this itself: it has about ten seconds, and a streamed
 * reply routinely takes longer.
 *
 * Failure is silent by design. Without a key, or offline, the message is still
 * saved and waiting in the conversation — a notification reading "I couldn't
 * answer that" would be worse than the answer simply arriving when they next
 * open the app.
 */
@HiltWorker
class CompanionReplyWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val engine: CompanionEngine,
    private val companionRepo: CompanionRepository,
    private val messenger: ProactiveMessenger
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val threadId = inputData.getString(KEY_THREAD_ID) ?: return Result.success()
        val latest = runCatching { companionRepo.latestMessage(threadId) }.getOrNull()
            ?: return Result.success()

        val events = runCatching {
            engine.send(
                threadId = threadId,
                userText = latest.content,
                persistUserTurn = false
            ).toList()
        }.getOrElse { return Result.success() }

        val reply = events.filterIsInstance<CompanionReplyEvent.Done>().lastOrNull()
            ?: return Result.success()

        // Delivered as a notification too, because the person who replied from
        // the shade is by definition not looking at the app.
        runCatching { messenger.notifyReply(reply.fullText) }
        return Result.success()
    }

    companion object {
        const val UNIQUE_NAME = "companion-reply"
        const val KEY_THREAD_ID = "threadId"
    }
}
