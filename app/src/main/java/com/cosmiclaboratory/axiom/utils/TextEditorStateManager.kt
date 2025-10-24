package com.cosmiclaboratory.axiom.utils

import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import com.cosmiclaboratory.axiom.domain.model.MarkdownTemplate

/**
 * Centralized Text Editor State Manager
 * 
 * Provides a single source of truth for text editing operations with context awareness.
 * Handles template insertions, cursor positioning, and state synchronization while
 * eliminating race conditions and preserving user experience.
 * 
 * Key Features:
 * - Context-aware template insertion (selection vs empty line vs cursor position)
 * - Proper cursor positioning for all scenarios
 * - Smart external content synchronization
 * - Prevention of cursor jumping and race conditions
 * 
 * Architecture:
 * - UI TextFieldValue is authoritative for immediate operations
 * - External content changes are filtered and only applied when appropriate
 * - Template insertions work directly with UI state for immediate feedback
 */
class TextEditorStateManager {
    
    private val smartDeletionHandler = SmartDeletionHandler()
    
    /**
     * Apply template formatting to text with full context awareness
     * 
     * @param currentValue Current TextFieldValue from UI
     * @param template Template to apply
     * @return New TextFieldValue with template applied and cursor positioned correctly
     */
    fun applyTemplate(
        currentValue: TextFieldValue,
        template: MarkdownTemplate
    ): TextFieldValue {
        
        val text = currentValue.text
        val selection = currentValue.selection
        val hasSelection = !selection.collapsed
        
        return when {
            hasSelection -> {
                // CASE 1: User has selected text - wrap it with template
                applyTemplateToSelection(currentValue, template)
            }
            
            text.isEmpty() || isAtLineStart(text, selection.start) -> {
                // CASE 2: Empty content or start of line - insert template with optimal cursor positioning
                insertTemplateAtPosition(currentValue, template, selection.start)
            }
            
            else -> {
                // CASE 3: Cursor in middle of text - insert template at cursor position
                insertTemplateAtPosition(currentValue, template, selection.start)
            }
        }
    }
    
    /**
     * Apply template to selected text (wrapping behavior)
     */
    private fun applyTemplateToSelection(
        currentValue: TextFieldValue,
        template: MarkdownTemplate
    ): TextFieldValue {
        
        val text = currentValue.text
        val selection = currentValue.selection
        val selectedText = text.substring(selection.start, selection.end)
        
        val beforeSelection = text.substring(0, selection.start)
        val afterSelection = text.substring(selection.end)
        
        // Extract clean markers for wrapping
        val (openMarker, closeMarker) = extractTemplateMarkers(template.template)
        
        // Create wrapped text
        val wrappedText = when {
            // Symmetric markers (bold, italic, code, etc.)
            closeMarker.isNotEmpty() && openMarker != template.template -> {
                "$openMarker$selectedText$closeMarker"
            }
            
            // Links and images - special handling
            template.template.contains("[") && template.template.contains("](") -> {
                if (template.template.startsWith("![")) {
                    "![$selectedText](image-url)"
                } else {
                    "[$selectedText](url)"
                }
            }
            
            // Headers - prefix handling
            template.template.startsWith("#") -> {
                "$openMarker$selectedText"
            }
            
            // Fallback - template replacement
            else -> {
                template.template
                    .replace("text", selectedText)
                    .replace("bold text", selectedText)
                    .replace("italic text", selectedText)
                    .replace("code", selectedText)
                    .replace("strikethrough text", selectedText)
                    .replace("highlighted text", selectedText)
                    .replace("underlined text", selectedText)
                    .replace("link text", selectedText)
                    .replace("alt text", selectedText)
            }
        }
        
        val newText = beforeSelection + wrappedText + afterSelection
        
        // Position cursor after the wrapped text for immediate typing
        val newCursorPosition = selection.start + wrappedText.length
        
        return TextFieldValue(
            text = newText,
            selection = TextRange(newCursorPosition)
        )
    }
    
    /**
     * Insert template at specific position with context-aware cursor positioning
     */
    private fun insertTemplateAtPosition(
        currentValue: TextFieldValue,
        template: MarkdownTemplate,
        position: Int
    ): TextFieldValue {
        
        val text = currentValue.text
        val beforeCursor = text.substring(0, position)
        val afterCursor = text.substring(position)
        
        val newText = beforeCursor + template.template + afterCursor
        
        // Smart cursor positioning based on template type and context
        val newCursorPosition = calculateOptimalCursorPosition(
            template = template,
            insertionPosition = position,
            isEmptyLine = beforeCursor.isEmpty() || beforeCursor.endsWith("\n")
        )
        
        return TextFieldValue(
            text = newText,
            selection = TextRange(newCursorPosition)
        )
    }
    
    /**
     * Calculate optimal cursor position after template insertion
     */
    private fun calculateOptimalCursorPosition(
        template: MarkdownTemplate,
        insertionPosition: Int,
        isEmptyLine: Boolean
    ): Int {
        
        return when {
            // For empty line insertions, position cursor optimally for typing
            isEmptyLine -> when {
                template.template.startsWith("**") && template.template.endsWith("**") -> {
                    // Bold: **bold text**| -> **|** (between markers)
                    insertionPosition + 2
                }
                template.template.startsWith("*") && template.template.endsWith("*") && !template.template.startsWith("**") -> {
                    // Italic: *italic text*| -> *|* (between markers)  
                    insertionPosition + 1
                }
                template.template.startsWith("`") && template.template.endsWith("`") -> {
                    // Code: `code`| -> `|` (between markers)
                    insertionPosition + 1
                }
                template.template.startsWith("~~") && template.template.endsWith("~~") -> {
                    // Strikethrough: ~~text~~| -> ~~|~~ (between markers)
                    insertionPosition + 2
                }
                template.template.contains("[") && template.template.contains("](") -> {
                    // Link: [text](url)| -> [|](url) (at text position)
                    insertionPosition + 1
                }
                template.template.startsWith("#") -> {
                    // Headers: # Title Text| -> # | (after hash and space)
                    val spaceIndex = template.template.indexOf(' ')
                    insertionPosition + if (spaceIndex > 0) spaceIndex + 1 else template.template.length
                }
                else -> {
                    // Default: end of template
                    insertionPosition + template.template.length
                }
            }
            
            // For mid-text insertions, position at end for immediate typing
            else -> insertionPosition + template.template.length
        }
    }
    
    /**
     * Extract opening and closing markers from template
     */
    private fun extractTemplateMarkers(template: String): Pair<String, String> {
        return when {
            // Bold: **bold text** -> ("**", "**")
            template.startsWith("**") && template.length > 4 && template.endsWith("**") -> "**" to "**"
            
            // Italic: *italic text* -> ("*", "*") (but not bold)
            template.startsWith("*") && !template.startsWith("**") && template.length > 2 && template.endsWith("*") -> "*" to "*"
            
            // Code: `code` -> ("`", "`")
            template.startsWith("`") && template.length > 2 && template.endsWith("`") -> "`" to "`"
            
            // Strikethrough: ~~text~~ -> ("~~", "~~")
            template.startsWith("~~") && template.length > 4 && template.endsWith("~~") -> "~~" to "~~"
            
            // Underline: __text__ -> ("__", "__")
            template.startsWith("__") && template.length > 4 && template.endsWith("__") -> "__" to "__"
            
            // Highlight: ==text== -> ("==", "==")
            template.startsWith("==") && template.length > 4 && template.endsWith("==") -> "==" to "=="
            
            // Link: [text](url) -> ("[", "]()")
            template.contains("[") && template.contains("](") -> {
                val linkStart = "["
                val urlPart = template.substring(template.indexOf("]("))
                linkStart to urlPart
            }
            
            // Image: ![text](url) -> ("![", "]()")
            template.startsWith("![") && template.contains("](") -> {
                val imageStart = "!["
                val urlPart = template.substring(template.indexOf("]("))
                imageStart to urlPart
            }
            
            // Headers: # text -> ("# ", "")
            template.startsWith("#") -> {
                val headerMarker = template.takeWhile { it == '#' } + " "
                headerMarker to ""
            }
            
            // Default: use template as-is for complex cases
            else -> template to ""
        }
    }
    
    /**
     * Check if cursor is at the start of a line
     */
    private fun isAtLineStart(text: String, position: Int): Boolean {
        if (position == 0) return true
        if (position > text.length) return false
        
        // Check if the character before cursor is a newline
        return text.getOrNull(position - 1) == '\n'
    }
    
    /**
     * Handle smart text changes with template cleanup
     */
    fun handleTextChange(
        oldValue: TextFieldValue,
        newValue: TextFieldValue
    ): TextFieldValue {
        
        // Check if this is a backspace operation
        val isBackspace = smartDeletionHandler.isBackspaceOperation(oldValue, newValue)
        
        return if (isBackspace) {
            // Try smart deletion first
            val smartResult = smartDeletionHandler.handleSmartBackspace(oldValue)
            smartResult ?: newValue
        } else {
            // Normal text change
            newValue
        }
    }
    
    /**
     * Determine if external content should sync with UI state
     * Returns true only for legitimate external content changes
     */
    fun shouldSyncExternalContent(
        currentUIValue: TextFieldValue,
        externalContent: String
    ): Boolean {
        
        val currentText = currentUIValue.text
        val newText = externalContent
        
        // Don't sync if content is identical
        if (currentText == newText) return false
        
        // Sync only for major external changes that aren't user typing
        val lengthDiff = newText.length - currentText.length
        
        return when {
            // Large changes (likely external updates)
            kotlin.math.abs(lengthDiff) > 50 -> true
            
            // Content cleared externally
            newText.isEmpty() && currentText.isNotEmpty() -> true
            
            // Content completely replaced
            !newText.contains(currentText.take(20)) && currentText.length > 20 -> true
            
            // Default: don't sync (preserve user typing)
            else -> false
        }
    }
    
    /**
     * Create TextFieldValue from external content with preserved cursor positioning
     */
    fun createTextFieldValueFromExternalContent(
        externalContent: String,
        preserveCursorAtEnd: Boolean = false
    ): TextFieldValue {
        
        val cursorPosition = if (preserveCursorAtEnd) {
            externalContent.length
        } else {
            // Try to preserve relative cursor position
            0.coerceAtMost(externalContent.length)
        }
        
        return TextFieldValue(
            text = externalContent,
            selection = TextRange(cursorPosition)
        )
    }
}

/**
 * Data class for template insertion context
 */
data class TemplateInsertionContext(
    val hasSelection: Boolean,
    val isEmptyLine: Boolean,
    val cursorPosition: Int,
    val selectedText: String = ""
)

/**
 * Result of template application with debugging info
 */
data class TemplateApplicationResult(
    val textFieldValue: TextFieldValue,
    val context: TemplateInsertionContext,
    val appliedTemplate: MarkdownTemplate,
    val operationType: String // "selection", "empty_line", "cursor_insertion"
)