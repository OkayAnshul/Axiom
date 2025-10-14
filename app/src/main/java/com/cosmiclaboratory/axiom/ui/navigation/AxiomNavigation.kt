package com.cosmiclaboratory.axiom.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.cosmiclaboratory.axiom.ui.screens.NoteDetailScreen
import com.cosmiclaboratory.axiom.ui.screens.NotesListScreen
import com.cosmiclaboratory.axiom.ui.screens.SearchScreen

@Composable
fun AxiomNavigation(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = AxiomScreen.NotesList.route,
        enterTransition = { slideInHorizontally(animationSpec = tween(300)) { it } },
        exitTransition = { slideOutHorizontally(animationSpec = tween(300)) { -it } },
        popEnterTransition = { slideInHorizontally(animationSpec = tween(300)) { -it } },
        popExitTransition = { slideOutHorizontally(animationSpec = tween(300)) { it } }
    ) {
        // Notes List Screen (Home)
        composable(
            route = AxiomScreen.NotesList.route,
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            NotesListScreen(
                onNoteClick = { noteId ->
                    navController.navigate(AxiomScreen.NoteDetail.createRoute(noteId))
                },
                onNewNoteClick = {
                    navController.navigate(AxiomScreen.NoteDetail.createRoute())
                },
                onSearchClick = {
                    navController.navigate(AxiomScreen.Search.route)
                }
            )
        }
        
        // Note Detail Screen (Create/Edit)
        composable(
            route = AxiomScreen.NoteDetail.route,
            arguments = AxiomScreen.NoteDetail.arguments
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong(AxiomScreen.NoteDetail.NOTE_ID_ARG)
                ?.takeIf { it != -1L }
            
            NoteDetailScreen(
                noteId = noteId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Search Screen
        composable(
            route = AxiomScreen.Search.route,
            enterTransition = { slideInHorizontally(animationSpec = tween(300)) { it } },
            exitTransition = { slideOutHorizontally(animationSpec = tween(300)) { it } }
        ) {
            SearchScreen(
                onNoteClick = { noteId ->
                    navController.navigate(AxiomScreen.NoteDetail.createRoute(noteId)) {
                        popUpTo(AxiomScreen.NotesList.route)
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}