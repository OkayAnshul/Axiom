package com.cosmiclaboratory.axiom.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.appwidget.cornerRadius
import androidx.glance.text.FontWeight
import com.cosmiclaboratory.axiom.MainActivity
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import java.time.format.DateTimeFormatter

class NotesListWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Get recent notes from repository
        val recentNotes = try {
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                WidgetEntryPoint::class.java
            )
            entryPoint.notesRepository().getAllNotes().first().take(3)
        } catch (e: Exception) {
            emptyList()
        }

        provideContent {
            GlanceTheme {
                NotesListContent(recentNotes)
            }
        }
    }

    @Composable
    private fun NotesListContent(entries: List<com.cosmiclaboratory.axiom.domain.model.Entry>) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color.White))
                .cornerRadius(16.dp)
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "📝 Recent Notes",
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(Color(0xFF1976D2))
                    )
                )
            }
            
            Spacer(modifier = GlanceModifier.height(12.dp))
            
            if (entries.isEmpty()) {
                // Empty state
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "No notes yet",
                        style = TextStyle(
                            fontSize = 14.sp,
                            color = ColorProvider(Color.Gray)
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    Box(
                        modifier = GlanceModifier
                            .background(ColorProvider(Color(0xFF1976D2)))
                            .cornerRadius(8.dp)
                            .clickable(actionStartActivity<MainActivity>())
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Create First Entry",
                            style = TextStyle(
                                fontSize = 12.sp,
                                color = ColorProvider(Color.White)
                            )
                        )
                    }
                }
            } else {
                // Notes list
                Column {
                    entries.forEach { note ->
                        NoteItem(note)
                        if (note != entries.last()) {
                            Spacer(modifier = GlanceModifier.height(8.dp))
                        }
                    }
                    
                    Spacer(modifier = GlanceModifier.height(12.dp))
                    
                    // View all button
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .background(ColorProvider(Color(0xFF1976D2).copy(alpha = 0.1f)))
                            .cornerRadius(8.dp)
                            .clickable(actionStartActivity<MainActivity>())
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "View All Notes",
                            style = TextStyle(
                                fontSize = 12.sp,
                                color = ColorProvider(Color(0xFF1976D2)),
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }
    }
    
    @Composable
    private fun NoteItem(note: com.cosmiclaboratory.axiom.domain.model.Entry) {
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(ColorProvider(Color(0xFFF5F5F5)))
                .cornerRadius(8.dp)
                .clickable(actionStartActivity<MainActivity>())
                .padding(12.dp)
        ) {
            Text(
                text = note.title.ifBlank { "Untitled" },
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = ColorProvider(Color.Black)
                )
            )
            
            if (note.content.isNotBlank()) {
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = note.content.take(80) + if (note.content.length > 80) "..." else "",
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = ColorProvider(Color.Gray)
                    )
                )
            }
            
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = note.updatedAt.format(DateTimeFormatter.ofPattern("MMM dd")),
                style = TextStyle(
                    fontSize = 10.sp,
                    color = ColorProvider(Color.Gray)
                )
            )
        }
    }
}