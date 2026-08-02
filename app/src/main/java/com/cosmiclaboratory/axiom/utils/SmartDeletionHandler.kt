package com.cosmiclaboratory.axiom.utils

import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange

/**
 * Smart Deletion Handler
 * 
 * Handles smart backspace operations for template patterns.
 * Provides intelligent deletion of placeholder text and empty templates.
 * 
 * Features:
 * - Single backspace removes placeholder text
 * - Second backspace removes empty template markers
 * - Preserves user content from accidental deletion
 * - Falls back to normal backspace when appropriate
 */
class SmartDeletionHandler {
    
    private val patternMatcher = TemplatePatternMatcher()
    
    /**
     * Handle backspace operation with smart deletion logic
     * 
     * @param currentValue Current TextFieldValue
     * @param isBackspaceAtEnd True if backspace at cursor position, false for delete key
     * @return Modified TextFieldValue or null if no smart deletion should occur
     */
    fun handleSmartBackspace(
        currentValue: TextFieldValue,
        isBackspaceAtEnd: Boolean = true
    ): TextFieldValue? {
        
        val text = currentValue.text
        val cursorPosition = currentValue.selection.start
        
        // Only handle backspace at cursor position (not selections)
        if (currentValue.selection.length > 0) {
            return null // Let normal deletion handle selections
        }
        
        // Find template pattern at cursor
        val pattern = patternMatcher.detectAtCursor(text, cursorPosition)
            ?: return null // No pattern found, use normal backspace
        
        // Determine deletion action based on pattern state
        return when {
            pattern.isPlaceholder -> {
                // Remove placeholder text first
                handlePlaceholderDeletion(currentValue, pattern)
            }
            
            pattern.innerText.trim().isEmpty() -> {
                // Empty template - remove markers
                handleEmptyTemplateDeletion(currentValue, pattern)
            }
            
            else -> {
                // User content - use normal backspace
                null
            }
        }
    }
    
    /**
     * Remove placeholder text from template, leaving markers
     * Example: "**bold text**|" -> "**|**"
     */
    private fun handlePlaceholderDeletion(
        currentValue: TextFieldValue,
        pattern: TemplatePattern
    ): TextFieldValue {
        
        val deletionResult = patternMatcher.createSmartDeletionResult(
            originalText = currentValue.text,
            pattern = pattern,
            deletionMode = DeletionMode.REMOVE_PLACEHOLDER
        )
        
        return TextFieldValue(
            text = deletionResult.newText,
            selection = TextRange(deletionResult.newCursorPosition)
        )
    }
    
    /**
     * Remove empty template markers
     * Example: "**|**" -> "|"
     */
    private fun handleEmptyTemplateDeletion(
        currentValue: TextFieldValue,
        pattern: TemplatePattern
    ): TextFieldValue {
        
        val deletionResult = patternMatcher.createSmartDeletionResult(
            originalText = currentValue.text,
            pattern = pattern,
            deletionMode = DeletionMode.REMOVE_ENTIRE_TEMPLATE
        )
        
        return TextFieldValue(
            text = deletionResult.newText,
            selection = TextRange(deletionResult.newCursorPosition)
        )
    }
    
    /**
     * Check if backspace operation should be handled smartly
     * 
     * @param oldValue Previous TextFieldValue
     * @param newValue New TextFieldValue
     * @return True if this looks like a backspace operation
     */
    fun isBackspaceOperation(oldValue: TextFieldValue, newValue: TextFieldValue): Boolean {
        val oldText = oldValue.text
        val newText = newValue.text
        val oldCursor = oldValue.selection.start
        val newCursor = newValue.selection.start
        
        // Check if text was deleted and cursor moved back
        return when {
            // Text got shorter
            newText.length < oldText.length -> {
                val deletedChars = oldText.length - newText.length
                val cursorMoved = oldCursor - newCursor
                
                // Cursor moved back by the same amount as deleted characters
                deletedChars == cursorMoved && deletedChars > 0
            }
            
            else -> false
        }
    }
    
    /**
     * Get preview of what would be deleted with smart backspace
     * Useful for UI feedback or testing
     */
    fun getSmartDeletionPreview(text: String, cursorPosition: Int): SmartDeletionPreview? {
        val pattern = patternMatcher.detectAtCursor(text, cursorPosition)
            ?: return null
        
        return when {
            pattern.isPlaceholder -> {
                SmartDeletionPreview(
                    pattern = pattern,
                    action = DeletionAction.REMOVE_PLACEHOLDER,
                    willDelete = pattern.innerText,
                    description = "Remove placeholder '${pattern.innerText}'"
                )
            }
            
            pattern.innerText.trim().isEmpty() -> {
                SmartDeletionPreview(
                    pattern = pattern,
                    action = DeletionAction.REMOVE_TEMPLATE,
                    willDelete = pattern.openMarker + pattern.closeMarker,
                    description = "Remove empty ${pattern.templateType} template"
                )
            }
            
            else -> {
                SmartDeletionPreview(
                    pattern = pattern,
                    action = DeletionAction.NORMAL_BACKSPACE,
                    willDelete = "",
                    description = "Use normal backspace (user content)"
                )
            }
        }
    }
    
    /**
     * Check if cursor is positioned for smart deletion
     */
    fun canPerformSmartDeletion(text: String, cursorPosition: Int): Boolean {
        val pattern = patternMatcher.detectAtCursor(text, cursorPosition)
        return pattern != null && (pattern.isPlaceholder || pattern.innerText.trim().isEmpty())
    }
}

/**
 * Preview of smart deletion action
 */
data class SmartDeletionPreview(
    val pattern: TemplatePattern,
    val action: DeletionAction,
    val willDelete: String,
    val description: String
)

/**
 * Types of deletion actions
 */
enum class DeletionAction {
    REMOVE_PLACEHOLDER,    // Remove placeholder text, keep markers
    REMOVE_TEMPLATE,       // Remove entire template
    NORMAL_BACKSPACE       // Use normal backspace behavior
}