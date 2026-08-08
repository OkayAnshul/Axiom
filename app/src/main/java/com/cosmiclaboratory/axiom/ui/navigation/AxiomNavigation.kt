package com.cosmiclaboratory.axiom.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navDeepLink
import com.cosmiclaboratory.axiom.ui.components.LocalSnackbarHostState
import com.cosmiclaboratory.axiom.ui.design.LocalNavAnimatedScope
import com.cosmiclaboratory.axiom.ui.design.LocalSharedTransitionScope
import com.cosmiclaboratory.axiom.ui.design.components.AxiomEmptyState
import com.cosmiclaboratory.axiom.ui.design.components.AxiomPlaceBarHeight
import com.cosmiclaboratory.axiom.ui.screens.companion.CompanionScreen
import com.cosmiclaboratory.axiom.ui.screens.onboarding.OnboardingScreen
import com.cosmiclaboratory.axiom.ui.screens.patterns.PatternsScreen
import com.cosmiclaboratory.axiom.ui.screens.settings.SettingsAiScreen
import com.cosmiclaboratory.axiom.ui.screens.settings.SettingsAppearanceScreen
import com.cosmiclaboratory.axiom.ui.screens.settings.SettingsScreen
import com.cosmiclaboratory.axiom.ui.screens.settings.SettingsVoiceScreen
import com.cosmiclaboratory.axiom.ui.screens.settings.SettingsHowITalkScreen
import com.cosmiclaboratory.axiom.ui.screens.backup.BackupScreen
import com.cosmiclaboratory.axiom.ui.screens.calendar.CalendarScreen
import com.cosmiclaboratory.axiom.ui.screens.composer.ComposerScreen
import com.cosmiclaboratory.axiom.ui.screens.journal.JournalScreen
import com.cosmiclaboratory.axiom.ui.screens.memory.MemoryScreen
import com.cosmiclaboratory.axiom.ui.screens.search.SearchScreen
import com.cosmiclaboratory.axiom.ui.screens.talks.TalksScreen
import com.cosmiclaboratory.axiom.ui.screens.reader.ReaderScreen
import com.cosmiclaboratory.axiom.ui.theme.AxiomTheme

/**
 * There is no bottom navigation bar.
 *
 * A permanent three-tab bar asserted that Journal and Patterns are peers of the
 * conversation, always worth a glance. They are not — they are things the two of
 * you have made, visited occasionally. The conversation now owns the whole
 * screen, and everything else is one tap away behind [ShelfSheet], where each
 * destination gets a label *and* a sentence rather than an icon and one word.
 *
 * The route graph below is unchanged, so every deep link still resolves.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AxiomNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startOnboarding: Boolean = false
) {
    val snackbarHostState = remember { SnackbarHostState() }
    // Captured here because the transition lambdas below are not composable.
    val dissolveSpec = AxiomTheme.motion.dissolve
    val slideSpec = AxiomTheme.motion.pageSlide

    /*
     * Going down into something, and coming back out.
     *
     * Every destination used to share one crossfade, which is right for the two
     * peer surfaces and wrong for everything else: opening an entry, the
     * composer or a settings page is descending from somewhere, and back returns
     * you there. A dissolve says all sixteen destinations are equally far away.
     *
     * The fade rides along with the slide because a slide alone reveals the
     * outgoing page's edge against the incoming one, and both pages here draw
     * the same canvas colour.
     */
    val enterDeeper: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideIntoContainer(SlideDirection.Start, slideSpec) + fadeIn(dissolveSpec)
    }
    val exitDeeper: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutOfContainer(SlideDirection.Start, slideSpec) + fadeOut(dissolveSpec)
    }
    val popEnterShallower: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideIntoContainer(SlideDirection.End, slideSpec) + fadeIn(dissolveSpec)
    }
    val popExitShallower: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutOfContainer(SlideDirection.End, slideSpec) + fadeOut(dissolveSpec)
    }

    Scaffold(
        modifier = modifier,
        containerColor = AxiomTheme.colors.canvas,
        // The conversation draws its own ambient background edge to edge, so the
        // scaffold must not reserve inset padding on its behalf.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        // Which leaves the snackbar with nothing to measure against: with zero
        // insets and no bottomBar here, Material puts it flush at the window
        // edge — under the place bar, over the gesture handle. Both of the
        // app's snackbars carry an action ("Read it", "Undo"), so being
        // unreachable is not cosmetic. The bar lives inside the NavHost
        // content, so it can only be cleared explicitly.
        snackbarHost = {
            SnackbarHost(
                snackbarHostState,
                Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = AxiomPlaceBarHeight)
            )
        }
    ) { padding ->
        // One layout for the whole graph: shared elements can only match across
        // destinations that live under the same SharedTransitionLayout, and the
        // pair this exists for — a card in the timeline and the entry it opens —
        // are two different destinations by definition.
        SharedTransitionLayout {
        CompositionLocalProvider(
            LocalSnackbarHostState provides snackbarHostState,
            LocalSharedTransitionScope provides this@SharedTransitionLayout
        ) {
            NavHost(
                navController = navController,
                startDestination = if (startOnboarding) Onboarding else Main,
                modifier = Modifier.padding(padding),
                // The DEFAULT is the dissolve, and it belongs to the peer
                // surfaces: between the conversation and the story neither a
                // sequence nor a hierarchy is being claimed. Destinations you
                // descend into override it above.
                enterTransition = { fadeIn(dissolveSpec) },
                exitTransition = { fadeOut(dissolveSpec) },
                popEnterTransition = { fadeIn(dissolveSpec) },
                popExitTransition = { fadeOut(dissolveSpec) }
            ) {
                composable<Onboarding> {
                    OnboardingScreen(
                        onFinished = {
                            navController.navigate(Main) {
                                popUpTo(Onboarding) { inclusive = true }
                            }
                        }
                    )
                }

                navigation<Main>(startDestination = Companion) {
                    composable<Companion>(
                        deepLinks = listOf(
                            navDeepLink { uriPattern = AxiomDeepLinks.COMPANION },
                            // Legacy alias: placed widgets/tiles still point here.
                            navDeepLink { uriPattern = AxiomDeepLinks.TODAY }
                        )
                    ) {
                        CompanionScreen(
                            onOpenEntry = { id -> navController.navigate(Reader(id)) },
                            onConnectAi = { navController.navigate(SettingsAi) },
                            onOpenMemories = { navController.navigate(Memories) },
                            onOpenSettings = { navController.navigate(Settings) },
                            onOpenJournal = { navController.navigate(Journal) },
                            onOpenPatterns = { navController.navigate(Patterns) },
                            onOpenTalks = { navController.navigate(Talks) },
                            onSaveToJournal = { text ->
                                navController.navigate(Composer(initialText = text))
                            },
                            // Passed as a prompt rather than pasted into the body:
                            // the composer heads the entry with it, saves it as
                            // promptSnapshot, and files the result under
                            // Prompted instead of Written.
                            onWriteAbout = { prompt, questionId ->
                                // The id is what lets the saved entry be credited
                                // to the question; without it the bank never
                                // learns what has been answered.
                                navController.navigate(
                                    Composer(promptText = prompt, questionId = questionId)
                                )
                            },
                            onContinueDraft = { id ->
                                navController.navigate(Composer(entryId = id))
                            }
                        )
                    }

                    composable<Journal>(
                        deepLinks = listOf(navDeepLink { uriPattern = AxiomDeepLinks.JOURNAL })
                    ) {
                        CompositionLocalProvider(LocalNavAnimatedScope provides this) {
                        JournalScreen(
                            onOpenEntry = { id -> navController.navigate(Reader(id)) },
                            onNewEntry = { navController.navigate(Composer()) },
                            onSearch = { navController.navigate(Search()) },
                            onOpenPatterns = { navController.navigate(Patterns) },
                            onOpenTalks = { navController.navigate(Talks) },
                            onOpenMemories = { navController.navigate(Memories) },
                            onOpenSettings = { navController.navigate(Settings) },
                            onCalendar = { navController.navigate(Calendar) },
                            onBackToCompanion = { navController.popBackStack() }
                        )
                        }
                    }

                    composable<Patterns>(
                        deepLinks = listOf(navDeepLink { uriPattern = AxiomDeepLinks.PATTERNS })
                    ) {
                        PatternsScreen(onBackToCompanion = { navController.popBackStack() })
                    }

                }

                composable<Composer>(
                    enterTransition = enterDeeper,
                    exitTransition = exitDeeper,
                    popEnterTransition = popEnterShallower,
                    popExitTransition = popExitShallower,
                    deepLinks = listOf(
                        navDeepLink { uriPattern = AxiomDeepLinks.COMPOSER },
                        navDeepLink { uriPattern = "${AxiomDeepLinks.COMPOSER}?voice={voice}" },
                        // "Write about this", straight from a notification.
                        navDeepLink {
                            uriPattern = "${AxiomDeepLinks.COMPOSER}?promptText={promptText}"
                        }
                    )
                ) {
                    ComposerScreen(
                        onBack = { navController.popBackStack() },
                        onDone = { navController.popBackStack() }
                    )
                }

                composable<Reader>(
                    deepLinks = listOf(navDeepLink { uriPattern = "${AxiomDeepLinks.SCHEME}://reader/{entryId}" })
                ) {
                    CompositionLocalProvider(LocalNavAnimatedScope provides this) {
                        ReaderScreen(
                            onBack = { navController.popBackStack() },
                            onEdit = { id -> navController.navigate(Composer(entryId = id)) }
                        )
                    }
                }

                composable<Search>(
                    enterTransition = enterDeeper,
                    exitTransition = exitDeeper,
                    popEnterTransition = popEnterShallower,
                    popExitTransition = popExitShallower,
                ) {
                    SearchScreen(
                        onBack = { navController.popBackStack() },
                        onOpenEntry = { id -> navController.navigate(Reader(id)) },
                        onAskInstead = {
                            navController.navigate(Companion) { popUpTo(Main) }
                        }
                    )
                }
                composable<Calendar>(
                    enterTransition = enterDeeper,
                    exitTransition = exitDeeper,
                    popEnterTransition = popEnterShallower,
                    popExitTransition = popExitShallower,
                ) {
                    CalendarScreen(
                        onBack = { navController.popBackStack() },
                        onOpenEntry = { id -> navController.navigate(Reader(id)) },
                        onWriteForDate = { navController.navigate(Composer()) }
                    )
                }

                composable<Talks>(
                    enterTransition = enterDeeper,
                    exitTransition = exitDeeper,
                    popEnterTransition = popEnterShallower,
                    popExitTransition = popExitShallower,
                ) {
                    TalksScreen(onBackToCompanion = { navController.popBackStack() })
                }

                composable<Memories>(
                    enterTransition = enterDeeper,
                    exitTransition = exitDeeper,
                    popEnterTransition = popEnterShallower,
                    popExitTransition = popExitShallower,
                ) {
                    MemoryScreen(
                        onBack = { navController.popBackStack() },
                        onOpenEntry = { id -> navController.navigate(Reader(id)) }
                    )
                }

                composable<Settings>(
                    enterTransition = enterDeeper,
                    exitTransition = exitDeeper,
                    popEnterTransition = popEnterShallower,
                    popExitTransition = popExitShallower,
                ) {
                    SettingsScreen(
                        onBack = { navController.popBackStack() },
                        onOpenAi = { navController.navigate(SettingsAi) },
                        onOpenAppearance = { navController.navigate(SettingsAppearance) },
                        onOpenMemories = { navController.navigate(Memories) },
                        onOpenVoice = { navController.navigate(SettingsVoice) },
                        onOpenHowITalk = { navController.navigate(SettingsHowITalk) },
                        onOpenBackup = { navController.navigate(Backup) }
                    )
                }
                composable<SettingsAppearance>(
                    enterTransition = enterDeeper,
                    exitTransition = exitDeeper,
                    popEnterTransition = popEnterShallower,
                    popExitTransition = popExitShallower,
                ) {
                    SettingsAppearanceScreen(onBack = { navController.popBackStack() })
                }
                composable<SettingsVoice>(
                    enterTransition = enterDeeper,
                    exitTransition = exitDeeper,
                    popEnterTransition = popEnterShallower,
                    popExitTransition = popExitShallower,
                ) {
                    SettingsVoiceScreen(onBack = { navController.popBackStack() })
                }
                composable<SettingsHowITalk>(
                    enterTransition = enterDeeper,
                    exitTransition = exitDeeper,
                    popEnterTransition = popEnterShallower,
                    popExitTransition = popExitShallower,
                ) {
                    SettingsHowITalkScreen(onBack = { navController.popBackStack() })
                }
                composable<Backup>(
                    enterTransition = enterDeeper,
                    exitTransition = exitDeeper,
                    popEnterTransition = popEnterShallower,
                    popExitTransition = popExitShallower,
                ) {
                    BackupScreen(onBack = { navController.popBackStack() })
                }
                composable<SettingsAi>(
                    enterTransition = enterDeeper,
                    exitTransition = exitDeeper,
                    popEnterTransition = popEnterShallower,
                    popExitTransition = popExitShallower,
                    deepLinks = listOf(navDeepLink { uriPattern = AxiomDeepLinks.SETTINGS_AI })
                ) {
                    SettingsAiScreen(onBack = { navController.popBackStack() })
                }
            }
        }
        }
    }
}


