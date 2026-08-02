package com.cosmiclaboratory.axiom.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmiclaboratory.axiom.ui.theme.*

@Composable
fun ModernQuickTileBar(
    selectedFormats: Set<String> = emptySet(),
    onFormatToggle: (String) -> Unit,
    onMoreOptionsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    
    val quickFormats = listOf(
        QuickFormat("h2", Icons.Default.Title, "Section"),
        QuickFormat("task_list", Icons.Default.CheckBox, "Task"),
        QuickFormat("quote", Icons.Default.FormatQuote, "Quote"),
        QuickFormat("bullet_list", Icons.AutoMirrored.Filled.FormatListBulleted, "List"),
        QuickFormat("link", Icons.Default.Link, "Link"),
        QuickFormat("more", Icons.Default.MoreHoriz, "More")
    )
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = QuickTileBackground
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            quickFormats.forEach { format ->
                ModernQuickTile(
                    icon = format.icon,
                    label = format.label,
                    isSelected = selectedFormats.contains(format.id),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (format.id == "more") {
                            onMoreOptionsClick()
                        } else {
                            onFormatToggle(format.id)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ModernQuickTile(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icon button with golden selection state
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) QuickTileActive 
                    else Color.Transparent
                )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) QuickTileActiveText else QuickTileText,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Label with selection-aware styling
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = if (isSelected) QuickTileActive else QuickTileText,
            maxLines = 1
        )
    }
}

// Light theme variant
@Composable
fun ModernQuickTileBarLight(
    selectedFormats: Set<String> = emptySet(),
    onFormatToggle: (String) -> Unit,
    onMoreOptionsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    
    val quickFormats = listOf(
        QuickFormat("h2", Icons.Default.Title, "Section"),
        QuickFormat("task_list", Icons.Default.CheckBox, "Task"),
        QuickFormat("quote", Icons.Default.FormatQuote, "Quote"),
        QuickFormat("bullet_list", Icons.AutoMirrored.Filled.FormatListBulleted, "List"),
        QuickFormat("link", Icons.Default.Link, "Link"),
        QuickFormat("more", Icons.Default.MoreHoriz, "More")
    )
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = QuickTileBackgroundLight
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            quickFormats.forEach { format ->
                ModernQuickTileLight(
                    icon = format.icon,
                    label = format.label,
                    isSelected = selectedFormats.contains(format.id),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (format.id == "more") {
                            onMoreOptionsClick()
                        } else {
                            onFormatToggle(format.id)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ModernQuickTileLight(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) QuickTileActive 
                    else Color.Transparent
                )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) QuickTileActiveText else QuickTileTextLight,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = if (isSelected) QuickTileActive else QuickTileTextLight,
            maxLines = 1
        )
    }
}

// Data class for quick format options
private data class QuickFormat(
    val id: String,
    val icon: ImageVector,
    val label: String
)
