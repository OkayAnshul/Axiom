package com.cosmiclaboratory.axiom.ui.screens.companion

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cosmiclaboratory.axiom.ui.components.ShowSnackbarOnError
import com.cosmiclaboratory.axiom.ui.design.AxiomError
import com.cosmiclaboratory.axiom.ui.design.components.*
import com.cosmiclaboratory.axiom.ui.design.rememberLocalized
import com.cosmiclaboratory.axiom.ui.theme.AxiomTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * The companion conversation — the home surface of the app.
 *
 * User turns are bubbles; COMPANION REPLIES ARE NOT. A reply renders as
 * full-width prose with a thin aiTint rule, so it reads like writing rather
 * than like a chat message — but is unmistakably attributed to the model. On a
 * private journal the user must never have to guess who wrote a sentence.
 *
 * Works fully without an API key: the daily opener, ritual header, typing and
 * saving to the journal are all local. Only the model reply needs the key, and
 * its absence is an inline card with a way forward, never a dead end.
 */
@Composable
fun CompanionScreen(
    onOpenEntry: (Long) -> Unit,
    onConnectAi: () -> Unit,
    onOpenMemories: () -> Unit,
    onOpenSettings: () -> Unit,
    onSaveToJournal: (String) -> Unit,
    onContinueDraft: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CompanionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var showDisclosure by remember { mutableStateOf(false) }

    // Mic permission gates both the composer mic and the hands-free toggle.
    var pendingVoiceAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val micPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) pendingVoiceAction?.invoke()
        pendingVoiceAction = null
    }
    fun withMicPermission(action: () -> Unit) {
        pendingVoiceAction = action
        micPermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    ShowSnackbarOnError(state.voiceError, viewModel::dismissVoiceError)

    LaunchedEffect(state.messages.size, state.streamingText?.length) {
        val extraRows = if (state.sending) 1 else 0
        val lastIndex = state.messages.lastIndex + extraRows
        if (lastIndex >= 0) listState.animateScrollToItem(lastIndex)
    }

    AxiomScaffold(
        title = "Axiom",
        screenTag = "screen:companion",
        modifier = modifier,
        actions = {
            AxiomIconButton(
                icon = if (state.handsFree) Icons.Filled.RecordVoiceOver else Icons.Outlined.RecordVoiceOver,
                label = if (state.handsFree) "Leave hands-free" else "Hands-free conversation",
                onClick = {
                    if (state.handsFree) {
                        viewModel.setHandsFree(false)
                    } else {
                        withMicPermission { viewModel.setHandsFree(true) }
                    }
                },
                tint = if (state.handsFree) AxiomTheme.colors.accent else AxiomTheme.colors.ink
            )
            AxiomTopBarAction(Icons.Outlined.Psychology, "What I remember", onClick = onOpenMemories)
            // Home is where settings belong, and since Today was folded into the
            // conversation this is the only place left to reach them from.
            AxiomTopBarAction(Icons.Outlined.Settings, "Settings", onClick = onOpenSettings)
            if (state.messages.isNotEmpty()) {
                AxiomTopBarAction(Icons.Outlined.DeleteSweep, "Clear conversation", viewModel::clearThread)
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            CompanionRitualHeader(
                streak = state.streak,
                todayMood = state.todayMood,
                todayEmotion = state.todayEmotion,
                todayMoodInferred = state.todayMoodInferred,
                writingDraft = state.writingDraft,
                onRecordMood = viewModel::recordMood,
                onContinueDraft = onContinueDraft,
                onDisclosure = { showDisclosure = true }
            )

            Box(Modifier.weight(1f)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().testTag("list:companion"),
                    contentPadding = PaddingValues(AxiomTheme.space.screenH),
                    verticalArrangement = Arrangement.spacedBy(AxiomTheme.space.base)
                ) {
                    var lastDate: LocalDate? = null
                    state.messages.forEach { message ->
                        val messageDate = message.createdAt.toLocalDate()
                        if (messageDate != lastDate) {
                            lastDate = messageDate
                            item(key = "date-$messageDate") { DateSeparator(messageDate) }
                        }
                        item(key = message.id) {
                            if (message.isUser) {
                                CompanionUserBubble(
                                    text = message.text,
                                    onSaveToJournal = { onSaveToJournal(message.text) }
                                )
                            } else {
                                CompanionReplyBlock(
                                    text = message.text,
                                    citedIds = message.citedEntryIds,
                                    citedEntries = state.citedEntries,
                                    onOpenEntry = onOpenEntry,
                                    onSaveToJournal = { onSaveToJournal(message.text) },
                                    onPlay = if (state.ttsAvailable) {
                                        { viewModel.speakMessage(message.text) }
                                    } else null
                                )
                            }
                        }
                    }
                    if (state.sending) {
                        item("thinking") {
                            val streaming = state.streamingText
                            if (streaming.isNullOrEmpty()) {
                                Row(Modifier.padding(vertical = AxiomTheme.space.sm)) {
                                    ThinkingIndicator()
                                }
                            } else {
                                CompanionReplyBlock(
                                    text = streaming,
                                    citedIds = emptyList(),
                                    citedEntries = emptyList(),
                                    onOpenEntry = onOpenEntry,
                                    onSaveToJournal = {},
                                    onPlay = null
                                )
                            }
                        }
                    }
                }
            }

            state.error?.let { error ->
                Column(Modifier.padding(horizontal = AxiomTheme.space.screenH)) {
                    AxiomErrorSurface(
                        error = error,
                        onRetry = if (error is AxiomError.NoAiKey) null else viewModel::retry,
                        onPrimary = if (error is AxiomError.NoAiKey) onConnectAi else null,
                        onDismiss = viewModel::dismissError
                    )
                    // Their words must survive a missing key or dead network.
                    state.lastFailedText?.let { failed ->
                        TextButton(onClick = {
                            viewModel.dismissError()
                            onSaveToJournal(failed)
                        }) {
                            Text(
                                "Save this to your journal instead",
                                style = AxiomTheme.type.uiLabel,
                                color = AxiomTheme.colors.accent
                            )
                        }
                    }
                }
                Spacer(Modifier.height(AxiomTheme.space.sm))
            }

            CompanionComposerBar(
                draft = state.draft,
                enabled = !state.sending,
                placeholder = when {
                    state.listening -> "Listening…"
                    state.transcribing -> "Transcribing…"
                    else -> "Talk to me…"
                },
                listening = state.listening,
                onChange = viewModel::setDraft,
                onSend = viewModel::send,
                onMic = { withMicPermission(viewModel::toggleListening) }
            )
        }
    }

    if (showDisclosure) {
        WhatGetsSentSheet(onDismiss = { showDisclosure = false })
    }
}

@Composable
private fun DateSeparator(date: LocalDate) {
    val formatter = rememberLocalized { locale ->
        DateTimeFormatter.ofPattern("EEEE d MMMM", locale)
    }
    val label = when (date) {
        LocalDate.now() -> "Today"
        LocalDate.now().minusDays(1) -> "Yesterday"
        else -> date.format(formatter)
    }
    Row(
        Modifier.fillMaxWidth().padding(vertical = AxiomTheme.space.xs),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(label, style = AxiomTheme.type.uiOverline, color = AxiomTheme.colors.inkFaint)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CompanionUserBubble(text: String, onSaveToJournal: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Text(
            text = text,
            style = AxiomTheme.type.uiBody,
            color = AxiomTheme.colors.onAccentSoft,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(AxiomTheme.shapes.md)
                .combinedClickable(onClick = {}, onLongClick = onSaveToJournal)
                .background(AxiomTheme.colors.accentSoft)
                .padding(horizontal = AxiomTheme.space.md, vertical = AxiomTheme.space.sm)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CompanionReplyBlock(
    text: String,
    citedIds: List<Long>,
    citedEntries: List<com.cosmiclaboratory.axiom.domain.model.Entry>,
    onOpenEntry: (Long) -> Unit,
    onSaveToJournal: () -> Unit,
    onPlay: (() -> Unit)?
) {
    val c = AxiomTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = onSaveToJournal)
    ) {
        // The rule, not a bubble: attribution without turning prose into chat.
        Box(
            Modifier
                .width(2.dp)
                .heightIn(min = 24.dp)
                .background(c.aiTint.copy(alpha = 0.6f))
        )
        Spacer(Modifier.width(AxiomTheme.space.md))
        Column(Modifier.weight(1f)) {
            Text(text, style = AxiomTheme.type.readingBody, color = c.ink)

            val cited = citedEntries.filter { it.id in citedIds }
            if (cited.isNotEmpty()) {
                Spacer(Modifier.height(AxiomTheme.space.md))
                Text(
                    "Read ${cited.size} ${if (cited.size == 1) "entry" else "entries"}",
                    style = AxiomTheme.type.uiOverline,
                    color = c.inkFaint
                )
                Spacer(Modifier.height(AxiomTheme.space.xs))
                Row(horizontalArrangement = Arrangement.spacedBy(AxiomTheme.space.xs)) {
                    cited.take(3).forEach { entry ->
                        EntryChip(entry, onClick = { onOpenEntry(entry.id) })
                    }
                }
            }
        }
        if (onPlay != null) {
            AxiomIconButton(
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                label = "Read aloud",
                onClick = onPlay,
                tint = c.inkFaint
            )
        }
    }
}

@Composable
private fun CompanionComposerBar(
    draft: String,
    enabled: Boolean,
    placeholder: String,
    listening: Boolean,
    onChange: (String) -> Unit,
    onSend: () -> Unit,
    onMic: () -> Unit
) {
    val c = AxiomTheme.colors
    val pulse by rememberInfiniteTransition(label = "mic-pulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "mic-pulse-scale"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.surface)
            .padding(horizontal = AxiomTheme.space.screenH, vertical = AxiomTheme.space.sm)
            .imePadding(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.weight(1f)) {
            if (draft.isEmpty()) {
                Text(placeholder, style = AxiomTheme.type.uiBody, color = c.inkFaint)
            }
            BasicTextField(
                value = draft,
                onValueChange = onChange,
                enabled = enabled,
                textStyle = AxiomTheme.type.uiBody.copy(color = c.ink),
                cursorBrush = SolidColor(c.accent),
                modifier = Modifier.fillMaxWidth().testTag("field:companion")
            )
        }
        AxiomIconButton(
            icon = if (listening) Icons.Filled.StopCircle else Icons.Filled.KeyboardVoice,
            label = if (listening) "Stop listening" else "Speak instead of typing",
            onClick = onMic,
            tint = if (listening) c.critical else c.inkMuted,
            modifier = Modifier.scale(if (listening) pulse else 1f)
        )
        AxiomIconButton(
            icon = Icons.AutoMirrored.Filled.Send,
            label = "Send",
            onClick = onSend,
            enabled = enabled && draft.isNotBlank(),
            tint = c.accent
        )
    }
}

/** Enumerates exactly what leaves the device. Vague reassurance is worth nothing. */
@Composable
private fun WhatGetsSentSheet(onDismiss: () -> Unit) {
    AxiomBottomSheet(title = "What gets sent?", onDismiss = onDismiss) {
        val c = AxiomTheme.colors
        Text(
            "Axiom stores everything on this device. Nothing is uploaded unless you " +
                "connect an AI key yourself.",
            style = AxiomTheme.type.uiBody,
            color = c.ink
        )
        Spacer(Modifier.height(AxiomTheme.space.base))
        listOf(
            "Your message, as typed.",
            "Your recent messages in this conversation, and a short running summary of older ones.",
            "Up to 20 remembered details — see and edit them all in \"What I remember\".",
            "Up to 4 short journal excerpts that matched your message, with their dates and moods.",
            "Today's mood, your writing streak, and the time of day.",
            "Your chosen companion voice."
        ).forEach { line ->
            Row(Modifier.padding(vertical = 4.dp)) {
                Text("•", style = AxiomTheme.type.uiBody, color = c.inkFaint)
                Spacer(Modifier.width(AxiomTheme.space.sm))
                Text(line, style = AxiomTheme.type.uiBodySmall, color = c.inkMuted)
            }
        }
        Spacer(Modifier.height(AxiomTheme.space.base))
        Text(
            "All of it goes to Groq with your own key, only when you send a message, " +
                "and is never stored anywhere but on this device.",
            style = AxiomTheme.type.uiBodySmall,
            color = c.inkMuted
        )
    }
}
