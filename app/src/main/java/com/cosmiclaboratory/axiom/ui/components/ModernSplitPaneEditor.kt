package com.cosmiclaboratory.axiom.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.TextFieldValue
import com.cosmiclaboratory.axiom.utils.SmartDeletionHandler
import com.cosmiclaboratory.axiom.utils.TextEditorStateManager
import com.cosmiclaboratory.axiom.domain.model.MarkdownTemplate
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.cosmiclaboratory.axiom.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Modern Split Pane Editor - Premium note-taking experience
 * 
 * Features:
 * - Clean, distraction-free interface
 * - Enhanced typography and spacing
 * - Smart metadata display
 * - Smooth animations and micro-interactions
 * - Optimized for extended writing sessions
 * 
 * Implementation Log: Phase 2 - Core Editor Components Redesign
 * - Removed excessive borders and visual clutter
 * - Enhanced focus states and transitions
 * - Implemented intelligent metadata display
 * - Added smooth pane transitions with preserved functionality
 */
@Composable
fun ModernSplitPaneEditor(
    title: String,
    content: String,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isPreviewVisible: Boolean = true,
    onPreviewToggle: () -> Unit = {},
    showToolbox: Boolean = false,
    onToolboxToggle: () -> Unit = {},
    wordCount: Int = 0,
    characterCount: Int = 0,
    lastModified: Long? = null,
    // Enhanced cursor tracking callbacks
    onContentWithCursorChange: ((TextFieldValue) -> Unit)? = null,
    onTitleWithCursorChange: ((TextFieldValue) -> Unit)? = null,
    // Template handler registration callback
    onTemplateHandlerReady: (((MarkdownTemplate) -> Unit) -> Unit)? = null
) {
    // Store template handler for child components
    var contentEditorTemplateHandler by remember { mutableStateOf<((MarkdownTemplate) -> Unit)?>(null) }
    
    // Register template handler with parent
    LaunchedEffect(onTemplateHandlerReady) {
        onTemplateHandlerReady?.let { callback ->
            contentEditorTemplateHandler?.let { handler ->
                callback(handler)
            }
        }
    }
    var targetPaneRatio by remember { mutableFloatStateOf(0.35f) } // 35% for preview, 65% for editor
    val animatedPaneRatio by animateFloatAsState(
        targetValue = targetPaneRatio,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "pane_ratio_animation"
    )
    
    val editorListState = rememberLazyListState()
    val previewListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // Optimized synchronized scrolling with throttling
    var lastSyncTime by remember { mutableLongStateOf(0L) }
    var isScrollSyncing by remember { mutableStateOf(false) }
    
    LaunchedEffect(editorListState.firstVisibleItemIndex, editorListState.firstVisibleItemScrollOffset) {
        if (isPreviewVisible && !isScrollSyncing) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastSyncTime > 33) { // Throttle to max 30 FPS for smoother sync
                lastSyncTime = currentTime
                isScrollSyncing = true
                
                coroutineScope.launch {
                    try {
                        // Improved scroll ratio calculation for enhanced preview
                        val scrollRatio = 0.85f // Better sync ratio for enhanced animations
                        val targetOffset = (editorListState.firstVisibleItemScrollOffset * scrollRatio).toInt()
                        
                        // Use animateScrollToItem for smoother preview updates
                        previewListState.animateScrollToItem(
                            index = editorListState.firstVisibleItemIndex,
                            scrollOffset = targetOffset
                        )
                    } catch (e: Exception) {
                        // Fallback to immediate scroll if animation fails
                        previewListState.scrollToItem(
                            editorListState.firstVisibleItemIndex,
                            (editorListState.firstVisibleItemScrollOffset * 0.85f).toInt()
                        )
                    } finally {
                        isScrollSyncing = false
                    }
                }
            }
        }
    }
    
    if (isPreviewVisible) {
        Column(
            modifier = modifier.fillMaxSize()
        ) {
            // Preview Pane (Top)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(animatedPaneRatio)
            ) {
                ModernPreviewPane(
                    content = content,
                    listState = previewListState,
                    onPreviewToggle = onPreviewToggle,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // Modern Resizable Divider (Horizontal)
            ModernResizableDivider(
                onDrag = { delta ->
                    val newRatio = (targetPaneRatio + delta).coerceIn(0.2f, 0.7f)
                    targetPaneRatio = newRatio
                },
                modifier = Modifier.fillMaxWidth(),
                isHorizontal = true
            )
            
            // Editor Pane (Bottom)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f - animatedPaneRatio)
            ) {
                ModernEditorPane(
                    title = title,
                    content = content,
                    onTitleChange = onTitleChange,
                    onContentChange = onContentChange,
                    onContentWithCursorChange = onContentWithCursorChange,
                    onTitleWithCursorChange = onTitleWithCursorChange,
                    onTemplateHandlerReady = { handler -> 
                        contentEditorTemplateHandler = handler
                        onTemplateHandlerReady?.invoke { tmpl -> handler(tmpl) }
                    },
                    listState = editorListState,
                    showToolbox = showToolbox,
                    onToolboxToggle = onToolboxToggle,
                    wordCount = wordCount,
                    characterCount = characterCount,
                    lastModified = lastModified,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    } else {
        // Single pane editor with full screen experience
        ModernEditorPane(
            title = title,
            content = content,
            onTitleChange = onTitleChange,
            onContentChange = onContentChange,
            onContentWithCursorChange = onContentWithCursorChange,
            onTitleWithCursorChange = onTitleWithCursorChange,
            onTemplateHandlerReady = { handler -> 
                contentEditorTemplateHandler = handler
                onTemplateHandlerReady?.invoke { tmpl -> handler(tmpl) }
            },
            listState = editorListState,
            showToolbox = showToolbox,
            onToolboxToggle = onToolboxToggle,
            wordCount = wordCount,
            characterCount = characterCount,
            lastModified = lastModified,
            modifier = modifier.fillMaxSize()
        )
    }
}

@Composable
private fun ModernEditorPane(
    title: String,
    content: String,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onContentWithCursorChange: ((TextFieldValue) -> Unit)?,
    onTitleWithCursorChange: ((TextFieldValue) -> Unit)?,
    onTemplateHandlerReady: ((MarkdownTemplate) -> Unit) -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState,
    showToolbox: Boolean,
    onToolboxToggle: () -> Unit,
    wordCount: Int,
    characterCount: Int,
    lastModified: Long?,
    modifier: Modifier = Modifier
) {
    val titleFocusRequester = remember { FocusRequester() }
    val contentFocusRequester = remember { FocusRequester() }
    var titleFocused by remember { mutableStateOf(false) }
    var contentFocused by remember { mutableStateOf(false) }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)  // More generous padding for premium feel
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Smart Metadata Header
            ModernMetadataHeader(
                wordCount = wordCount,
                characterCount = characterCount,
                lastModified = lastModified,
                onToolboxToggle = onToolboxToggle,
                showToolbox = showToolbox,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Modern Title Input
            ModernTitleInput(
                title = title,
                onTitleChange = onTitleChange,
                onTitleWithCursorChange = onTitleWithCursorChange,
                onNext = { contentFocusRequester.requestFocus() },
                focusRequester = titleFocusRequester,
                onFocusChanged = { titleFocused = it },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Modern Content Editor
            ModernContentEditor(
                content = content,
                onContentChange = onContentChange,
                onContentWithCursorChange = onContentWithCursorChange,
                onTemplateHandlerReady = onTemplateHandlerReady,
                focusRequester = contentFocusRequester,
                onFocusChanged = { contentFocused = it },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ModernMetadataHeader(
    wordCount: Int,
    characterCount: Int,
    lastModified: Long?,
    onToolboxToggle: () -> Unit,
    showToolbox: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Document Statistics
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Word count
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "$wordCount",
                    style = EditorMetadataStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "words",
                    style = EditorSubtleHintStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            
            // Character count
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "$characterCount",
                    style = EditorMetadataStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "chars",
                    style = EditorSubtleHintStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            
            // Last modified
            lastModified?.let { timestamp ->
                val formattedTime = remember(timestamp) {
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
                }
                Text(
                    text = "Modified $formattedTime",
                    style = EditorSubtleHintStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
        
        // Toolbox toggle
        IconButton(
            onClick = onToolboxToggle,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Build,
                contentDescription = "Markdown Toolbox",
                modifier = Modifier.size(18.dp),
                tint = if (showToolbox)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun ModernTitleInput(
    title: String,
    onTitleChange: (String) -> Unit,
    onTitleWithCursorChange: ((TextFieldValue) -> Unit)?,
    onNext: () -> Unit,
    focusRequester: FocusRequester,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    var titleFieldValue by remember(title) {
        mutableStateOf(TextFieldValue(text = title, selection = androidx.compose.ui.text.TextRange(title.length)))
    }
    
    // Update TextFieldValue when external title changes
    LaunchedEffect(title) {
        if (titleFieldValue.text != title) {
            titleFieldValue = TextFieldValue(text = title, selection = androidx.compose.ui.text.TextRange(title.length))
        }
    }
    
    BasicTextField(
        value = titleFieldValue,
        onValueChange = { newValue ->
            titleFieldValue = newValue
            // Call both callbacks for backward compatibility
            onTitleChange(newValue.text)
            onTitleWithCursorChange?.invoke(newValue)
        },
        modifier = modifier
            .focusRequester(focusRequester)
            .onFocusChanged { 
                focused = it.isFocused
                onFocusChanged(it.isFocused)
            },
        textStyle = ModernNoteTitleStyle.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { onNext() }),
        singleLine = true,
        decorationBox = { innerTextField ->
            Box {
                if (titleFieldValue.text.isEmpty()) {
                    Text(
                        text = "Untitled Note",
                        style = ModernNoteTitleStyle.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    )
                }
                innerTextField()
            }
        }
    )
}

@Composable
private fun ModernContentEditor(
    content: String,
    onContentChange: (String) -> Unit,
    onContentWithCursorChange: ((TextFieldValue) -> Unit)?,
    onTemplateHandlerReady: (((MarkdownTemplate) -> Unit) -> Unit)? = null,
    focusRequester: FocusRequester,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    
    // Initialize with cursor at beginning for better UX
    var contentFieldValue by remember {
        mutableStateOf(TextFieldValue(text = content, selection = androidx.compose.ui.text.TextRange(0)))
    }
    
    // Centralized state manager for template operations
    val stateManager = remember { TextEditorStateManager() }
    
    // Template application handler with UI selection awareness
    val handleTemplateApplication: (MarkdownTemplate) -> Unit = remember {
        { template: MarkdownTemplate ->
            // Apply template using current UI state (selection-aware)
            val newValue = stateManager.applyTemplate(contentFieldValue, template)
            
            // Update UI state immediately (no cursor jumping)
            contentFieldValue = newValue
            
            // Notify ViewModel for auto-save
            onContentChange(newValue.text)
            onContentWithCursorChange?.invoke(newValue)
        }
    }
    
    // Register template handler with parent component
    LaunchedEffect(Unit) {
        onTemplateHandlerReady?.invoke(handleTemplateApplication)
    }
    
    
    // Smart external content synchronization using state manager
    LaunchedEffect(content) {
        // Use state manager to determine if external content should sync
        if (stateManager.shouldSyncExternalContent(contentFieldValue, content)) {
            // Only sync for major external changes, preserving cursor when possible
            contentFieldValue = stateManager.createTextFieldValueFromExternalContent(
                externalContent = content,
                preserveCursorAtEnd = false // Try to preserve relative cursor position
            )
        }
        // For normal typing differences, ignore external sync to preserve cursor
    }
    
    BasicTextField(
        value = contentFieldValue,
        onValueChange = { newValue ->
            // Use state manager for smart text handling 
            val processedValue = stateManager.handleTextChange(
                oldValue = contentFieldValue,
                newValue = newValue
            )
            
            // Update local state immediately (authoritative)
            contentFieldValue = processedValue
            
            // Notify ViewModel for auto-save only (no cursor feedback)
            onContentChange(processedValue.text)
            onContentWithCursorChange?.invoke(processedValue)
        },
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .onFocusChanged { 
                focused = it.isFocused
                onFocusChanged(it.isFocused)
            },
        textStyle = EditorContentStyle.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { innerTextField ->
            Box(modifier = Modifier.fillMaxSize()) {
                if (contentFieldValue.text.isEmpty()) {
                    Text(
                        text = "Start writing your thoughts...\n\nMarkdown is supported for formatting.\nUse the toolbox for quick shortcuts.",
                        style = EditorPlaceholderStyle.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                }
                innerTextField()
            }
        }
    )
}


@Composable
private fun ModernPreviewPane(
    content: String,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onPreviewToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                RoundedCornerShape(8.dp)
            )
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Preview Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Live Preview",
                    style = EditorHeaderStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                IconButton(
                    onClick = onPreviewToggle,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Visibility,
                        contentDescription = "Hide Preview",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Enhanced Preview Content with Scroll Synchronization
            EnhancedMarkdownPreview(
                content = content,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                showDebug = false,
                enableAnimations = false, // Disable animations for better scroll sync performance
                readerMode = false,
                useScrollableContainer = true,
                listState = listState // Enable scroll synchronization
            )
        }
    }
}

@Composable
private fun ModernResizableDivider(
    onDrag: (Float) -> Unit,
    modifier: Modifier = Modifier,
    isHorizontal: Boolean = false
) {
    var isDragging by remember { mutableStateOf(false) }
    
    Box(
        modifier = modifier
            .then(
                if (isHorizontal) Modifier.height(16.dp) else Modifier.width(16.dp)
            )
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false }
                ) { _, dragAmount ->
                    val dragDelta = if (isHorizontal) dragAmount.y else dragAmount.x
                    val containerSize = if (isHorizontal) size.height else size.width
                    val normalizedDelta = dragDelta / containerSize.toFloat()
                    onDrag(normalizedDelta * 0.3f)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Minimal divider line
        Box(
            modifier = if (isHorizontal) {
                Modifier
                    .fillMaxWidth()
                    .height(if (isDragging) 2.dp else 1.dp)
                    .background(
                        if (isDragging) 
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        else 
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        RoundedCornerShape(1.dp)
                    )
            } else {
                Modifier
                    .fillMaxHeight()
                    .width(if (isDragging) 2.dp else 1.dp)
                    .background(
                        if (isDragging) 
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        else 
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        RoundedCornerShape(1.dp)
                    )
            }
        )
        
        // Subtle drag handle that appears on hover/drag
        if (isDragging) {
            Box(
                modifier = Modifier
                    .size(
                        if (isHorizontal) 40.dp else 24.dp,
                        if (isHorizontal) 24.dp else 40.dp
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DragIndicator,
                    contentDescription = "Resize",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}