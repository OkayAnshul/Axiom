package com.cosmiclaboratory.axiom.demo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Screenshot control surface, driven from adb.
 *
 *     adb shell am broadcast -a com.cosmiclaboratory.axiom.DEMO_SEED
 *     adb shell am broadcast -a com.cosmiclaboratory.axiom.DEMO_CLEAR
 *
 * A receiver rather than a hidden gesture in Settings, because it is declared in
 * `src/debug/AndroidManifest.xml` and so is merged into debug builds only. That
 * keeps `src/main` completely untouched by the demo tooling — no build-type
 * branch to read past, nothing for R8 to shrink out of the release bundle, and
 * no demo prose sitting in the binary a reviewer downloads.
 *
 * It also makes a screenshot run scriptable, which matters more than it sounds:
 * the shots have to be retaken every time the UI moves, and a manual setup that
 * takes twenty minutes gets skipped.
 *
 * It has to be exported to be reachable at all: `am broadcast` runs as the shell
 * user, and an unexported receiver accepts broadcasts only from its own UID —
 * the broadcast is accepted by ActivityManager, enqueued, and then dropped at
 * delivery with nothing in the log to say why. Exporting is safe only because
 * this component is absent from release builds entirely.
 */
@AndroidEntryPoint
class DemoSeedReceiver : BroadcastReceiver() {

    @Inject
    lateinit var seeder: DemoDataSeeder

    /**
     * [goAsync] rather than a plain launch: seeding writes a few hundred rows,
     * which comfortably outlives `onReceive`, and without the pending result the
     * process is a candidate for death the moment this method returns.
     */
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                when (intent.action) {
                    ACTION_SEED -> {
                        seeder.seed()
                        Log.i(TAG, "Demo data seeded.")
                    }

                    ACTION_CLEAR -> {
                        seeder.clear()
                        Log.i(TAG, "Demo data cleared.")
                    }

                    else -> Log.w(TAG, "Ignoring unknown action: ${intent.action}")
                }
            } catch (e: Exception) {
                // Debug tooling: report loudly, never take the app down with it.
                Log.e(TAG, "Demo seeding failed.", e)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "DemoSeed"
        const val ACTION_SEED = "com.cosmiclaboratory.axiom.DEMO_SEED"
        const val ACTION_CLEAR = "com.cosmiclaboratory.axiom.DEMO_CLEAR"
    }
}
