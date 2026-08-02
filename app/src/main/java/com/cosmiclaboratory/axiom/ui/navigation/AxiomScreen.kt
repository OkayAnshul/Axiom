package com.cosmiclaboratory.axiom.ui.navigation

import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.navArgument

sealed class AxiomScreen(
    val route: String,
    val arguments: List<NamedNavArgument> = emptyList()
) {
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
    
    data object NoteReader : AxiomScreen(
        route = "note_reader/{noteId}",
        arguments = listOf(
            navArgument("noteId") {
                type = NavType.LongType
            }
        )
    ) {
        const val NOTE_ID_ARG = "noteId"
        
        fun createRoute(noteId: Long): String {
            return "note_reader/$noteId"
        }
    }
    
    data object Search : AxiomScreen("search")

    data object Developer : AxiomScreen("developer")

    data object History : AxiomScreen("history")

    data object JournalSettings : AxiomScreen("journal_settings")

    data object AnswerCapture : AxiomScreen(
        route = "answer_capture/{questionId}",
        arguments = listOf(
            navArgument("questionId") { type = NavType.LongType }
        )
    ) {
        const val QUESTION_ID_ARG = "questionId"
        fun createRoute(questionId: Long): String = "answer_capture/$questionId"
    }

    data object Insights : AxiomScreen(
        route = "insights/{entryId}",
        arguments = listOf(
            navArgument("entryId") { type = NavType.LongType }
        )
    ) {
        const val ENTRY_ID_ARG = "entryId"
        fun createRoute(entryId: Long): String = "insights/$entryId"
    }

    // v2 surfaces. Connect-AI is a bottom sheet inside CompanionScreen, not a route.
    data object Today : AxiomScreen("today")
    data object Library : AxiomScreen("library")
    data object Companion : AxiomScreen("companion")
}