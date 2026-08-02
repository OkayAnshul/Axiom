package com.cosmiclaboratory.axiom.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.cosmiclaboratory.axiom.ui.theme.LocalAxiomDarkTheme
import com.cosmiclaboratory.axiom.domain.model.MarkdownTemplate
import com.cosmiclaboratory.axiom.domain.model.MarkdownTemplates
import com.cosmiclaboratory.axiom.domain.model.TemplateCategory

@Composable
fun ModernFormattingToolbar(
    onTemplateSelected: (MarkdownTemplate) -> Unit,
    modifier: Modifier = Modifier
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedFormats by remember { mutableStateOf(setOf<String>()) }
    var selectedTextStyle by remember { mutableStateOf("Body") }
    
    // Must be the RESOLVED theme, not isSystemInDarkTheme(). Reading the OS setting
    // here ignored the user's theme override and painted the light tile bar on top
    // of a dark editor whenever the two disagreed.
    val isDarkTheme = LocalAxiomDarkTheme.current
    
    Box(modifier = modifier) {
        if (isDarkTheme) {
            ModernQuickTileBar(
                selectedFormats = selectedFormats,
                onFormatToggle = { formatId ->
                    handleFormatToggle(formatId, selectedFormats) { newFormats ->
                        selectedFormats = newFormats
                        findWritingTemplate(formatId)?.let { template ->
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
                        findWritingTemplate(formatId)?.let { template ->
                            onTemplateSelected(template)
                        }
                    }
                },
                onMoreOptionsClick = { showBottomSheet = true }
            )
        }
        
        ModernFormatBottomSheet(
            isVisible = showBottomSheet,
            onDismiss = { showBottomSheet = false },
            selectedTextStyle = selectedTextStyle,
            selectedFormats = selectedFormats,
            onTextStyleChange = { style ->
                selectedTextStyle = style
                findWritingStyleTemplate(style)?.let { template ->
                    onTemplateSelected(template)
                }
            },
            onFormatToggle = { formatId ->
                handleFormatToggle(formatId, selectedFormats) { newFormats ->
                    selectedFormats = newFormats
                    findWritingTemplate(formatId)?.let { template ->
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

private fun findWritingTemplate(formatId: String): MarkdownTemplate? {
    val template = when (formatId) {
        "quote" -> WritingTemplateSpec("quote", "Quote", "quote: text")
        "table" -> WritingTemplateSpec("table_2x2", "Comparison", "Comparison:\nOption one - \nOption two - ")
        "link" -> WritingTemplateSpec("link", "Link", "https://example.com")
        "image" -> WritingTemplateSpec("image", "Image note", "Image note: text")
        "bullet_list" -> WritingTemplateSpec("bullet_list", "List", "list: text")
        "task_list" -> WritingTemplateSpec("task_list", "Task", "todo text")
        "h1" -> WritingTemplateSpec("h1", "Title", "Title:")
        "h2" -> WritingTemplateSpec("h2", "Section", "Section title:")
        "h3" -> WritingTemplateSpec("h3", "Heading", "Heading:")
        "h4" -> WritingTemplateSpec("h4", "Detail", "Detail:")
        "h5" -> WritingTemplateSpec("h5", "Note", "Note:")
        "h6" -> WritingTemplateSpec("h6", "Small note", "Small note:")
        else -> null
    } ?: return null

    return naturalTemplate(template)
}

private fun findWritingStyleTemplate(styleName: String): MarkdownTemplate? {
    return when (styleName) {
        "Title" -> findWritingTemplate("h1")
        "Subtitle" -> findWritingTemplate("h2")
        "Heading" -> findWritingTemplate("h3")
        "Subheading" -> findWritingTemplate("h4")
        "Section" -> findWritingTemplate("h5")
        "Note" -> findWritingTemplate("h6")
        "Body" -> null
        else -> null
    }
}

private fun naturalTemplate(spec: WritingTemplateSpec): MarkdownTemplate {
    val source = MarkdownTemplates.getAllTemplates()
        .values
        .flatten()
        .firstOrNull { it.id == spec.sourceId }

    return (source ?: MarkdownTemplates.headers.first()).copy(
        id = "natural_${spec.sourceId}",
        name = spec.name,
        template = spec.template,
        category = TemplateCategory.TEMPLATES,
        description = "",
        cursorPosition = -1,
        isMarkdownOnly = false,
        placeholderText = "text",
        supportsSmartDeletion = false
    )
}

private data class WritingTemplateSpec(
    val sourceId: String,
    val name: String,
    val template: String
)

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
