package com.cosmiclaboratory.axiom.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cosmiclaboratory.axiom.domain.model.Note
import com.cosmiclaboratory.axiom.ui.theme.NoteMetadataStyle
import com.cosmiclaboratory.axiom.ui.theme.NoteTitleStyle
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Note title
            Text(
                text = note.title.ifEmpty { "Untitled Note" },
                style = NoteTitleStyle,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            if (note.content.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                
                // Note preview
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Metadata row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Last updated
                Text(
                    text = formatLastUpdated(note),
                    style = NoteMetadataStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Tags or other indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (note.isFavorite) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "★",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = NoteMetadataStyle,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    if (note.tags.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "${note.tags.size} tag${if (note.tags.size > 1) "s" else ""}",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = NoteMetadataStyle,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun formatLastUpdated(note: Note): String {
    val now = java.time.LocalDateTime.now()
    val updated = note.updatedAt
    
    return when {
        updated.toLocalDate() == now.toLocalDate() -> {
            "Today ${updated.format(DateTimeFormatter.ofPattern("HH:mm"))}"
        }
        updated.toLocalDate() == now.toLocalDate().minusDays(1) -> {
            "Yesterday ${updated.format(DateTimeFormatter.ofPattern("HH:mm"))}"
        }
        updated.year == now.year -> {
            updated.format(DateTimeFormatter.ofPattern("MMM d"))
        }
        else -> {
            updated.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
        }
    }
}