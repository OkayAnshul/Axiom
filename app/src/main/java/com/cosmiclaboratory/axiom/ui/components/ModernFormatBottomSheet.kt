package com.cosmiclaboratory.axiom.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmiclaboratory.axiom.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernFormatBottomSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    selectedTextStyle: String = "Body",
    selectedFormats: Set<String> = emptySet(),
    onTextStyleChange: (String) -> Unit,
    onFormatToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    
    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = FormatPanelBackground,
            contentColor = FormatPanelText,
            modifier = modifier
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Format",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = FormatPanelText
                        )
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = FormatPanelText
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                MarkdownTextStyleSection(
                    selectedStyle = selectedTextStyle,
                    onStyleChange = { style ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onTextStyleChange(style)
                    }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                
                MarkdownFormattingSection(
                    selectedFormats = selectedFormats,
                    onFormatToggle = { format ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onFormatToggle(format)
                    }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
            }
        }
    }
}

@Composable
private fun MarkdownTextStyleSection(
    selectedStyle: String,
    onStyleChange: (String) -> Unit
) {
    Column {
        Text(
            text = "Text style",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = FormatPanelText
            ),
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        val markdownStyles = listOf(
            MarkdownHeaderStyle("Title", "Lead", 32.sp, FontWeight.Bold),
            MarkdownHeaderStyle("Subtitle", "Sub", 28.sp, FontWeight.Bold),
            MarkdownHeaderStyle("Heading", "Head", 24.sp, FontWeight.SemiBold),
            MarkdownHeaderStyle("Subheading", "Part", 22.sp, FontWeight.SemiBold),
            MarkdownHeaderStyle("Section", "Sec", 18.sp, FontWeight.Medium),
            MarkdownHeaderStyle("Note", "Note", 16.sp, FontWeight.Medium)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(markdownStyles) { style ->
                MarkdownStyleTile(
                    headerStyle = style,
                    isSelected = selectedStyle == style.name,
                    onClick = { onStyleChange(style.name) }
                )
            }
        }
    }
}

@Composable
private fun MarkdownStyleTile(
    headerStyle: MarkdownHeaderStyle,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(84.dp)
            .height(68.dp)
            .toggleable(
                value = isSelected,
                onValueChange = { onClick() }
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) QuickTileActive else FormatPanelSurface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = headerStyle.markdownLevel,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) FormatPanelSelectedText else MaterialTheme.colorScheme.primary
                    )
                )
                
                Text(
                    text = headerStyle.name,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = headerStyle.size * 0.5f,
                        fontWeight = headerStyle.weight,
                        color = if (isSelected) FormatPanelSelectedText else FormatPanelText
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                
                Text(
                    text = "${headerStyle.size.value.toInt()}sp",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isSelected) FormatPanelSelectedText else FormatPanelText.copy(alpha = 0.7f)
                    )
                )
            }
        }
    }
}



@Composable
private fun MarkdownFormattingSection(
    selectedFormats: Set<String>,
    onFormatToggle: (String) -> Unit
) {
    Column {
        Text(
            text = "Insert",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = FormatPanelText
            ),
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Only pure markdown formatting options
        val markdownFormats = listOf(
            MarkdownFormat("quote", Icons.Default.FormatQuote, "Quote"),
            MarkdownFormat("table", Icons.Default.TableChart, "Table"),
            MarkdownFormat("link", Icons.Default.Link, "Link"),
            MarkdownFormat("image", Icons.Default.Image, "Image"),
            MarkdownFormat("bullet_list", Icons.AutoMirrored.Filled.FormatListBulleted, "List"),
            MarkdownFormat("task_list", Icons.Default.CheckBox, "Tasks")
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(markdownFormats) { format ->
                MarkdownFormatTile(
                    format = format,
                    isSelected = selectedFormats.contains(format.id),
                    onClick = { onFormatToggle(format.id) }
                )
            }
        }
    }
}

@Composable
private fun MarkdownFormatTile(
    format: MarkdownFormat,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .size(64.dp)
            .toggleable(
                value = isSelected,
                onValueChange = { onClick() }
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) QuickTileActive else FormatPanelSurface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = format.icon,
                contentDescription = format.label,
                tint = if (isSelected) FormatPanelSelectedText else FormatPanelText,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = format.label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (isSelected) FormatPanelSelectedText else FormatPanelText
                ),
                maxLines = 1
            )
        }
    }
}


// Data classes for markdown formatting options
private data class MarkdownHeaderStyle(
    val name: String,
    val markdownLevel: String,
    val size: androidx.compose.ui.unit.TextUnit,
    val weight: FontWeight
)

private data class MarkdownFormat(
    val id: String,
    val icon: ImageVector,
    val label: String
)
