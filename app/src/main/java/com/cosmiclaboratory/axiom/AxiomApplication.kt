package com.cosmiclaboratory.axiom

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.cosmiclaboratory.axiom.data.database.seed.DatabaseSeeder
import com.cosmiclaboratory.axiom.data.notification.AxiomNotifications
import com.cosmiclaboratory.axiom.data.work.JournalWorkScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class AxiomApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var seeder: DatabaseSeeder
    @Inject lateinit var workScheduler: JournalWorkScheduler

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        AxiomNotifications.ensureChannels(this)
        appScope.launch { seeder.seedIfNeeded() }
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
    }
}
