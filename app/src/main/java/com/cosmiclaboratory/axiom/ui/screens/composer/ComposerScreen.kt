package com.cosmiclaboratory.axiom.ui.screens.composer

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cosmiclaboratory.axiom.ui.design.AxiomDimens
import com.cosmiclaboratory.axiom.ui.design.ReadingColumn
import com.cosmiclaboratory.axiom.ui.components.VoiceInputFab
import com.cosmiclaboratory.axiom.ui.design.components.*
import com.cosmiclaboratory.axiom.ui.theme.AxiomTheme

/**
 * The writing surface.
 *
 * Three modes rather than a split pane: on a phone, side-by-side preview halves
 * the writing measure, which is the most common complaint about mobile markdown
 * editors. A labelled toggle costs one tap and gives full measure in both.
 */
@Composable
fun ComposerScreen(
    onBack: () -> Unit,
    onDone: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ComposerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = AxiomTheme.colors
    val focusMode = state.mode == ComposerMode.Focus

    BackHandler { viewModel.saveAndExit(onBack) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(c.canvas)
            .testTag("screen:composer")
    ) {
        // Chrome retracts in Focus mode so only the words remain.
        AnimatedVisibility(
            visible = !focusMode,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            ComposerTopBar(
                saveState = state.saveState,
                mode = state.mode,
                onModeChange = viewModel::setMode,
                onBack = { viewModel.saveAndExit(onBack) },
                onDone = { viewModel.complete(onDone) },
                onRetrySave = viewModel::retrySave
            )
        }

        Box(Modifier.weight(1f)) {
            ReadingColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = AxiomTheme.space.composerGutter)
            ) {
                Column {
                    state.promptText?.let { prompt ->
                        PromptHeader(prompt, collapsed = focusMode)
                    }

                    if (state.mode == ComposerMode.Preview) {
                        Spacer(Modifier.height(AxiomTheme.space.base))
                        if (state.title.isNotBlank()) {
                            Text(state.title, style = AxiomTheme.type.readingTitle, color = c.ink)
                            Spacer(Modifier.height(AxiomTheme.space.sm))
                        }
                        AxiomMarkdown(source = state.body)
                    } else {
                        TitleField(
                            value = state.title,
                            onChange = viewModel::setTitle,
                            visible = !focusMode
                        )
                        BodyField(
                            value = state.body,
                            onChange = viewModel::setBody
                        )
                    }
                    Spacer(Modifier.height(160.dp))
                }
            }

            if (focusMode) {
                // The single way out of an immersive mode must always be visible.
                TextButton(
                    onClick = { viewModel.setMode(ComposerMode.Write) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = AxiomTheme.space.base)
                ) {
                    Text("Exit focus", style = AxiomTheme.type.uiLabel, color = c.inkFaint)
                }
            }
        }

        AnimatedVisibility(visible = !focusMode, enter = fadeIn(), exit = fadeOut()) {
            ComposerBottomBar(
                wordCount = state.wordCount,
                mood = state.mood,
                onMoodChange = viewModel::setMood,
                onTranscript = viewModel::appendTranscript
            )
        }
    }
}

@Composable
private fun ComposerTopBar(
    saveState: SaveState,
    mode: ComposerMode,
    onModeChange: (ComposerMode) -> Unit,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onRetrySave: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AxiomTheme.space.sm, vertical = AxiomTheme.space.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AxiomIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack)
        // Always visible, never silent — the fix for autosave you can't trust.
        SaveStateIndicator(saveState, onRetry = onRetrySave)
        Spacer(Modifier.weight(1f))
        AxiomSegmented(
            options = listOf("Write", "Preview", "Focus"),
            selectedIndex = mode.ordinal,
            onSelect = { onModeChange(ComposerMode.entries[it]) }
        )
        Spacer(Modifier.width(AxiomTheme.space.sm))
        TextButton(onClick = onDone) {
            Text("Done", style = AxiomTheme.type.uiLabel, color = AxiomTheme.colors.accent)
        }
    }
}

@Composable
private fun PromptHeader(prompt: String, collapsed: Boolean) {
    AnimatedVisibility(visible = !collapsed) {
        Column {
            Spacer(Modifier.height(AxiomTheme.space.base))
            Row {
                Box(
                    Modifier
                        .width(3.dp)
                        .heightIn(min = 24.dp)
                        .background(AxiomTheme.colors.accent.copy(alpha = 0.5f))
                )
                Spacer(Modifier.width(AxiomTheme.space.md))
                Text(
                    prompt,
                    style = AxiomTheme.type.readingSubtitle,
                    color = AxiomTheme.colors.inkMuted
                )
            }
        }
    }
}

@Composable
private fun TitleField(value: String, onChange: (String) -> Unit, visible: Boolean) {
    if (!visible) return
    val c = AxiomTheme.colors
    Spacer(Modifier.height(AxiomTheme.space.base))
    Box {
        if (value.isEmpty()) {
            Text("Title (optional)", style = AxiomTheme.type.readingTitle, color = c.inkFaint)
        }
        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = AxiomTheme.type.readingTitle.copy(color = c.ink),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(c.accent),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("field:title")
        )
    }
}

@Composable
private fun BodyField(value: String, onChange: (String) -> Unit) {
    val c = AxiomTheme.colors
    val t = AxiomTheme.type
    // Rebuilt only when the palette or type scale changes, not per keystroke.
    val transformation = remember(c, t) { MarkdownSyntaxHighlight(c, t) }

    Spacer(Modifier.height(AxiomTheme.space.md))
    Box {
        if (value.isEmpty()) {
            Text("Start writing…", style = t.readingBody, color = c.inkFaint)
        }
        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = t.readingBody.copy(color = c.ink),
            visualTransformation = transformation,
            cursorBrush = androidx.compose.ui.graphics.SolidColor(c.accent),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("field:body")
        )
    }
}

@Composable
private fun ComposerBottomBar(
    wordCount: Int,
    mood: Int?,
    onMoodChange: (Int) -> Unit,
    onTranscript: (String) -> Unit
) {
    val c = AxiomTheme.colors
    var showMood by remember { mutableStateOf(false) }

    Column {
        AnimatedVisibility(visible = showMood) {
            Column(Modifier.padding(horizontal = AxiomTheme.space.screenH)) {
                MoodPicker(
                    value = mood,
                    onChange = { onMoodChange(it); showMood = false },
                    compact = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(AxiomTheme.space.sm))
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(c.surface)
                .padding(horizontal = AxiomTheme.space.screenH, vertical = AxiomTheme.space.sm)
                .imePadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$wordCount",
                style = AxiomTheme.type.uiNumeric,
                color = c.inkFaint
            )
            Spacer(Modifier.width(4.dp))
            Text("words", style = AxiomTheme.type.uiMeta, color = c.inkFaint)

            Spacer(Modifier.weight(1f))

            TextButton(onClick = { showMood = !showMood }) {
                if (mood != null) {
                    MoodDot(mood = mood, size = 12.dp)
                    Spacer(Modifier.width(6.dp))
                    Text(moodLabel(mood), style = AxiomTheme.type.uiLabel, color = c.ink)
                } else {
                    Text("Add mood", style = AxiomTheme.type.uiLabel, color = c.inkMuted)
                }
            }
            // The real on-device recogniser, not a placeholder: results append
            // at the end of the body via the composer's normal save path.
            VoiceInputFab(onVoiceResult = onTranscript)
        }
    }
}
