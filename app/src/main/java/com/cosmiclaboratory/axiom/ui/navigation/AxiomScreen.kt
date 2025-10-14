package com.cosmiclaboratory.axiom.ui.navigation

import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.navArgument

sealed class AxiomScreen(
    val route: String,
    val arguments: List<NamedNavArgument> = emptyList()
) {
    data object NotesList : AxiomScreen("notes_list")
    
    data object NoteDetail : AxiomScreen(
        route = "note_detail?noteId={noteId}",
        arguments = listOf(
            navArgument("noteId") {
                type = NavType.LongType
                defaultValue = -1L
            }
        )
    ) {
        const val NOTE_ID_ARG = "noteId"
        
        fun createRoute(noteId: Long? = null): String {
            return if (noteId != null) {
                "note_detail?noteId=$noteId"
            } else {
                "note_detail"
            }
        }
    }
    
    data object Search : AxiomScreen("search")
    
    data object Settings : AxiomScreen("settings")
}

// Extension functions for easier navigation
fun AxiomScreen.withArgs(vararg args: String): String {
    return buildString {
        append(route)
        args.forEach { arg ->
            append("/$arg")
        }
    }
}