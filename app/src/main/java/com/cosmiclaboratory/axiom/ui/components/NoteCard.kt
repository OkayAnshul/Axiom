package com.cosmiclaboratory.axiom.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cosmiclaboratory.axiom.domain.model.Note
import com.cosmiclaboratory.axiom.ui.theme.NoteMetadataStyle
import com.cosmiclaboratory.axiom.ui.theme.NoteTitleStyle
import com.cosmiclaboratory.axiom.utils.MarkdownParser
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onReaderClick: (() -> Unit)? = null
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
                
                // Markdown preview
                MarkdownPreview(
                    content = note.content,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Metadata row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side - last updated
                Text(
                    text = formatLastUpdated(note),
                    style = NoteMetadataStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                
                // Center - tags and indicators
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
                
                // Right side - reader button
                if (onReaderClick != null) {
                    IconButton(
                        onClick = onReaderClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Visibility,
                            contentDescription = "Open in Reader",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
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