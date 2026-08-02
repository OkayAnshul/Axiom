package com.cosmiclaboratory.axiom.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cosmiclaboratory.axiom.domain.model.MarkdownTemplate
import com.cosmiclaboratory.axiom.ui.theme.AxiomAsh
import com.cosmiclaboratory.axiom.ui.theme.AxiomCyan
import com.cosmiclaboratory.axiom.ui.theme.AxiomGraphite
import com.cosmiclaboratory.axiom.ui.theme.AxiomGraphiteRaised
import com.cosmiclaboratory.axiom.ui.theme.AxiomGraphiteSoft
import com.cosmiclaboratory.axiom.ui.theme.AxiomIvory
import com.cosmiclaboratory.axiom.ui.theme.AxiomMist
import com.cosmiclaboratory.axiom.ui.theme.AxiomOledBlack
import com.cosmiclaboratory.axiom.ui.theme.AxiomSun
import com.cosmiclaboratory.axiom.ui.theme.EditorContentStyle
import com.cosmiclaboratory.axiom.ui.theme.EditorHeaderStyle
import com.cosmiclaboratory.axiom.ui.theme.EditorMetadataStyle
import com.cosmiclaboratory.axiom.ui.theme.EditorPlaceholderStyle
import com.cosmiclaboratory.axiom.ui.theme.EditorSubtleHintStyle
import com.cosmiclaboratory.axiom.ui.theme.ModernNoteTitleStyle
import com.cosmiclaboratory.axiom.utils.PlainTextToMarkdownConverter
import com.cosmiclaboratory.axiom.utils.TextEditorStateManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

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
    onContentWithCursorChange: ((TextFieldValue) -> Unit)? = null,
    onTitleWithCursorChange: ((TextFieldValue) -> Unit)? = null,
    onTemplateHandlerReady: (((MarkdownTemplate) -> Unit) -> Unit)? = null
) {
    var targetPaneRatio by remember { mutableFloatStateOf(0.38f) }
    var focusMode by remember { mutableStateOf(false) }
    val animatedPaneRatio by animateFloatAsState(
        targetValue = targetPaneRatio,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "axiom_reader_ratio"
    )
    val previewListState = rememberLazyListState()
    val generatedMarkdown = remember(content, title) {
        PlainTextToMarkdownConverter.convert(content, title)
    }
    val readerVisible = isPreviewVisible && !focusMode

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(EditorBackdrop())
    ) {
        if (readerVisible) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                ModernPreviewPane(
                    title = title,
                    markdown = generatedMarkdown,
                    listState = previewListState,
                    onPreviewToggle = onPreviewToggle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(animatedPaneRatio)
                )

                ModernResizableDivider(
                    onDrag = { delta -> targetPaneRatio = (targetPaneRatio + delta).coerceIn(0.24f, 0.68f) },
                    modifier = Modifier.fillMaxWidth(),
                    isHorizontal = true
                )

                ModernEditorPane(
                    title = title,
                    content = content,
                    onTitleChange = onTitleChange,
                    onContentChange = onContentChange,
                    onContentWithCursorChange = onContentWithCursorChange,
                    onTitleWithCursorChange = onTitleWithCursorChange,
                    onTemplateHandlerReady = onTemplateHandlerReady,
                    showToolbox = showToolbox,
                    onToolboxToggle = onToolboxToggle,
                    onPreviewToggle = onPreviewToggle,
                    isPreviewVisible = isPreviewVisible,
                    focusMode = focusMode,
                    onFocusModeToggle = { focusMode = !focusMode },
                    wordCount = wordCount,
                    characterCount = characterCount,
                    lastModified = lastModified,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f - animatedPaneRatio)
                )
            }
        } else {
            ModernEditorPane(
                title = title,
                content = content,
                onTitleChange = onTitleChange,
                onContentChange = onContentChange,
                onContentWithCursorChange = onContentWithCursorChange,
                onTitleWithCursorChange = onTitleWithCursorChange,
                onTemplateHandlerReady = onTemplateHandlerReady,
                showToolbox = showToolbox,
                onToolboxToggle = onToolboxToggle,
                onPreviewToggle = onPreviewToggle,
                isPreviewVisible = isPreviewVisible,
                focusMode = focusMode,
                onFocusModeToggle = { focusMode = !focusMode },
                wordCount = wordCount,
                characterCount = characterCount,
                lastModified = lastModified,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun EditorBackdrop(): Brush {
    return Brush.verticalGradient(
        colors = listOf(
            AxiomOledBlack,
            Color(0xFF020707),
            AxiomOledBlack
        )
    )
}

@Composable
private fun ModernEditorPane(
    title: String,
    content: String,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onContentWithCursorChange: ((TextFieldValue) -> Unit)?,
    onTitleWithCursorChange: ((TextFieldValue) -> Unit)?,
    onTemplateHandlerReady: (((MarkdownTemplate) -> Unit) -> Unit)?,
    showToolbox: Boolean,
    onToolboxToggle: () -> Unit,
    onPreviewToggle: () -> Unit,
    isPreviewVisible: Boolean,
    focusMode: Boolean,
    onFocusModeToggle: () -> Unit,
    wordCount: Int,
    characterCount: Int,
    lastModified: Long?,
    modifier: Modifier = Modifier
) {
    val titleFocusRequester = remember { FocusRequester() }
    val contentFocusRequester = remember { FocusRequester() }
    val readingMinutes = remember(wordCount) { max(1, (wordCount + 179) / 180) }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        AxiomCyan.copy(alpha = 0.26f),
                        AxiomGraphiteSoft.copy(alpha = 0.48f),
                        AxiomSun.copy(alpha = 0.16f)
                    )
                ),
                shape = RoundedCornerShape(28.dp)
            ),
        color = AxiomGraphite.copy(alpha = 0.92f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = if (focusMode) 22.dp else 18.dp,
                        top = if (focusMode) 24.dp else 18.dp,
                        end = if (focusMode) 22.dp else 18.dp,
                        bottom = 92.dp
                    )
            ) {
                AnimatedVisibility(
                    visible = !focusMode,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    ModernMetadataHeader(
                        wordCount = wordCount,
                        characterCount = characterCount,
                        readingMinutes = readingMinutes,
                        lastModified = lastModified,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(if (focusMode) 12.dp else 20.dp))

                ModernTitleInput(
                    title = title,
                    onTitleChange = onTitleChange,
                    onTitleWithCursorChange = onTitleWithCursorChange,
                    onNext = { contentFocusRequester.requestFocus() },
                    focusRequester = titleFocusRequester,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(18.dp))

                ModernContentEditor(
                    content = content,
                    onContentChange = onContentChange,
                    onContentWithCursorChange = onContentWithCursorChange,
                    onTemplateHandlerReady = onTemplateHandlerReady,
                    focusRequester = contentFocusRequester,
                    modifier = Modifier.weight(1f)
                )
            }

            BottomCommandDock(
                showToolbox = showToolbox,
                isPreviewVisible = isPreviewVisible,
                focusMode = focusMode,
                onToolboxToggle = onToolboxToggle,
                onPreviewToggle = onPreviewToggle,
                onFocusModeToggle = onFocusModeToggle,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(14.dp)
            )
        }
    }
}

@Composable
private fun ModernMetadataHeader(
    wordCount: Int,
    characterCount: Int,
    readingMinutes: Int,
    lastModified: Long?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        QuietMetric(value = "$wordCount", label = "words")
        QuietMetric(value = "$readingMinutes", label = "min read")
        QuietMetric(value = "$characterCount", label = "chars")

        Spacer(Modifier.weight(1f))

        lastModified?.let { timestamp ->
            val formattedTime = remember(timestamp) {
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
            }
            Text(
                text = "Saved $formattedTime",
                style = EditorSubtleHintStyle,
                color = AxiomAsh,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun QuietMetric(value: String, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(text = value, style = EditorMetadataStyle, color = AxiomIvory)
        Text(text = label, style = EditorSubtleHintStyle, color = AxiomAsh)
    }
}

@Composable
private fun ModernTitleInput(
    title: String,
    onTitleChange: (String) -> Unit,
    onTitleWithCursorChange: ((TextFieldValue) -> Unit)?,
    onNext: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    var titleFieldValue by remember(title) {
        mutableStateOf(TextFieldValue(text = title, selection = TextRange(title.length)))
    }

    LaunchedEffect(title) {
        if (titleFieldValue.text != title) {
            titleFieldValue = TextFieldValue(text = title, selection = TextRange(title.length))
        }
    }

    BasicTextField(
        value = titleFieldValue,
        onValueChange = { newValue ->
            titleFieldValue = newValue
            onTitleChange(newValue.text)
            onTitleWithCursorChange?.invoke(newValue)
        },
        modifier = modifier.focusRequester(focusRequester),
        textStyle = ModernNoteTitleStyle.copy(color = AxiomIvory),
        cursorBrush = SolidColor(AxiomCyan),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { onNext() }),
        singleLine = true,
        decorationBox = { innerTextField ->
            Box {
                if (titleFieldValue.text.isEmpty()) {
                    Text(
                        text = "Untitled reflection",
                        style = ModernNoteTitleStyle.copy(color = AxiomAsh)
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
    onTemplateHandlerReady: (((MarkdownTemplate) -> Unit) -> Unit)?,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    var contentFieldValue by remember {
        mutableStateOf(TextFieldValue(text = content, selection = TextRange(content.length)))
    }
    val stateManager = remember { TextEditorStateManager() }

    val handleTemplateApplication: (MarkdownTemplate) -> Unit = remember {
        { template ->
            val newValue = stateManager.applyTemplate(contentFieldValue, template)
            contentFieldValue = newValue
            onContentChange(newValue.text)
            onContentWithCursorChange?.invoke(newValue)
        }
    }

    LaunchedEffect(Unit) {
        onTemplateHandlerReady?.invoke(handleTemplateApplication)
    }

    LaunchedEffect(content) {
        if (stateManager.shouldSyncExternalContent(contentFieldValue, content)) {
            contentFieldValue = stateManager.createTextFieldValueFromExternalContent(
                externalContent = content,
                preserveCursorAtEnd = false
            )
        }
    }

    BasicTextField(
        value = contentFieldValue,
        onValueChange = { newValue ->
            val processedValue = stateManager.handleTextChange(
                oldValue = contentFieldValue,
                newValue = newValue
            )
            contentFieldValue = processedValue
            onContentChange(processedValue.text)
            onContentWithCursorChange?.invoke(processedValue)
        },
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .onFocusChanged { },
        textStyle = EditorContentStyle.copy(color = AxiomIvory),
        cursorBrush = SolidColor(AxiomCyan),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(22.dp))
                    .background(AxiomOledBlack.copy(alpha = 0.26f))
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                if (contentFieldValue.text.isEmpty()) {
                    Text(
                        text = "Start with what happened, what changed, or what you want to remember.",
                        style = EditorPlaceholderStyle.copy(color = AxiomAsh)
                    )
                }
                innerTextField()
            }
        }
    )
}

@Composable
private fun ModernPreviewPane(
    title: String,
    markdown: String,
    listState: LazyListState,
    onPreviewToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .border(
                1.dp,
                Brush.horizontalGradient(
                    listOf(AxiomSun.copy(alpha = 0.18f), AxiomCyan.copy(alpha = 0.18f))
                ),
                RoundedCornerShape(28.dp)
            ),
        color = AxiomGraphiteRaised.copy(alpha = 0.88f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(AxiomCyan)
                    )
                    Column {
                        Text("Reader", style = EditorHeaderStyle, color = AxiomIvory)
                        Text(
                            text = title.ifBlank { "Polished page" },
                            style = EditorSubtleHintStyle,
                            color = AxiomAsh,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(onClick = onPreviewToggle, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Visibility,
                        contentDescription = "Hide reader",
                        tint = AxiomMist,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            EnhancedMarkdownPreview(
                content = markdown,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                showDebug = false,
                enableAnimations = false,
                readerMode = false,
                useScrollableContainer = true,
                listState = listState
            )
        }
    }
}

@Composable
private fun BottomCommandDock(
    showToolbox: Boolean,
    isPreviewVisible: Boolean,
    focusMode: Boolean,
    onToolboxToggle: () -> Unit,
    onPreviewToggle: () -> Unit,
    onFocusModeToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, AxiomGraphiteSoft, RoundedCornerShape(24.dp)),
        color = AxiomOledBlack.copy(alpha = 0.92f),
        tonalElevation = 0.dp,
        shadowElevation = 10.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DockAction(
                label = "Style",
                icon = Icons.Filled.Build,
                active = showToolbox,
                onClick = onToolboxToggle,
                modifier = Modifier.weight(1f)
            )
            DockAction(
                label = "Reader",
                icon = Icons.AutoMirrored.Filled.MenuBook,
                active = isPreviewVisible,
                onClick = onPreviewToggle,
                modifier = Modifier.weight(1f)
            )
            DockAction(
                label = if (focusMode) "Full" else "Focus",
                icon = if (focusMode) Icons.Filled.CloseFullscreen else Icons.Filled.OpenInFull,
                active = focusMode,
                onClick = onFocusModeToggle,
                modifier = Modifier.weight(1f)
            )
            DockAction(
                label = "Saved",
                icon = Icons.Filled.CheckCircle,
                active = false,
                onClick = {},
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DockAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tint = if (active) AxiomOledBlack else AxiomMist
    val background = if (active) AxiomCyan else AxiomGraphiteRaised

    Row(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
            .then(if (isHorizontal) Modifier.height(18.dp) else Modifier.width(18.dp))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false }
                ) { _, dragAmount ->
                    val dragDelta = if (isHorizontal) dragAmount.y else dragAmount.x
                    onDrag(dragDelta / 620f)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(if (isDragging) AxiomCyan.copy(alpha = 0.22f) else AxiomGraphiteSoft)
                .padding(horizontal = 18.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.DragIndicator,
                contentDescription = "Resize reader",
                tint = if (isDragging) AxiomCyan else AxiomAsh,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
