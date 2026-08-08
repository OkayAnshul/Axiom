package com.cosmiclaboratory.axiom

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.cosmiclaboratory.axiom.ui.navigation.AxiomDeepLinks
import com.cosmiclaboratory.axiom.ui.navigation.AxiomNavigation
import com.cosmiclaboratory.axiom.ui.theme.AxiomTheme
import com.cosmiclaboratory.axiom.ui.viewmodels.StartupViewModel
import com.cosmiclaboratory.axiom.utils.ShareIntentHandler
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.runtime.CompositionLocalProvider
import com.cosmiclaboratory.axiom.ui.design.LocalCompanionName

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val startupViewModel: StartupViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate() so the system splash owns the first frame.
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Hold the splash only until preferences resolve — no fixed delay.
        splashScreen.setKeepOnScreenCondition { !startupViewModel.state.value.isReady }

        enableEdgeToEdge()

        setContent {
            val startup by startupViewModel.state.collectAsStateWithLifecycle()

            /*
             * Nothing is composed until preferences have actually resolved.
             *
             * StartupState.onboardingComplete defaults to false, which makes "not
             * onboarded" indistinguishable from "haven't looked yet". Feeding that
             * provisional value into the nav graph meant the first composition
             * always rooted at Onboarding and laid out the name page behind the
             * splash. The splash is torn down on the very next draw after isReady
             * flips, but swapping the graph costs another frame plus a 260ms
             * dissolve — so a returning user saw the name page flash past.
             *
             * Returning early also keeps the deep-link effect below from running
             * against a graph that is about to be replaced: the replacement pops
             * the back stack, which is how a QS-tile or widget launch could lose
             * its destination and land on the conversation instead.
             */
            if (!startup.isReady) return@setContent

            AxiomTheme(
                themeMode = startup.themeMode,
                adaptiveLight = startup.adaptiveLight
            ) {
              CompositionLocalProvider(LocalCompanionName provides startup.companionName) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    /*
                     * Entry points (QS tile, widgets, share sheet, notifications) are
                     * normalised into a deep link and handed to the nav controller.
                     * The previous version branched on intent extras and called
                     * navigate() with a popUpTo targeting route "notes_list", which
                     * had not existed since the rename to "library" — so the pop
                     * silently matched nothing.
                     */
                    LaunchedEffect(Unit) {
                        intent?.toAxiomDeepLink()?.let { link ->
                            navController.navigate(Uri.parse(link))
                        }
                    }

                    AxiomNavigation(
                        navController = navController,
                        startOnboarding = !startup.onboardingComplete
                    )
                }
              }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

/**
 * Normalises every external entry point into a deep link, or null to land on the
 * default start destination.
 */
private fun Intent.toAxiomDeepLink(): String? {
    // Already a deep link (widget / notification PendingIntent).
    data?.takeIf { it.scheme == AxiomDeepLinks.SCHEME }?.let { return it.toString() }

    // Quick Settings tile.
    if (getStringExtra("action") == "create_note") return AxiomDeepLinks.COMPOSER

    // Share sheet: carry the shared text straight into a new entry.
    ShareIntentHandler.extractSharedContent(this)?.let { shared ->
        val text = Uri.encode(shared.text.take(4000))
        return "${AxiomDeepLinks.COMPOSER}?initialText=$text"
    }
    return null
}
