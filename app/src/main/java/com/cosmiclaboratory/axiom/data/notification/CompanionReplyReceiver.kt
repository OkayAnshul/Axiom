package com.cosmiclaboratory.axiom.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.cosmiclaboratory.axiom.data.repository.CompanionRepository
import com.cosmiclaboratory.axiom.data.work.JournalWorkScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Answering the companion from the notification shade.
 *
 * The point is not convenience — it is that a check-in you can answer in two
 * seconds is a conversation, and one that makes you open an app is a chore. Most
 * of the days someone does not write are days they never got as far as opening
 * anything.
 *
 * The reply is persisted here and the model's answer is left to
 * [com.cosmiclaboratory.axiom.data.work.CompanionReplyWorker]. A broadcast
 * receiver gets roughly ten seconds before the system considers it hung, which
 * is not enough for a streamed reply, and losing someone's words because a model
 * was slow would be far worse than answering a moment late. Writing locally
 * first also means the message survives even if the worker never runs.
 */
@AndroidEntryPoint
class CompanionReplyReceiver : BroadcastReceiver() {

    @Inject lateinit var companionRepo: CompanionRepository
    @Inject lateinit var scheduler: JournalWorkScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REPLY) return
        val text = RemoteInput.getResultsFromIntent(intent)
            ?.getCharSequence(KEY_REPLY_TEXT)
            ?.toString()
            ?.trim()
            .orEmpty()
        if (text.isEmpty()) return

        val threadId = intent.getStringExtra(EXTRA_THREAD_ID) ?: DEFAULT_THREAD_ID
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)

        // goAsync keeps the process alive across the suspend boundary; without
        // it the receiver returns and the write races teardown.
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching {
                companionRepo.appendUser(threadId, text)
                scheduler.requestCompanionReply(threadId)
            }
            // The shade should stop showing a question that has been answered.
            runCatching {
                NotificationManagerCompat.from(context).cancel(notificationId)
            }
            pending.finish()
        }
    }

    companion object {
        const val ACTION_REPLY = "com.cosmiclaboratory.axiom.action.COMPANION_REPLY"
        const val KEY_REPLY_TEXT = "axiom.reply_text"
        const val EXTRA_THREAD_ID = "axiom.thread_id"
        const val EXTRA_NOTIFICATION_ID = "axiom.notification_id"
        private const val DEFAULT_THREAD_ID = "companion"
    }
}
