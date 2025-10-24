package com.cosmiclaboratory.axiom.utils

/**
 * Template Pattern Recognition System
 * 
 * Detects markdown template patterns around cursor position and identifies
 * whether they contain placeholder text that can be smart-deleted.
 * 
 * Usage:
 * - Detect if cursor is at the end of a template pattern
 * - Identify placeholder vs user content
 * - Enable smart backspace deletion
 */

data class TemplatePattern(
    val startPos: Int,
    val endPos: Int,
    val templateType: String,        // "bold", "italic", "code", etc.
    val openMarker: String,          // "**", "*", "`", "~~", etc.
    val closeMarker: String,         // "**", "*", "`", "~~", etc.
    val innerText: String,           // The text between markers
    val fullMatch: String,           // Complete template string
    val isPlaceholder: Boolean       // Is it default placeholder text?
) {
    val isUserContent: Boolean get() = !isPlaceholder
    val canBeSmartDeleted: Boolean get() = isPlaceholder
}

class TemplatePatternMatcher {
    
    companion object {
        // Default placeholder texts that can be smart-deleted
        private val PLACEHOLDER_TEXTS = mapOf(
            "bold" to setOf("bold text", "text"),
            "italic" to setOf("italic text", "text"),
            "code" to setOf("code", "code text", "text"),
            "strikethrough" to setOf("strikethrough text", "text"),
            "underline" to setOf("underlined text", "text"),
            "highlight" to setOf("highlighted text", "text"),
            "link" to setOf("link text", "text"),
            "image" to setOf("alt text", "text"),
            // Headers
            "h1" to setOf("title text", "text"),
            "h2" to setOf("subtitle text", "text"),
            "h3" to setOf("heading text", "text"),
            "h4" to setOf("subheading text", "text"),
            "h5" to setOf("section text", "text"),
            "h6" to setOf("note text", "text")
        )
        
        // Template patterns with their markers
        private val TEMPLATE_PATTERNS = listOf(
            // Order matters: More specific patterns first
            // Headers (check from H6 to H1 for proper matching)
            TemplateDefinition("h6", Regex("#{6}\\s+(.+?)(?=\\n|$)"), "###### ", ""),
            TemplateDefinition("h5", Regex("#{5}\\s+(.+?)(?=\\n|$)"), "##### ", ""),
            TemplateDefinition("h4", Regex("#{4}\\s+(.+?)(?=\\n|$)"), "#### ", ""),
            TemplateDefinition("h3", Regex("#{3}\\s+(.+?)(?=\\n|$)"), "### ", ""),
            TemplateDefinition("h2", Regex("#{2}\\s+(.+?)(?=\\n|$)"), "## ", ""),
            TemplateDefinition("h1", Regex("#\\s+(.+?)(?=\\n|$)"), "# ", ""),
            // Formatting patterns
            TemplateDefinition("bold", Regex("\\*\\*(.*?)\\*\\*"), "**", "**"),
            TemplateDefinition("italic", Regex("\\*([^*]+?)\\*"), "*", "*"),
            TemplateDefinition("strikethrough", Regex("~~(.*?)~~"), "~~", "~~"),
            TemplateDefinition("code", Regex("`([^`]+?)`"), "`", "`"),
            TemplateDefinition("underline", Regex("__(.*?)__"), "__", "__"),
            TemplateDefinition("highlight", Regex("==(.*?)=="), "==", "=="),
            TemplateDefinition("link", Regex("\\[([^\\]]+?)\\]\\([^)]+?\\)"), "[", "](url)"),
            TemplateDefinition("image", Regex("!\\[([^\\]]+?)\\]\\([^)]+?\\)"), "![", "](image-url)")
        )
    }
    
    private data class TemplateDefinition(
        val type: String,
        val regex: Regex,
        val openMarker: String,
        val closeMarker: String
    )
    
    /**
     * Detect template pattern at cursor position
     * Returns null if no pattern is found or cursor is not at pattern boundary
     */
    fun detectAtCursor(text: String, cursorPosition: Int): TemplatePattern? {
        // Check if cursor is at valid position
        if (cursorPosition < 0 || cursorPosition > text.length) {
            return null
        }
        
        // Find all template matches in the text
        val allMatches = findAllTemplateMatches(text)
        
        // Find pattern where cursor is at the end boundary
        return allMatches.find { pattern ->
            cursorPosition == pattern.endPos
        }
    }
    
    /**
     * Find all template patterns in the text
     */
    fun findAllTemplateMatches(text: String): List<TemplatePattern> {
        val patterns = mutableListOf<TemplatePattern>()
        
        for (definition in TEMPLATE_PATTERNS) {
            val matches = definition.regex.findAll(text)
            for (match in matches) {
                val innerText = if (definition.type == "link" || definition.type == "image") {
                    // For links and images, extract the link/alt text
                    match.groupValues[1]
                } else {
                    match.groupValues[1]
                }
                
                val isPlaceholder = isPlaceholderText(innerText, definition.type)
                
                patterns.add(
                    TemplatePattern(
                        startPos = match.range.first,
                        endPos = match.range.last + 1,
                        templateType = definition.type,
                        openMarker = definition.openMarker,
                        closeMarker = definition.closeMarker,
                        innerText = innerText,
                        fullMatch = match.value,
                        isPlaceholder = isPlaceholder
                    )
                )
            }
        }
        
        // Sort by position for consistent ordering
        return patterns.sortedBy { it.startPos }
    }
    
    /**
     * Check if text is a recognized placeholder
     */
    private fun isPlaceholderText(text: String, templateType: String): Boolean {
        val placeholders = PLACEHOLDER_TEXTS[templateType] ?: return false
        return placeholders.contains(text.trim().lowercase())
    }
    
    /**
     * Create smart deletion result for a pattern
     * Returns the text after removing the template pattern
     */
    fun createSmartDeletionResult(
        originalText: String, 
        pattern: TemplatePattern,
        deletionMode: DeletionMode = DeletionMode.REMOVE_PLACEHOLDER
    ): SmartDeletionResult {
        
        return when (deletionMode) {
            DeletionMode.REMOVE_PLACEHOLDER -> {
                if (pattern.isPlaceholder) {
                    // Remove the inner placeholder text, keep markers
                    val beforePattern = originalText.substring(0, pattern.startPos)
                    val afterPattern = originalText.substring(pattern.endPos)
                    val emptyTemplate = pattern.openMarker + pattern.closeMarker
                    val newText = beforePattern + emptyTemplate + afterPattern
                    
                    // Smart cursor positioning for optimal user experience
                    val newCursorPos = when (pattern.templateType) {
                        "bold", "italic", "strikethrough", "underline", "highlight" -> {
                            // Position between symmetric markers: **|**
                            pattern.startPos + pattern.openMarker.length
                        }
                        "code" -> {
                            // Position between code markers: `|`
                            pattern.startPos + pattern.openMarker.length
                        }
                        "link" -> {
                            // Position at link text area: [|](url)
                            pattern.startPos + 1
                        }
                        "image" -> {
                            // Position at alt text area: ![|](image-url)
                            pattern.startPos + 2
                        }
                        "h1", "h2", "h3", "h4", "h5", "h6" -> {
                            // Position after header markers: "# |", "## |", etc.
                            pattern.startPos + pattern.openMarker.length
                        }
                        else -> {
                            // Default: position between markers or after open marker
                            pattern.startPos + pattern.openMarker.length
                        }
                    }
                    
                    SmartDeletionResult(
                        newText = newText,
                        newCursorPosition = newCursorPos,
                        deletedContent = pattern.innerText,
                        remainingTemplate = emptyTemplate
                    )
                } else {
                    // User content - no smart deletion
                    SmartDeletionResult.noChange(originalText, pattern.endPos)
                }
            }
            
            DeletionMode.REMOVE_ENTIRE_TEMPLATE -> {
                // Remove the entire template
                val beforePattern = originalText.substring(0, pattern.startPos)
                val afterPattern = originalText.substring(pattern.endPos)
                val newText = beforePattern + afterPattern
                
                SmartDeletionResult(
                    newText = newText,
                    newCursorPosition = pattern.startPos,
                    deletedContent = pattern.fullMatch,
                    remainingTemplate = ""
                )
            }
        }
    }
    
    /**
     * Check if position is at the end of an empty template (e.g., "**|**")
     */
    fun isAtEmptyTemplate(text: String, cursorPosition: Int): TemplatePattern? {
        val pattern = detectAtCursor(text, cursorPosition) ?: return null
        
        // Check if we're in an empty template (markers only)
        return if (pattern.innerText.trim().isEmpty()) {
            pattern
        } else {
            null
        }
    }
}

/**
 * Deletion mode for smart backspace
 */
enum class DeletionMode {
    REMOVE_PLACEHOLDER,      // Remove placeholder text, keep markers: "**bold text**" -> "****"
    REMOVE_ENTIRE_TEMPLATE   // Remove entire template: "****" -> ""
}

/**
 * Result of smart deletion operation
 */
data class SmartDeletionResult(
    val newText: String,
    val newCursorPosition: Int,
    val deletedContent: String,
    val remainingTemplate: String,
    val wasSmartDeleted: Boolean = true
) {
    companion object {
        fun noChange(originalText: String, cursorPosition: Int): SmartDeletionResult {
            return SmartDeletionResult(
                newText = originalText,
                newCursorPosition = cursorPosition,
                deletedContent = "",
                remainingTemplate = "",
                wasSmartDeleted = false
            )
        }
    }
}