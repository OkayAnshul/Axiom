package com.cosmiclaboratory.axiom.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.cosmiclaboratory.axiom.ui.components.LocalSnackbarHostState
import com.cosmiclaboratory.axiom.ui.screens.DeveloperScreen
import com.cosmiclaboratory.axiom.ui.screens.NoteDetailScreen
import com.cosmiclaboratory.axiom.ui.screens.NoteReaderScreen
import com.cosmiclaboratory.axiom.ui.screens.NotesListScreen
import com.cosmiclaboratory.axiom.ui.screens.SearchScreen
import com.cosmiclaboratory.axiom.ui.screens.companion.CompanionScreen
import com.cosmiclaboratory.axiom.ui.screens.journal.AnswerCaptureScreen
import com.cosmiclaboratory.axiom.ui.screens.journal.InsightsScreen
import com.cosmiclaboratory.axiom.ui.screens.journal.JournalHistoryScreen
import com.cosmiclaboratory.axiom.ui.screens.journal.JournalSettingsScreen
import com.cosmiclaboratory.axiom.ui.screens.today.TodayScreen
import com.cosmiclaboratory.axiom.utils.ShareIntentHandler

private val TAB_ROUTES = setOf(
    AxiomScreen.Today.route,
    AxiomScreen.Library.route,
    AxiomScreen.Companion.route
)

@Composable
fun AxiomNavigation(
    navController: NavHostController,
    sharedContent: ShareIntentHandler.SharedContent? = null,
    onSharedContentConsumed: () -> Unit = {}
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in TAB_ROUTES
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == AxiomScreen.Today.route,
                        onClick = { navigateTab(navController, AxiomScreen.Today.route) },
                        icon = { Icon(Icons.Filled.WbSunny, contentDescription = null) },
                        label = { Text("Today") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == AxiomScreen.Library.route,
                        onClick = { navigateTab(navController, AxiomScreen.Library.route) },
                        icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null) },
                        label = { Text("Library") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == AxiomScreen.Companion.route,
                        onClick = { navigateTab(navController, AxiomScreen.Companion.route) },
                        icon = { Icon(Icons.Filled.AutoAwesome, contentDescription = null) },
                        label = { Text("Companion") }
                    )
                }
            }
        }
    ) { padding ->
        CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
        NavHost(
            navController = navController,
            startDestination = AxiomScreen.Today.route,
            modifier = Modifier.padding(padding),
            enterTransition = { slideInHorizontally(animationSpec = tween(300)) { it } },
            exitTransition = { slideOutHorizontally(animationSpec = tween(300)) { -it } },
            popEnterTransition = { slideInHorizontally(animationSpec = tween(300)) { -it } },
            popExitTransition = { slideOutHorizontally(animationSpec = tween(300)) { it } }
        ) {
            composable(
                route = AxiomScreen.Today.route,
                enterTransition = { fadeIn(animationSpec = tween(300)) },
                exitTransition = { fadeOut(animationSpec = tween(300)) }
            ) {
                TodayScreen(
                    onNewEntry = { navController.navigate(AxiomScreen.NoteDetail.createRoute()) },
                    onVoiceEntry = { navController.navigate(AxiomScreen.NoteDetail.createRoute()) },
                    onOpenEntry = { id -> navController.navigate(AxiomScreen.NoteDetail.createRoute(id)) }
                )
            }

            composable(
                route = AxiomScreen.Library.route,
                enterTransition = { fadeIn(animationSpec = tween(300)) },
                exitTransition = { fadeOut(animationSpec = tween(300)) }
            ) {
                NotesListScreen(
                    onNoteClick = { id -> navController.navigate(AxiomScreen.NoteDetail.createRoute(id)) },
                    onNewNoteClick = { navController.navigate(AxiomScreen.NoteDetail.createRoute()) },
                    onSearchClick = { navController.navigate(AxiomScreen.Search.route) },
                    onReaderClick = { id -> navController.navigate(AxiomScreen.NoteReader.createRoute(id)) },
                    onTutorialClick = { /* tutorial removed in v2 — see MarkdownCheatsheet sheet in editor */ },
                    onDeveloperClick = { navController.navigate(AxiomScreen.Developer.route) }
                )
            }

            composable(
                route = AxiomScreen.Companion.route,
                enterTransition = { fadeIn(animationSpec = tween(300)) },
                exitTransition = { fadeOut(animationSpec = tween(300)) }
            ) {
                CompanionScreen()
            }

            // Journal flows kept for backwards-compat with existing prompts/answers.
            composable(
                route = AxiomScreen.AnswerCapture.route,
                arguments = AxiomScreen.AnswerCapture.arguments
            ) {
                AnswerCaptureScreen(
                    onBack = { navController.popBackStack() },
                    onCompleted = { entryId ->
                        navController.navigate(AxiomScreen.Insights.createRoute(entryId)) {
                            popUpTo(AxiomScreen.Today.route)
                        }
                    }
                )
            }

            composable(
                route = AxiomScreen.Insights.route,
                arguments = AxiomScreen.Insights.arguments
            ) { InsightsScreen(onBack = { navController.popBackStack() }) }

            composable(route = AxiomScreen.History.route) {
                JournalHistoryScreen(
                    onBack = { navController.popBackStack() },
                    onEntryClick = { id -> navController.navigate(AxiomScreen.Insights.createRoute(id)) }
                )
            }

            composable(route = AxiomScreen.JournalSettings.route) {
                JournalSettingsScreen(onBack = { navController.popBackStack() })
            }

            composable(
                route = AxiomScreen.NoteDetail.route,
                arguments = AxiomScreen.NoteDetail.arguments
            ) { backStackEntry ->
                val noteId = backStackEntry.arguments?.getLong(AxiomScreen.NoteDetail.NOTE_ID_ARG)
                    ?.takeIf { it != -1L }
                NoteDetailScreen(
                    noteId = noteId,
                    sharedContent = sharedContent,
                    onNavigateBack = { navController.popBackStack() },
                    onSharedContentConsumed = onSharedContentConsumed
                )
            }

            composable(
                route = AxiomScreen.NoteReader.route,
                arguments = AxiomScreen.NoteReader.arguments
            ) { backStackEntry ->
                val noteId = backStackEntry.arguments?.getLong(AxiomScreen.NoteReader.NOTE_ID_ARG) ?: 0L
                NoteReaderScreen(
                    noteId = noteId,
                    onNavigateBack = { navController.popBackStack() },
                    onEditNote = {
                        navController.navigate(AxiomScreen.NoteDetail.createRoute(noteId)) {
                            popUpTo(AxiomScreen.Library.route)
                        }
                    }
                )
            }

            composable(
                route = AxiomScreen.Search.route,
                enterTransition = { slideInHorizontally(animationSpec = tween(300)) { it } },
                exitTransition = { slideOutHorizontally(animationSpec = tween(300)) { it } }
            ) {
                SearchScreen(
                    onNoteClick = { id ->
                        navController.navigate(AxiomScreen.NoteDetail.createRoute(id)) {
                            popUpTo(AxiomScreen.Library.route)
                        }
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = AxiomScreen.Developer.route,
                enterTransition = { slideInHorizontally(animationSpec = tween(300)) { it } },
                exitTransition = { slideOutHorizontally(animationSpec = tween(300)) { it } }
            ) {
                DeveloperScreen(onNavigateBack = { navController.popBackStack() })
            }
        }
        }
    }
}

private fun navigateTab(navController: NavHostController, route: String) {
    if (navController.currentDestination?.route == route) return
    navController.navigate(route) {
        popUpTo(AxiomScreen.Today.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
