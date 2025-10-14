package com.cosmiclaboratory.axiom.ui.components

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
import com.cosmiclaboratory.axiom.utils.MarkdownParser

@Composable
fun MarkdownPreview(
    content: String,
    modifier: Modifier = Modifier,
    showDebug: Boolean = false
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
            Text(
                text = "Nothing to preview yet...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else if (elements.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "No markdown elements found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                if (showDebug) {
                    Text(
                        text = "Content: \"${content.take(100)}...\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    } else {
        Column(modifier = modifier.fillMaxSize()) {
            // Debug info
            if (showDebug) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Text(
                        text = "DEBUG: ${elements.size} elements parsed",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
            
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(elements, key = { index, _ -> index }) { index, element ->
                    MarkdownElement(
                        element = element, 
                        showDebug = showDebug,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun MarkdownElement(
    element: MarkdownParser.Element,
    showDebug: Boolean = false,
    modifier: Modifier = Modifier
) {
    when (element) {
        is MarkdownParser.Element.Header -> {
            val textStyle = when (element.level) {
                1 -> MaterialTheme.typography.headlineLarge
                2 -> MaterialTheme.typography.headlineMedium
                3 -> MaterialTheme.typography.headlineSmall
                4 -> MaterialTheme.typography.titleLarge
                5 -> MaterialTheme.typography.titleMedium
                else -> MaterialTheme.typography.titleSmall
            }
            
            Column {
                if (showDebug) {
                    Text(
                        text = "[H${element.level}]",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = element.text,
                    style = textStyle,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = modifier
                )
            }
        }
        
        is MarkdownParser.Element.Text -> {
            Column {
                if (showDebug && element.text.isNotBlank()) {
                    Text(
                        text = "[TEXT]",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Text(
                    text = element.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = modifier
                )
            }
        }
        
        is MarkdownParser.Element.Bold -> {
            Column {
                if (showDebug) {
                    Text(
                        text = "[BOLD]",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = element.text,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = modifier
                )
            }
        }
        
        is MarkdownParser.Element.Italic -> {
            Column {
                if (showDebug) {
                    Text(
                        text = "[ITALIC]",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = element.text,
                    style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = modifier
                )
            }
        }
        
        is MarkdownParser.Element.Strikethrough -> {
            Column {
                if (showDebug) {
                    Text(
                        text = "[STRIKETHROUGH]",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = element.text,
                    style = MaterialTheme.typography.bodyMedium,
                    textDecoration = TextDecoration.LineThrough,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = modifier
                )
            }
        }
        
        is MarkdownParser.Element.Highlight -> {
            Column {
                if (showDebug) {
                    Text(
                        text = "[HIGHLIGHT]",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(4.dp),
                    modifier = modifier
                ) {
                    Text(
                        text = element.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
        
        is MarkdownParser.Element.Underline -> {
            Column {
                if (showDebug) {
                    Text(
                        text = "[UNDERLINE]",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = element.text,
                    style = MaterialTheme.typography.bodyMedium,
                    textDecoration = TextDecoration.Underline,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = modifier
                )
            }
        }
        
        is MarkdownParser.Element.Link -> {
            Column {
                if (showDebug) {
                    Text(
                        text = "[LINK: ${element.url}]",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = element.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    modifier = modifier
                )
            }
        }
        
        is MarkdownParser.Element.Image -> {
            Column {
                if (showDebug) {
                    Text(
                        text = "[IMAGE: ${element.url}]",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = modifier
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info, // Placeholder for image
                            contentDescription = element.alt,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (element.alt.isNotEmpty()) element.alt else "Image",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = element.url,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
        
        is MarkdownParser.Element.Code -> {
            Column {
                if (showDebug) {
                    Text(
                        text = "[CODE]",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(4.dp),
                    modifier = modifier
                ) {
                    Text(
                        text = element.text,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
        
        is MarkdownParser.Element.CodeBlock -> {
            Column {
                if (showDebug) {
                    Text(
                        text = "[CODE BLOCK: ${element.language}]",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = modifier
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        if (element.language.isNotEmpty()) {
                            Text(
                                text = element.language,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        Text(
                            text = element.code,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        is MarkdownParser.Element.Task -> {
            Column {
                if (showDebug) {
                    Text(
                        text = "[TASK: ${if (element.completed) "✓" else "○"}]",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = modifier
                ) {
                    Checkbox(
                        checked = element.completed,
                        onCheckedChange = null,
                        enabled = false
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = element.text,
                        style = MaterialTheme.typography.bodyMedium,
                        textDecoration = if (element.completed) TextDecoration.LineThrough else null,
                        color = if (element.completed) 
                            MaterialTheme.colorScheme.onSurfaceVariant 
                        else 
                            MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        
        is MarkdownParser.Element.ListItem -> {
            Column {
                if (showDebug) {
                    Text(
                        text = "[LIST L${element.level}]",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row(
                    modifier = modifier.padding(start = (element.level * 16).dp)
                ) {
                    Text(
                        text = "• ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = element.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        
        is MarkdownParser.Element.NumberedListItem -> {
            Column {
                if (showDebug) {
                    Text(
                        text = "[NUMBERED LIST ${element.number} L${element.level}]",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row(
                    modifier = modifier.padding(start = (element.level * 16).dp)
                ) {
                    Text(
                        text = "${element.number}. ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = element.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        
        is MarkdownParser.Element.Table -> {
            Column {
                if (showDebug) {
                    Text(
                        text = "[TABLE ${element.headers.size}x${element.rows.size + 1}]",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = modifier
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        // Header row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(8.dp)
                        ) {
                            element.headers.forEachIndexed { index, header ->
                                Text(
                                    text = header,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.weight(1f),
                                    textAlign = when (element.alignments.getOrElse(index) { MarkdownParser.TableAlignment.NONE }) {
                                        MarkdownParser.TableAlignment.CENTER -> TextAlign.Center
                                        MarkdownParser.TableAlignment.RIGHT -> TextAlign.End
                                        MarkdownParser.TableAlignment.LEFT -> TextAlign.Start
                                        MarkdownParser.TableAlignment.NONE -> TextAlign.Start
                                    }
                                )
                                if (index < element.headers.size - 1) {
                                    VerticalDivider(
                                        modifier = Modifier
                                            .height(20.dp)
                                            .padding(horizontal = 4.dp)
                                    )
                                }
                            }
                        }
                        
                        // Data rows
                        element.rows.forEach { row ->
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            ) {
                                row.forEachIndexed { index, cell ->
                                    Text(
                                        text = cell,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f),
                                        textAlign = when (element.alignments.getOrElse(index) { MarkdownParser.TableAlignment.NONE }) {
                                            MarkdownParser.TableAlignment.CENTER -> TextAlign.Center
                                            MarkdownParser.TableAlignment.RIGHT -> TextAlign.End
                                            MarkdownParser.TableAlignment.LEFT -> TextAlign.Start
                                            MarkdownParser.TableAlignment.NONE -> TextAlign.Start
                                        }
                                    )
                                    if (index < row.size - 1) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        is MarkdownParser.Element.HorizontalRule -> {
            Column {
                if (showDebug) {
                    Text(
                        text = "[HORIZONTAL RULE]",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                HorizontalDivider(
                    modifier = modifier.padding(vertical = 8.dp),
                    thickness = 2.dp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        
        is MarkdownParser.Element.Quote -> {
            Column {
                if (showDebug) {
                    Text(
                        text = "[QUOTE]",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = modifier
                ) {
                    Row {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(40.dp)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Text(
                            text = element.text,
                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
        
        is MarkdownParser.Element.Paragraph -> {
            Column {
                if (showDebug) {
                    Text(
                        text = "[PARAGRAPH: ${element.elements.size} elements]",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                // Render paragraph as a single text with multiple spans
                val annotatedString = buildAnnotatedString {
                    element.elements.forEach { elem ->
                        when (elem) {
                            is MarkdownParser.Element.Text -> append(elem.text)
                            is MarkdownParser.Element.Bold -> {
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append(elem.text)
                                }
                            }
                            is MarkdownParser.Element.Italic -> {
                                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                    append(elem.text)
                                }
                            }
                            is MarkdownParser.Element.Code -> {
                                withStyle(SpanStyle(
                                    fontFamily = FontFamily.Monospace,
                                    background = MaterialTheme.colorScheme.surfaceVariant
                                )) {
                                    append(elem.text)
                                }
                            }
                            is MarkdownParser.Element.Strikethrough -> {
                                withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                                    append(elem.text)
                                }
                            }
                            is MarkdownParser.Element.Highlight -> {
                                withStyle(SpanStyle(background = MaterialTheme.colorScheme.primaryContainer)) {
                                    append(elem.text)
                                }
                            }
                            is MarkdownParser.Element.Underline -> {
                                withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                                    append(elem.text)
                                }
                            }
                            is MarkdownParser.Element.Link -> {
                                withStyle(SpanStyle(
                                    color = MaterialTheme.colorScheme.primary,
                                    textDecoration = TextDecoration.Underline
                                )) {
                                    append(elem.text)
                                }
                            }
                            else -> append(elem.toString()) // Fallback
                        }
                    }
                }
                
                Text(
                    text = annotatedString,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = modifier
                )
            }
        }
        
        is MarkdownParser.Element.EmptyLine -> {
            if (showDebug) {
                Text(
                    text = "[EMPTY LINE]",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}