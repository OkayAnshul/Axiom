package com.cosmiclaboratory.axiom.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmiclaboratory.axiom.utils.MarkdownParser

@Composable
fun EnhancedMarkdownPreview(
    content: String,
    modifier: Modifier = Modifier,
    showDebug: Boolean = false,
    enableAnimations: Boolean = true,
    readerMode: Boolean = false,
    useScrollableContainer: Boolean = true
) {
    val elements = remember(content) { 
        MarkdownParser.parse(content).also { 
            if (showDebug) println("DEBUG: Parsed ${it.size} elements from content")
        }
    }
    
    if (content.isBlank()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            EmptyState(readerMode = readerMode)
        }
    } else if (elements.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            ErrorState(content = content, showDebug = showDebug)
        }
    } else {
        if (useScrollableContainer) {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = if (readerMode) 24.dp else 16.dp,
                    vertical = if (readerMode) 32.dp else 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(
                    if (readerMode) 16.dp else 12.dp
                )
            ) {
                if (showDebug) {
                    item {
                        DebugInfo(elementsCount = elements.size)
                    }
                }
                
                itemsIndexed(elements, key = { index, _ -> index }) { index, element ->
                    AnimatedVisibility(
                        visible = true,
                        enter = if (enableAnimations) {
                            fadeIn(
                                animationSpec = tween(
                                    durationMillis = 300,
                                    delayMillis = index * 50
                                )
                            ) + slideInVertically(
                                animationSpec = tween(
                                    durationMillis = 300,
                                    delayMillis = index * 50
                                ),
                                initialOffsetY = { it / 4 }
                            )
                        } else {
                            EnterTransition.None
                        },
                        exit = ExitTransition.None
                    ) {
                        EnhancedMarkdownElement(
                            element = element,
                            showDebug = showDebug,
                            readerMode = readerMode,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = if (readerMode) 0.dp else 16.dp,
                        vertical = if (readerMode) 0.dp else 16.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(
                    if (readerMode) 16.dp else 12.dp
                )
            ) {
                if (showDebug) {
                    DebugInfo(elementsCount = elements.size)
                }
                
                elements.forEachIndexed { index, element ->
                    AnimatedVisibility(
                        visible = true,
                        enter = if (enableAnimations) {
                            fadeIn(
                                animationSpec = tween(
                                    durationMillis = 300,
                                    delayMillis = index * 50
                                )
                            ) + slideInVertically(
                                animationSpec = tween(
                                    durationMillis = 300,
                                    delayMillis = index * 50
                                ),
                                initialOffsetY = { it / 4 }
                            )
                        } else {
                            EnterTransition.None
                        },
                        exit = ExitTransition.None
                    ) {
                        EnhancedMarkdownElement(
                            element = element,
                            showDebug = showDebug,
                            readerMode = readerMode,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(readerMode: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (readerMode) "Nothing to read yet..." else "Nothing to preview yet...",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        if (!readerMode) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Start typing to see your markdown come to life!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ErrorState(content: String, showDebug: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "No markdown elements found",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
        if (showDebug) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Content: \"${content.take(100)}...\"",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DebugInfo(elementsCount: Int) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "DEBUG: $elementsCount elements parsed",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
private fun EnhancedMarkdownElement(
    element: MarkdownParser.Element,
    showDebug: Boolean = false,
    readerMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    when (element) {
        is MarkdownParser.Element.Header -> {
            val textStyle = when (element.level) {
                1 -> if (readerMode) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineLarge
                2 -> if (readerMode) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineMedium
                3 -> if (readerMode) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineSmall
                4 -> MaterialTheme.typography.titleLarge
                5 -> MaterialTheme.typography.titleMedium
                else -> MaterialTheme.typography.titleSmall
            }
            
            Column {
                if (showDebug) {
                    DebugLabel("[H${element.level}]", MaterialTheme.colorScheme.primary)
                }
                
                Text(
                    text = element.text,
                    style = textStyle,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = modifier.padding(
                        vertical = if (readerMode) 8.dp else 4.dp
                    )
                )
                
                // Add subtle underline for headers in reader mode
                if (readerMode && element.level <= 2) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.3f)
                            .height(2.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                RoundedCornerShape(1.dp)
                            )
                    )
                }
            }
        }
        
        is MarkdownParser.Element.Text -> {
            Column {
                if (showDebug && element.text.isNotBlank()) {
                    DebugLabel("[TEXT]", MaterialTheme.colorScheme.secondary)
                }
                Text(
                    text = element.text,
                    style = if (readerMode) {
                        MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 1.8.sp * (MaterialTheme.typography.bodyLarge.fontSize.value)
                        )
                    } else {
                        MaterialTheme.typography.bodyMedium
                    },
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = modifier
                )
            }
        }
        
        is MarkdownParser.Element.Bold -> {
            Column {
                if (showDebug) {
                    DebugLabel("[BOLD]", MaterialTheme.colorScheme.primary)
                }
                Text(
                    text = element.text,
                    style = if (readerMode) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = modifier
                )
            }
        }
        
        is MarkdownParser.Element.Italic -> {
            Column {
                if (showDebug) {
                    DebugLabel("[ITALIC]", MaterialTheme.colorScheme.primary)
                }
                Text(
                    text = element.text,
                    style = (if (readerMode) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium)
                        .copy(fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = modifier
                )
            }
        }
        
        is MarkdownParser.Element.Strikethrough -> {
            Column {
                if (showDebug) {
                    DebugLabel("[STRIKETHROUGH]", MaterialTheme.colorScheme.primary)
                }
                Text(
                    text = element.text,
                    style = if (readerMode) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                    textDecoration = TextDecoration.LineThrough,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = modifier
                )
            }
        }
        
        is MarkdownParser.Element.Highlight -> {
            Column {
                if (showDebug) {
                    DebugLabel("[HIGHLIGHT]", MaterialTheme.colorScheme.primary)
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp),
                    modifier = modifier
                ) {
                    Text(
                        text = element.text,
                        style = if (readerMode) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(
                            horizontal = 6.dp,
                            vertical = if (readerMode) 4.dp else 2.dp
                        )
                    )
                }
            }
        }
        
        is MarkdownParser.Element.Code -> {
            Column {
                if (showDebug) {
                    DebugLabel("[CODE]", MaterialTheme.colorScheme.primary)
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp),
                    modifier = modifier
                ) {
                    Text(
                        text = element.text,
                        style = if (readerMode) {
                            MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace
                            )
                        } else {
                            MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(
                            horizontal = 6.dp,
                            vertical = if (readerMode) 4.dp else 2.dp
                        )
                    )
                }
            }
        }
        
        is MarkdownParser.Element.CodeBlock -> {
            Column {
                if (showDebug) {
                    DebugLabel("[CODE BLOCK: ${element.language}]", MaterialTheme.colorScheme.primary)
                }
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = modifier
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (element.language.isNotEmpty()) {
                            Text(
                                text = element.language,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        Text(
                            text = element.code,
                            style = if (readerMode) {
                                MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 1.6.sp * MaterialTheme.typography.bodyMedium.fontSize.value
                                )
                            } else {
                                MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace
                                )
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        is MarkdownParser.Element.Quote -> {
            Column {
                if (showDebug) {
                    DebugLabel("[QUOTE]", MaterialTheme.colorScheme.primary)
                }
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ),
                    modifier = modifier
                ) {
                    Row {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Text(
                            text = element.text,
                            style = (if (readerMode) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium)
                                .copy(fontStyle = FontStyle.Italic),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
        
        // Add implementations for other elements...
        else -> {
            // Fallback to the original MarkdownElement implementation
            MarkdownElement(
                element = element,
                showDebug = showDebug,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun DebugLabel(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier.padding(bottom = 2.dp)
    )
}