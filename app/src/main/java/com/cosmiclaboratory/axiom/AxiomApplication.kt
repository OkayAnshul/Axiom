package com.cosmiclaboratory.axiom

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.cosmiclaboratory.axiom.data.database.seed.DatabaseSeeder
import com.cosmiclaboratory.axiom.data.notification.AxiomNotifications
import com.cosmiclaboratory.axiom.data.repository.JournalRepository
import com.cosmiclaboratory.axiom.data.work.JournalWorkScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.app.Activity
import android.os.Bundle
import com.cosmiclaboratory.axiom.data.companion.ConversationDigester

@HiltAndroidApp
class AxiomApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var seeder: DatabaseSeeder
    @Inject lateinit var workScheduler: JournalWorkScheduler
    @Inject lateinit var journal: JournalRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        AxiomNotifications.ensureChannels(this)
        appScope.launch { seeder.seedIfNeeded() }
        // Anything written before back-exits-as-draft was fixed is still sitting
        // as a draft, which the timeline hides. Bring it back into the story.
        appScope.launch { runCatching { journal.completeAbandonedDrafts() } }
        // Daily batch of personalized companion openers. This was written long
        // ago but never scheduled — the "personalized prompt" path could never
        // fire until this line existed.
        workScheduler.schedulePeriodicInitiatorBatch()
        // The companion's hourly tick. Enqueued with KEEP, and the worker itself
        // no-ops when both proactive settings are off, so this is safe to call
        // on every launch.
        workScheduler.scheduleProactiveCheckIns()
        // Weekly on-device tidy-up of long-term memory.
        workScheduler.scheduleMemoryConsolidation()
        digestWhenBackgrounded()
    }

    /**
     * Digest the conversation shortly after the user leaves the app.
     *
     * Memories only ever form in workers, and the digest was scheduled for three
     * hours after the last message. Someone who talks daily and closes the app
     * accumulates nothing until they happen to stay quiet that long — the
     * feature works, invisibly, far too late to feel like the companion is
     * learning anything.
     *
     * Counted rather than triggered on the first stop, because a rotation stops
     * one activity before starting the next and would otherwise read as leaving.
     * Two minutes rather than none, so switching apps to check something does
     * not write up a half-finished thought; the schedule is REPLACE, so coming
     * back and carrying on simply pushes it out again.
     *
     * No lifecycle-process dependency for this: one counter is the whole
     * mechanism, and it stays correct if a second activity ever appears.
     */
    private fun digestWhenBackgrounded() {
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            private var started = 0

            override fun onActivityStarted(activity: Activity) {
                started++
            }

            override fun onActivityStopped(activity: Activity) {
                started--
                if (started <= 0) {
                    workScheduler.scheduleConversationDigest(
                        threadId = ConversationDigester.THREAD_ID_DEFAULT,
                        delayMinutes = BACKGROUND_DIGEST_MINUTES
                    )
                }
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }

    private companion object {
        /** Long enough that app-switching is not "the conversation ended". */
        const val BACKGROUND_DIGEST_MINUTES = 2L
    }
}
