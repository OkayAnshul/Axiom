package com.cosmiclaboratory.axiom.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun updateNotesListWidgets() {
        try {
            NotesListWidget().updateAll(context)
        } catch (e: Exception) {
            // Handle update failure silently
        }
    }
    
    suspend fun updateQuickNoteWidgets() {
        try {
            QuickNoteWidget().updateAll(context)
        } catch (e: Exception) {
            // Handle update failure silently
        }
    }
    
    suspend fun updateAllWidgets() {
        updateNotesListWidgets()
        updateQuickNoteWidgets()
    }
}