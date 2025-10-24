# Text Editor Architecture Debug & Analysis

## Executive Summary

This document provides a comprehensive analysis of the text editor architecture issues in the Axiom note-taking app and the implemented solutions. The problems involved cursor jumping, template duplication, and race conditions that were affecting user experience.

## Problem Analysis

### Issue 1: Cursor Jumping to Last Line
**Symptoms**: When typing on the first line, cursor would automatically jump to the last line of the document.

**Root Cause**: Located in `ModernSplitPaneEditor.kt:424-444`
```kotlin
// PROBLEMATIC CODE (Fixed)
LaunchedEffect(content) {
    // Smart external content detection logic was too aggressive
    val isSignificantChange = lengthDiff > 5 || // Likely template insertion
                             (lengthDiff > 0 && newText.contains(Regex("[*`~=\\[#]")) && 
                              !currentText.contains(Regex("[*`~=\\[#]"))) || // New formatting
                             newText.isEmpty() && currentText.isNotEmpty() // Content cleared
    
    if (isSignificantChange && currentText != newText) {
        // This line was causing cursor jumping!
        contentFieldValue = TextFieldValue(
            text = newText, 
            selection = androidx.compose.ui.text.TextRange(newText.length) // ← CURSOR TO END!
        )
    }
}
```

**Explanation**: The external content synchronization logic was moving the cursor to the end of the text whenever it detected "significant changes", which included normal user typing that triggered markdown detection patterns.

### Issue 2: Manual Text Selection Broken  
**Symptoms**: When selecting text like "hello" and applying Bold formatting, result was "**bold hello**" instead of "**hello**".

**Root Cause**: Template insertion bypass of UI selection state in `NoteDetailViewModel.kt:122-151`

**Flow Breakdown**:
1. User selects "hello" in UI → TextFieldValue has selection state
2. User clicks Bold button → calls `viewModel.insertTemplate()` 
3. ViewModel uses its own stored cursor state (ignores current UI selection)
4. Template gets inserted using placeholder text instead of selected text

**Architecture Problem**: Two sources of truth:
- UI: TextFieldValue with current selection
- ViewModel: Stored cursor position from previous updates

### Issue 3: Race Conditions and Context Loss
**Root Causes**:
- Multiple async state updates between UI and ViewModel
- LaunchedEffect feedback loops
- Overly complex cursor synchronization logic
- Missing context awareness in template operations

## Solution Architecture

### Phase 1: Centralized State Management
**Created**: `TextEditorStateManager.kt` (309 lines)

**Key Features**:
```kotlin
class TextEditorStateManager {
    /**
     * Apply template formatting with full context awareness
     */
    fun applyTemplate(
        currentValue: TextFieldValue,
        template: MarkdownTemplate
    ): TextFieldValue {
        return when {
            hasSelection -> applyTemplateToSelection(currentValue, template)
            text.isEmpty() || isAtLineStart() -> insertTemplateAtPosition()
            else -> insertTemplateAtPosition() // Cursor in middle
        }
    }
}
```

**Benefits**:
- Single source of truth for text operations
- Context-aware template insertion (selection vs empty line vs cursor position)
- Proper cursor positioning for all scenarios
- Smart external content synchronization
- Prevention of cursor jumping and race conditions

### Phase 2: UI Selection-Aware Template Insertion
**Modified**: `ModernSplitPaneEditor.kt`

**Architecture Changes**:
```kotlin
// OLD: Template insertion via ViewModel (lost UI context)
onTemplateSelected = { template ->
    viewModel.insertTemplate(template.template, template.cursorPosition)
}

// NEW: Direct UI template application (preserves selection)
val handleTemplateApplication: (MarkdownTemplate) -> Unit = { template ->
    // Apply template using current UI state (selection-aware)
    val newValue = stateManager.applyTemplate(contentFieldValue, template)
    
    // Update UI state immediately (no cursor jumping)
    contentFieldValue = newValue
    
    // Notify ViewModel for auto-save only
    onContentChange(newValue.text)
    onContentWithCursorChange?.invoke(newValue)
}
```

**Key Improvements**:
- UI TextFieldValue is authoritative for immediate operations
- Template operations work directly with current UI selection state
- Eliminated ViewModel/UI state disconnect
- Preserved auto-save functionality through separate callbacks

### Phase 3: Smart Content Detection Refinement
**Enhanced Logic**:
```kotlin
fun shouldSyncExternalContent(
    currentUIValue: TextFieldValue,
    externalContent: String
): Boolean {
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
```

**Benefits**:
- Only syncs on actual external changes
- Preserves cursor for normal user typing
- Handles edge cases properly

### Phase 4: Event-Driven Architecture
**Separation of Concerns**:
```kotlin
// Template operations → Direct UI updates (immediate feedback)
// Auto-save → Background ViewModel updates (persistence)
// Cursor tracking → Lightweight state sync (coordination)
```

**Callback Architecture**:
```kotlin
// NoteDetailScreen.kt
val handleTemplateSelected = { template: MarkdownTemplate ->
    if (useModernEditor) {
        // Modern: Use UI selection-aware template application
        editorTemplateHandler?.invoke(template)
    } else {
        // Legacy: Fallback to ViewModel
        viewModel.insertTemplate(template.template, template.cursorPosition)
    }
    showToolbox = false
}
```

## Implementation Details

### File Modifications Summary

1. **TextEditorStateManager.kt** (NEW)
   - 309 lines of centralized text editing logic
   - Context-aware template insertion
   - Smart cursor positioning
   - External content synchronization

2. **ModernSplitPaneEditor.kt** (MAJOR REFACTOR)
   - Removed problematic LaunchedEffect causing cursor jumping
   - Implemented UI-authoritative TextFieldValue management
   - Added template handler registration system
   - Streamlined external content detection

3. **NoteDetailScreen.kt** (UPDATED)
   - Added template handler bridge for modern/legacy editor modes
   - Simplified callback architecture
   - Preserved backward compatibility

4. **NoteDetailViewModel.kt** (PRESERVED)
   - Maintained existing auto-save functionality
   - Kept cursor tracking for coordination
   - Template insertion methods remain for legacy support

### Smart Template Operations

**Context-Aware Insertion**:
```kotlin
// CASE 1: User has selected text → wrap it
"hello" + Bold → "**hello**"

// CASE 2: Empty line → insert with optimal cursor positioning  
"" + Bold → "**|**" (cursor between markers)

// CASE 3: Cursor in middle → insert at position
"some text|" + Bold → "some text**bold text**|"
```

**Marker Extraction Logic**:
```kotlin
private fun extractTemplateMarkers(template: String): Pair<String, String> {
    return when {
        template.startsWith("**") && template.endsWith("**") -> "**" to "**"
        template.startsWith("*") && !template.startsWith("**") -> "*" to "*"
        template.contains("[") && template.contains("](") -> "[" to "](url)"
        // ... additional patterns
    }
}
```

## Testing Scenarios

### Cursor Positioning Tests
1. **Empty Line Template Insertion**
   - Bold: `**|**` (cursor between markers)
   - Italic: `*|*` (cursor between markers)
   - Link: `[|](url)` (cursor at text position)

2. **Text Selection Template Application**
   - "hello" + Bold → "**hello**" (cursor after)
   - "world" + Link → "[world](url)" (cursor after)

3. **Mid-Text Cursor Insertion**
   - "some text|" + Bold → "some text**bold text**|"

### Race Condition Prevention
1. **Rapid Typing**: No cursor jumping during fast typing
2. **Quick Template Application**: Immediate UI feedback without delays
3. **External Content Changes**: Only syncs on legitimate external updates

### Smart Deletion Integration
- Preserved existing smart backspace functionality
- Single backspace removes placeholder text
- Second backspace removes empty template markers

## Performance Optimizations

### Reduced State Synchronization
- **Before**: Every keystroke triggered ViewModel ↔ UI sync
- **After**: UI is authoritative, ViewModel only for auto-save

### Eliminated LaunchedEffect Loops
- **Before**: Multiple LaunchedEffect watching different states
- **After**: Minimal external content detection only

### Context-Aware Operations
- **Before**: Template insertion always used fallback logic
- **After**: Different logic paths for different contexts

## Debugging Tools

### Template Application Debug Info
```kotlin
data class TemplateApplicationResult(
    val textFieldValue: TextFieldValue,
    val context: TemplateInsertionContext,
    val appliedTemplate: MarkdownTemplate,
    val operationType: String // "selection", "empty_line", "cursor_insertion"
)
```

### Smart Deletion Preview
```kotlin
fun getSmartDeletionPreview(text: String, cursorPosition: Int): SmartDeletionPreview? {
    // Returns preview of what would be deleted with smart backspace
    // Useful for UI feedback or testing
}
```

## Edge Cases Handled

### Template Insertion Edge Cases
1. **Empty Content**: Insert template with optimal positioning
2. **Line Start**: Insert with header-specific logic for # patterns
3. **Line End**: Insert at cursor position
4. **Multiple Templates**: Each insertion is independent and context-aware

### Cursor Position Edge Cases
1. **Text Length Changes**: Clamp cursor position to valid range
2. **Selection Beyond Text**: Handle selection start/end validation
3. **External Content Shorter**: Preserve relative cursor position when possible

### Smart Deletion Edge Cases
1. **Nested Templates**: Only delete outermost template at cursor
2. **User Content**: Preserve user-modified text, only delete placeholders
3. **Empty Templates**: Smart deletion of marker-only templates

## Future Enhancements

### Possible Improvements
1. **Template History**: Undo/redo support for template operations
2. **Multi-Cursor Support**: Handle multiple cursor positions
3. **Real-time Collaboration**: Conflict resolution for simultaneous edits
4. **Advanced Context Detection**: More sophisticated content analysis

### Monitoring Points
1. **Performance**: Monitor TextFieldValue creation frequency
2. **Memory**: Track template handler registrations
3. **User Experience**: Log template application success/failure rates

## Conclusion

The implemented solution successfully addresses all three critical issues:

1. **✅ Cursor Jumping Fixed**: Eliminated problematic external content sync
2. **✅ Manual Selection Working**: Direct UI template application preserves selection state  
3. **✅ Race Conditions Eliminated**: Centralized state management with clear separation of concerns

The architecture now provides:
- **Immediate Feedback**: Template operations apply instantly to UI
- **Context Awareness**: Different behavior for selection vs empty line vs cursor position
- **Robust Synchronization**: External content changes only sync when appropriate
- **Backward Compatibility**: Legacy editor mode still works
- **Performance**: Reduced state synchronization overhead

`★ Insight ─────────────────────────────────────`
The key architectural insight was recognizing that text editing requires UI state to be authoritative for immediate operations, while background services (auto-save) should be reactive consumers rather than controllers of the editing state.
`─────────────────────────────────────────────────`