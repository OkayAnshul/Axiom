package com.cosmiclaboratory.axiom.ui.screens.ask

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Tune
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cosmiclaboratory.axiom.domain.model.PersonaKey
import com.cosmiclaboratory.axiom.ui.design.AxiomError
import com.cosmiclaboratory.axiom.ui.design.components.*
import com.cosmiclaboratory.axiom.ui.theme.AxiomTheme

/**
 * Grounded chat over the user's own entries.
 *
 * User turns are bubbles; ANSWERS ARE NOT. An answer renders as full-width prose
 * with a thin aiTint rule, so it reads like writing rather than like a chat
 * message — but is unmistakably attributed to the model. On a private journal
 * the user must never have to guess who wrote a sentence.
 */
@Composable
fun AskScreen(
    onOpenEntry: (Long) -> Unit,
    onConnectAi: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AskViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val c = AxiomTheme.colors
    var showPersonas by remember { mutableStateOf(false) }
    var showDisclosure by remember { mutableStateOf(false) }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex)
    }

    AxiomScaffold(
        title = "Ask",
        screenTag = "screen:ask",
        modifier = modifier,
        actions = {
            AxiomTopBarAction(Icons.Outlined.Tune, "Change voice", onClick = { showPersonas = true })
            if (state.messages.isNotEmpty()) {
                AxiomTopBarAction(Icons.Outlined.DeleteSweep, "Clear conversation", viewModel::clearThread)
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                when {
                    state.messages.isEmpty() -> AskEmptyState(
                        keyConnected = state.keyConnected,
                        onConnect = onConnectAi,
                        onDisclosure = { showDisclosure = true },
                        onSuggestion = { viewModel.setDraft(it); viewModel.send() }
                    )

                    else -> LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().testTag("list:ask"),
                        contentPadding = PaddingValues(AxiomTheme.space.screenH),
                        verticalArrangement = Arrangement.spacedBy(AxiomTheme.space.base)
                    ) {
                        items(state.messages, key = { it.id }) { message ->
                            if (message.isUser) {
                                AskUserBubble(message.text)
                            } else {
                                AskAnswerBlock(
                                    text = message.text,
                                    citedIds = message.citedEntryIds,
                                    citedEntries = state.citedEntries,
                                    onOpenEntry = onOpenEntry
                                )
                            }
                        }
                        if (state.sending) {
                            item("thinking") {
                                Row(Modifier.padding(vertical = AxiomTheme.space.sm)) {
                                    ThinkingIndicator()
                                }
                            }
                        }
                    }
                }
            }

            state.error?.let { error ->
                Box(Modifier.padding(horizontal = AxiomTheme.space.screenH)) {
                    AxiomErrorSurface(
                        error = error,
                        onRetry = viewModel::retry,
                        onPrimary = if (error is AxiomError.NoAiKey) onConnectAi else null,
                        onDismiss = viewModel::dismissError
                    )
                }
                Spacer(Modifier.height(AxiomTheme.space.sm))
            }

            AskComposerBar(
                draft = state.draft,
                enabled = state.keyConnected && !state.sending,
                placeholder = if (state.keyConnected) "Ask your journal…" else "Connect AI to ask",
                onChange = viewModel::setDraft,
                onSend = viewModel::send
            )
        }
    }

    if (showPersonas) {
        PersonaSheet(
            personas = state.personas,
            active = state.activePersona,
            onPick = { viewModel.setPersona(it); showPersonas = false },
            onDismiss = { showPersonas = false }
        )
    }

    if (showDisclosure) {
        WhatGetsSentSheet(onDismiss = { showDisclosure = false })
    }
}

@Composable
private fun AskUserBubble(text: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Text(
            text = text,
            style = AxiomTheme.type.uiBody,
            color = AxiomTheme.colors.onAccentSoft,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(AxiomTheme.shapes.md)
                .background(AxiomTheme.colors.accentSoft)
                .padding(horizontal = AxiomTheme.space.md, vertical = AxiomTheme.space.sm)
        )
    }
}

@Composable
private fun AskAnswerBlock(
    text: String,
    citedIds: List<Long>,
    citedEntries: List<com.cosmiclaboratory.axiom.domain.model.Entry>,
    onOpenEntry: (Long) -> Unit
) {
    val c = AxiomTheme.colors
    Row(Modifier.fillMaxWidth()) {
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
    }
}

@Composable
private fun AskEmptyState(
    keyConnected: Boolean,
    onConnect: () -> Unit,
    onDisclosure: () -> Unit,
    onSuggestion: (String) -> Unit
) {
    val c = AxiomTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .padding(AxiomTheme.space.screenH),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Ask your journal", style = AxiomTheme.type.uiTitleLarge, color = c.ink)
        Spacer(Modifier.height(AxiomTheme.space.sm))
        Text(
            "Questions are answered from what you've written, with the entries it used.",
            style = AxiomTheme.type.uiBody,
            color = c.inkMuted
        )

        if (keyConnected) {
            Spacer(Modifier.height(AxiomTheme.space.xl))
            listOf(
                "What's been on my mind lately?",
                "When did I last feel genuinely rested?",
                "What keeps coming up that I haven't dealt with?"
            ).forEach { suggestion ->
                Text(
                    text = suggestion,
                    style = AxiomTheme.type.uiBody,
                    color = c.accent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = AxiomTheme.space.xs)
                        .clip(AxiomTheme.shapes.sm)
                        .background(c.surfaceSunken)
                        .clickable { onSuggestion(suggestion) }
                        .padding(AxiomTheme.space.md)
                )
            }
        } else {
            Spacer(Modifier.height(AxiomTheme.space.xl))
            AxiomErrorSurface(AxiomError.NoAiKey, onPrimary = onConnect)
        }

        Spacer(Modifier.height(AxiomTheme.space.xl))
        // Against three cloud-subscription competitors, this panel IS the
        // differentiator — so it is a first-class affordance, not fine print.
        TextButton(onClick = onDisclosure) {
            Text("What gets sent?", style = AxiomTheme.type.uiLabel, color = c.inkMuted)
        }
    }
}

@Composable
private fun AskComposerBar(
    draft: String,
    enabled: Boolean,
    placeholder: String,
    onChange: (String) -> Unit,
    onSend: () -> Unit
) {
    val c = AxiomTheme.colors
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
                modifier = Modifier.fillMaxWidth().testTag("field:ask")
            )
        }
        AxiomIconButton(
            icon = Icons.AutoMirrored.Filled.Send,
            label = "Send question",
            onClick = onSend,
            enabled = enabled && draft.isNotBlank(),
            tint = c.accent
        )
    }
}

@Composable
private fun PersonaSheet(
    personas: List<com.cosmiclaboratory.axiom.domain.model.Persona>,
    active: PersonaKey,
    onPick: (PersonaKey) -> Unit,
    onDismiss: () -> Unit
) {
    AxiomBottomSheet(title = "Companion voice", onDismiss = onDismiss) {
        personas.forEach { persona ->
            AxiomCard(
                tone = if (persona.key == active) CardTone.Accent else CardTone.Neutral,
                onClick = { onPick(persona.key) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AxiomTheme.space.xs)
            ) {
                Text(persona.displayName, style = AxiomTheme.type.uiTitleSmall, color = AxiomTheme.colors.ink)
                Spacer(Modifier.height(2.dp))
                Text(
                    persona.systemPromptFragment,
                    style = AxiomTheme.type.uiBodySmall,
                    color = AxiomTheme.colors.inkMuted,
                    maxLines = 2
                )
            }
        }
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
            "Your question, as typed.",
            "Up to 8 entry excerpts the search matched, capped at 800 characters each.",
            "The date and mood of those entries.",
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
            "Entries you didn't ask about are never sent. Neither is your name, " +
                "your streak, or anything from other tabs.",
            style = AxiomTheme.type.uiBodySmall,
            color = c.inkMuted
        )
    }
}
