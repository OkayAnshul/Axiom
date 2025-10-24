package com.cosmiclaboratory.axiom.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cosmiclaboratory.axiom.domain.model.MarkdownTemplate
import com.cosmiclaboratory.axiom.domain.model.MarkdownTemplates

@Composable
fun ModernFormattingToolbar(
    onTemplateSelected: (MarkdownTemplate) -> Unit,
    modifier: Modifier = Modifier
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedFormats by remember { mutableStateOf(setOf<String>()) }
    var selectedTextStyle by remember { mutableStateOf("Body") }
    
    val isDarkTheme = isSystemInDarkTheme()
    
    // Pure markdown format options only
    val availableFormats = setOf(
        "bold", "italic", "strikethrough", "code", 
        "h1", "h2", "h3", "h4", "h5", "h6",
        "bullet_list", "task_list", "quote", "link", "table"
    )
    
    Box(modifier = modifier) {
        // Quick Tile Bar (always visible at bottom)
        if (isDarkTheme) {
            ModernQuickTileBar(
                selectedFormats = selectedFormats,
                onFormatToggle = { formatId ->
                    handleFormatToggle(formatId, selectedFormats) { newFormats ->
                        selectedFormats = newFormats
                        // Apply the format by finding corresponding template
                        findMarkdownTemplate(formatId)?.let { template ->
                            onTemplateSelected(template)
                        }
                    }
                },
                onMoreOptionsClick = { showBottomSheet = true }
            )
        } else {
            ModernQuickTileBarLight(
                selectedFormats = selectedFormats,
                onFormatToggle = { formatId ->
                    handleFormatToggle(formatId, selectedFormats) { newFormats ->
                        selectedFormats = newFormats
                        findMarkdownTemplate(formatId)?.let { template ->
                            onTemplateSelected(template)
                        }
                    }
                },
                onMoreOptionsClick = { showBottomSheet = true }
            )
        }
        
        // Bottom Sheet for Markdown Formatting
        ModernFormatBottomSheet(
            isVisible = showBottomSheet,
            onDismiss = { showBottomSheet = false },
            selectedTextStyle = selectedTextStyle,
            selectedFormats = selectedFormats,
            onTextStyleChange = { style ->
                selectedTextStyle = style
                // Apply markdown header template
                findMarkdownHeaderTemplate(style)?.let { template ->
                    onTemplateSelected(template)
                }
            },
            onFormatToggle = { formatId ->
                handleFormatToggle(formatId, selectedFormats) { newFormats ->
                    selectedFormats = newFormats
                    findMarkdownTemplate(formatId)?.let { template ->
                        onTemplateSelected(template)
                    }
                }
            }
        )
    }
}

private fun handleFormatToggle(
    formatId: String,
    currentFormats: Set<String>,
    onUpdate: (Set<String>) -> Unit
) {
    val newFormats = if (currentFormats.contains(formatId)) {
        currentFormats - formatId
    } else {
        currentFormats + formatId
    }
    onUpdate(newFormats)
}

private fun findMarkdownTemplate(formatId: String): MarkdownTemplate? {
    val templates = MarkdownTemplates.getMarkdownOnlyTemplates()
    
    return templates.values.flatten().find { template ->
        when (formatId) {
            "bold" -> template.id == "bold"
            "italic" -> template.id == "italic"
            "strikethrough" -> template.id == "strikethrough"
            "code" -> template.id == "code_inline"
            "quote" -> template.id == "quote"
            "table" -> template.id == "table_2x2" // Default to 2x2 table for quick access
            "link" -> template.id == "link"
            "image" -> template.id == "image"
            // Header mappings for font size control
            "h1" -> template.id == "h1"
            "h2" -> template.id == "h2"
            "h3" -> template.id == "h3"
            "h4" -> template.id == "h4"
            "h5" -> template.id == "h5"
            "h6" -> template.id == "h6"
            "bullet_list" -> template.id == "bullet_list"
            "numbered_list" -> template.id == "numbered_list"
            "task_list" -> template.id == "task_list"
            "code_block" -> template.id == "code_block"
            "horizontal_rule" -> template.id == "horizontal_rule"
            else -> false
        }
    }
}

private fun findMarkdownHeaderTemplate(styleName: String): MarkdownTemplate? {
    val headerTemplates = MarkdownTemplates.headers
    
    return when (styleName) {
        "Title" -> headerTemplates.find { it.id == "h1" }
        "Subtitle" -> headerTemplates.find { it.id == "h2" }
        "Heading" -> headerTemplates.find { it.id == "h3" }
        "Subheading" -> headerTemplates.find { it.id == "h4" }
        "Section" -> headerTemplates.find { it.id == "h5" }
        "Note" -> headerTemplates.find { it.id == "h6" }
        "Body" -> null // Body text doesn't need a template
        else -> null
    }
}

// State management for formatting toolbar
@Composable
fun rememberFormattingState(): FormattingState {
    return remember {
        FormattingState()
    }
}

class FormattingState {
    var selectedFormats by mutableStateOf(setOf<String>())
        private set
    
    var selectedTextStyle by mutableStateOf("Body")
        private set
    
    var selectedFontSize by mutableStateOf(16)
        private set
    
    fun toggleFormat(formatId: String) {
        selectedFormats = if (selectedFormats.contains(formatId)) {
            selectedFormats - formatId
        } else {
            selectedFormats + formatId
        }
    }
    
    fun setTextStyle(style: String) {
        selectedTextStyle = style
    }
    
    fun setFontSize(size: Int) {
        selectedFontSize = size
    }
    
    fun reset() {
        selectedFormats = emptySet()
        selectedTextStyle = "Body"
        selectedFontSize = 16
    }
}

// Extension function to integrate with existing MarkdownTemplate system
fun MarkdownTemplate.toQuickFormat(): String? {
    return when (this.id) {
        "bold" -> "bold"
        "italic" -> "italic"
        "underline" -> "underline" 
        "strikethrough" -> "strikethrough"
        "code_inline" -> "code"
        "highlight" -> "highlight"
        "quote" -> "quote"
        "table" -> "table"
        "link" -> "link"
        "image" -> "image"
        else -> null
    }
}