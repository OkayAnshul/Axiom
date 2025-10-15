package com.cosmiclaboratory.axiom.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun SplitPaneEditor(
    title: String,
    content: String,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isPreviewVisible: Boolean = true,
    onPreviewToggle: () -> Unit = {},
    showToolbox: Boolean = false,
    onToolboxToggle: () -> Unit = {}
) {
    var targetPaneRatio by remember { mutableFloatStateOf(0.3f) } // 30% for preview, 70% for editor
    val animatedPaneRatio by animateFloatAsState(
        targetValue = targetPaneRatio,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
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
            if (currentTime - lastSyncTime > 50) { // Throttle to max 20 FPS
                lastSyncTime = currentTime
                isScrollSyncing = true
                
                coroutineScope.launch {
                    try {
                        // More intelligent scroll ratio calculation
                        val scrollRatio = 0.8f // Slightly slower preview scroll
                        val targetOffset = (editorListState.firstVisibleItemScrollOffset * scrollRatio).toInt()
                        
                        previewListState.scrollToItem(
                            editorListState.firstVisibleItemIndex,
                            targetOffset
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
                PreviewPane(
                    content = content,
                    listState = previewListState,
                    onPreviewToggle = onPreviewToggle,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // Resizable Divider (Horizontal)
            ResizableDivider(
                onDrag = { delta ->
                    val newRatio = (targetPaneRatio + delta).coerceIn(0.2f, 0.8f)
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
                EditorPane(
                    title = title,
                    content = content,
                    onTitleChange = onTitleChange,
                    onContentChange = onContentChange,
                    listState = editorListState,
                    showToolbox = showToolbox,
                    onToolboxToggle = onToolboxToggle,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    } else {
        // Single pane editor
        EditorPane(
            title = title,
            content = content,
            onTitleChange = onTitleChange,
            onContentChange = onContentChange,
            listState = editorListState,
            showToolbox = showToolbox,
            onToolboxToggle = onToolboxToggle,
            modifier = modifier.fillMaxSize()
        )
    }
}

@Composable
private fun EditorPane(
    title: String,
    content: String,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState,
    showToolbox: Boolean,
    onToolboxToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val titleFocusRequester = remember { FocusRequester() }
    val contentFocusRequester = remember { FocusRequester() }
    
    Column(
        modifier = modifier
            .padding(16.dp)
            .background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(12.dp)
            )
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        // Editor Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Editor",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Row {
                IconButton(
                    onClick = onToolboxToggle,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = "Toolbox",
                        modifier = Modifier.size(16.dp),
                        tint = if (showToolbox) MaterialTheme.colorScheme.primary 
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Title Input
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text("Title") },
            placeholder = { Text("Untitled Note") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = { contentFocusRequester.requestFocus() }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Content Input with enhanced styling
        OutlinedTextField(
            value = content,
            onValueChange = onContentChange,
            label = { Text("Content") },
            placeholder = { 
                Text(
                    text = "Start writing your markdown...\n\nTip: Use the toolbox for quick formatting!",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.5f
                    )
                ) 
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .focusRequester(contentFocusRequester),
            minLines = 15,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4f
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
private fun PreviewPane(
    content: String,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onPreviewToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(start = 8.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)
            .background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(12.dp)
            )
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        // Preview Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Live Preview",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Row {
                IconButton(
                    onClick = onPreviewToggle,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Toggle Preview",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Preview Content
        MarkdownPreview(
            content = content,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            showDebug = false
        )
    }
}

@Composable
private fun ResizableDivider(
    onDrag: (Float) -> Unit,
    modifier: Modifier = Modifier,
    isHorizontal: Boolean = false
) {
    var isDragging by remember { mutableStateOf(false) }
    
    Box(
        modifier = modifier
            .then(
                if (isHorizontal) Modifier.height(12.dp) else Modifier.width(12.dp)
            )
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false }
                ) { _, dragAmount ->
                    // Use dragAmount instead of position for smooth incremental changes
                    val dragDelta = if (isHorizontal) dragAmount.y else dragAmount.x
                    val containerSize = if (isHorizontal) size.height else size.width
                    
                    // Convert to normalized delta (-1.0 to 1.0 range)
                    val normalizedDelta = dragDelta / containerSize.toFloat()
                    onDrag(normalizedDelta * 0.3f) // Scale down for smoother control
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Invisible drag area
        Box(
            modifier = if (isHorizontal) {
                Modifier
                    .fillMaxWidth()
                    .height(12.dp)
            } else {
                Modifier
                    .fillMaxHeight()
                    .width(12.dp)
            }
        )
        
        // Visible divider
        Box(
            modifier = if (isHorizontal) {
                Modifier
                    .fillMaxWidth()
                    .height(if (isDragging) 3.dp else 2.dp)
                    .background(
                        if (isDragging) 
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        else 
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        RoundedCornerShape(1.dp)
                    )
            } else {
                Modifier
                    .fillMaxHeight()
                    .width(if (isDragging) 3.dp else 2.dp)
                    .background(
                        if (isDragging) 
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        else 
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        RoundedCornerShape(1.dp)
                    )
            }
        )
        
        // Drag handle
        Box(
            modifier = Modifier
                .size(
                    if (isHorizontal) 48.dp else 32.dp,
                    if (isHorizontal) 32.dp else 48.dp
                )
                .clip(RoundedCornerShape(24.dp))
                .background(
                    if (isDragging)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                )
                .border(
                    1.dp,
                    if (isDragging)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    else
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    RoundedCornerShape(24.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.DragIndicator,
                contentDescription = "Resize",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}