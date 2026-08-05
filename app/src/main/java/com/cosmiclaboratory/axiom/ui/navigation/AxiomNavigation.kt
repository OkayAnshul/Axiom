package com.cosmiclaboratory.axiom.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navDeepLink
import com.cosmiclaboratory.axiom.ui.components.LocalSnackbarHostState
import com.cosmiclaboratory.axiom.ui.design.components.AxiomEmptyState
import com.cosmiclaboratory.axiom.ui.screens.companion.CompanionScreen
import com.cosmiclaboratory.axiom.ui.screens.onboarding.OnboardingScreen
import com.cosmiclaboratory.axiom.ui.screens.patterns.PatternsScreen
import com.cosmiclaboratory.axiom.ui.screens.settings.SettingsAiScreen
import com.cosmiclaboratory.axiom.ui.screens.settings.SettingsAppearanceScreen
import com.cosmiclaboratory.axiom.ui.screens.settings.SettingsScreen
import com.cosmiclaboratory.axiom.ui.screens.settings.SettingsVoiceScreen
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
@Composable
fun AxiomNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startOnboarding: Boolean = false
) {
    val snackbarHostState = remember { SnackbarHostState() }
    // Captured here because the transition lambdas below are not composable.
    val dissolveSpec = AxiomTheme.motion.dissolve

    Scaffold(
        modifier = modifier,
        containerColor = AxiomTheme.colors.canvas,
        // The conversation draws its own ambient background edge to edge, so the
        // scaffold must not reserve inset padding on its behalf.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
            NavHost(
                navController = navController,
                startDestination = if (startOnboarding) Onboarding else Main,
                modifier = Modifier.padding(padding),
                // Pages dissolve. Sliding implies a linear sequence and scaling
                // implies a hierarchy; between a conversation and the things it
                // produced, neither is true.
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
                            onWriteAbout = { prompt ->
                                navController.navigate(Composer(promptText = prompt))
                            },
                            onContinueDraft = { id ->
                                navController.navigate(Composer(entryId = id))
                            }
                        )
                    }

                    composable<Journal>(
                        deepLinks = listOf(navDeepLink { uriPattern = AxiomDeepLinks.JOURNAL })
                    ) {
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

                    composable<Patterns>(
                        deepLinks = listOf(navDeepLink { uriPattern = AxiomDeepLinks.PATTERNS })
                    ) {
                        PatternsScreen(onBackToCompanion = { navController.popBackStack() })
                    }

                }

                composable<Composer>(
                    deepLinks = listOf(
                        navDeepLink { uriPattern = AxiomDeepLinks.COMPOSER },
                        navDeepLink { uriPattern = "${AxiomDeepLinks.COMPOSER}?voice={voice}" }
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
                    ReaderScreen(
                        onBack = { navController.popBackStack() },
                        onEdit = { id -> navController.navigate(Composer(entryId = id)) }
                    )
                }

                composable<Search> {
                    SearchScreen(
                        onBack = { navController.popBackStack() },
                        onOpenEntry = { id -> navController.navigate(Reader(id)) },
                        onAskInstead = {
                            navController.navigate(Companion) { popUpTo(Main) }
                        }
                    )
                }
                composable<Calendar> {
                    CalendarScreen(
                        onBack = { navController.popBackStack() },
                        onOpenEntry = { id -> navController.navigate(Reader(id)) },
                        onWriteForDate = { navController.navigate(Composer()) }
                    )
                }

                composable<Talks> {
                    TalksScreen(onBackToCompanion = { navController.popBackStack() })
                }

                composable<Memories> {
                    MemoryScreen(
                        onBack = { navController.popBackStack() },
                        onOpenEntry = { id -> navController.navigate(Reader(id)) }
                    )
                }

                composable<Settings> {
                    SettingsScreen(
                        onBack = { navController.popBackStack() },
                        onOpenAi = { navController.navigate(SettingsAi) },
                        onOpenAppearance = { navController.navigate(SettingsAppearance) },
                        onOpenMemories = { navController.navigate(Memories) },
                        onOpenVoice = { navController.navigate(SettingsVoice) },
                        onOpenBackup = { navController.navigate(Backup) }
                    )
                }
                composable<SettingsAppearance> {
                    SettingsAppearanceScreen(onBack = { navController.popBackStack() })
                }
                composable<SettingsVoice> {
                    SettingsVoiceScreen(onBack = { navController.popBackStack() })
                }
                composable<Backup> {
                    BackupScreen(onBack = { navController.popBackStack() })
                }
                composable<SettingsAi>(
                    deepLinks = listOf(navDeepLink { uriPattern = AxiomDeepLinks.SETTINGS_AI })
                ) {
                    SettingsAiScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}


