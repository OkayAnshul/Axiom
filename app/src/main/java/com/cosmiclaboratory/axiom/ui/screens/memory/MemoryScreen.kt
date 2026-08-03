package com.cosmiclaboratory.axiom.ui.screens.memory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cosmiclaboratory.axiom.domain.model.MemoryItem
import com.cosmiclaboratory.axiom.domain.model.MemoryKind
import com.cosmiclaboratory.axiom.domain.model.MemorySource
import com.cosmiclaboratory.axiom.domain.model.humanizedMemory
import com.cosmiclaboratory.axiom.ui.components.LocalSnackbarHostState
import com.cosmiclaboratory.axiom.ui.design.components.AxiomBottomSheet
import com.cosmiclaboratory.axiom.ui.design.components.AxiomCard
import com.cosmiclaboratory.axiom.ui.design.components.AxiomEmptyState
import com.cosmiclaboratory.axiom.ui.design.components.AxiomIconButton
import com.cosmiclaboratory.axiom.ui.design.components.AxiomScaffold
import com.cosmiclaboratory.axiom.ui.design.components.AxiomTopBarAction
import com.cosmiclaboratory.axiom.ui.design.components.CardTone
import com.cosmiclaboratory.axiom.ui.design.components.SectionHeader
import com.cosmiclaboratory.axiom.ui.theme.AxiomTheme
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * "What I remember" — the memory transparency screen. The companion's privacy
 * promise depends on this surface: every remembered item is visible, shows
 * where it came from and how often it has come up, and can be rewritten or
 * deleted by the user. The user owns their memories; the model only borrows
 * them.
 */
@Composable
fun MemoryScreen(
    onBack: () -> Unit,
    onOpenEntry: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MemoryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()

    AxiomScaffold(
        title = "What I remember",
        screenTag = "screen:memories",
        modifier = modifier,
        navigationIcon = { AxiomIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack) },
        actions = {
            AxiomTopBarAction(
                Icons.Outlined.Add,
                "Tell it how to talk to you",
                onClick = viewModel::startAddingPreference
            )
        }
    ) { padding ->
        if (state.loaded && state.groups.isEmpty()) {
            AxiomEmptyState(
                title = "Nothing remembered yet",
                body = "As you talk and write, your companion quietly keeps what matters — " +
                    "people, goals, recurring themes. Everything it keeps shows up here, " +
                    "and it never leaves this device except inside your own conversations.",
                icon = Icons.Outlined.Psychology,
                modifier = Modifier.fillMaxSize().padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).testTag("list:memories"),
                contentPadding = PaddingValues(AxiomTheme.space.screenH),
                verticalArrangement = Arrangement.spacedBy(AxiomTheme.space.sm)
            ) {
                state.groups.forEach { (kind, items) ->
                    item(key = "header-${kind.name}") {
                        SectionHeader(kindLabel(kind), Modifier.padding(top = AxiomTheme.space.sm))
                    }
                    items(items, key = { it.id }) { memory ->
                        MemoryRow(
                            memory = memory,
                            onEdit = { viewModel.startEditing(memory) },
                            onDelete = {
                                viewModel.delete(memory)
                                scope.launch {
                                    val result = snackbar.showSnackbar(
                                        message = "Memory forgotten",
                                        actionLabel = "Undo",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete()
                                }
                            },
                            onOpenSource = { entryId -> onOpenEntry(entryId) }
                        )
                    }
                }
                item(key = "footer") {
                    Text(
                        "Only the strongest of these are shared with the model, and only inside " +
                            "your own conversation. Deleting a memory removes it immediately.",
                        style = AxiomTheme.type.uiBodySmall,
                        color = AxiomTheme.colors.inkFaint,
                        modifier = Modifier.padding(vertical = AxiomTheme.space.lg)
                    )
                }
            }
        }
    }

    state.editing?.let { editing ->
        EditMemorySheet(
            memory = editing,
            onSave = viewModel::saveEdit,
            onDismiss = viewModel::cancelEditing
        )
    }

    if (state.addingPreference) {
        AddPreferenceSheet(
            onSave = viewModel::addPreference,
            onDismiss = viewModel::cancelAddingPreference
        )
    }
}

/**
 * The direct route to changing how the companion talks. Saying it in
 * conversation works too — extraction listens for it — but a person who wants
 * to be told less to "just listen" should not have to wait to be understood.
 */
@Composable
private fun AddPreferenceSheet(
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val c = AxiomTheme.colors
    var text by rememberSaveable { mutableStateOf("") }
    AxiomBottomSheet(title = "How should I talk to you?", onDismiss = onDismiss) {
        Text(
            "Anything you write here becomes a rule the companion follows, and it will " +
                "never be reworded on its own.",
            style = AxiomTheme.type.uiBodySmall,
            color = c.inkMuted
        )
        Spacer(Modifier.height(AxiomTheme.space.base))
        Box {
            if (text.isEmpty()) {
                Text(
                    "Keep replies short. Don't give advice unless I ask.",
                    style = AxiomTheme.type.uiBody,
                    color = c.inkFaint
                )
            }
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                textStyle = AxiomTheme.type.uiBody.copy(color = c.ink),
                cursorBrush = SolidColor(c.accent),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("field:preference-add")
            )
        }
        Spacer(Modifier.height(AxiomTheme.space.lg))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDismiss) {
                Text("Cancel", style = AxiomTheme.type.uiLabel, color = c.inkMuted)
            }
            TextButton(onClick = { onSave(text) }, enabled = text.isNotBlank()) {
                Text("Save", style = AxiomTheme.type.uiLabel, color = c.accent)
            }
        }
    }
}

@Composable
private fun MemoryRow(
    memory: MemoryItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenSource: (Long) -> Unit
) {
    val c = AxiomTheme.colors
    AxiomCard(tone = CardTone.Neutral, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                // Stored third-person for the model; addressed to the reader here.
                Text(memory.text.humanizedMemory(), style = AxiomTheme.type.uiBody, color = c.ink)
                Spacer(Modifier.height(AxiomTheme.space.xs))
                Text(
                    buildString {
                        append("Noticed ${memory.createdAt.format(DATE)}")
                        if (memory.timesSeen > 1) {
                            append(" · came up ${memory.timesSeen}×, last ${memory.lastSeenAt.format(DATE)}")
                        }
                        if (memory.userEdited) append(" · edited by you")
                    },
                    style = AxiomTheme.type.uiMeta,
                    color = c.inkFaint
                )
                val sourceEntryId = memory.sourceId.takeIf { memory.source == MemorySource.ENTRY }
                Text(
                    text = when (memory.source) {
                        MemorySource.CONVERSATION -> "From a conversation"
                        MemorySource.ENTRY -> "From a journal entry — read it"
                        MemorySource.MANUAL -> "You told me this directly"
                    },
                    style = AxiomTheme.type.uiMeta,
                    color = if (sourceEntryId != null) c.accent else c.inkFaint,
                    modifier = if (sourceEntryId != null) {
                        Modifier.clickable { onOpenSource(sourceEntryId) }
                    } else {
                        Modifier
                    }
                )
            }
            AxiomIconButton(Icons.Outlined.Edit, "Edit memory", onEdit, tint = c.inkMuted)
            AxiomIconButton(Icons.Outlined.Delete, "Forget this", onDelete, tint = c.inkMuted)
        }
    }
}

@Composable
private fun EditMemorySheet(
    memory: MemoryItem,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val c = AxiomTheme.colors
    var text by rememberSaveable(memory.id) { mutableStateOf(memory.text) }
    AxiomBottomSheet(title = "Edit memory", onDismiss = onDismiss) {
        Text(
            "Your wording wins: the companion will never rewrite a memory you've edited.",
            style = AxiomTheme.type.uiBodySmall,
            color = c.inkMuted
        )
        Spacer(Modifier.height(AxiomTheme.space.base))
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            textStyle = AxiomTheme.type.uiBody.copy(color = c.ink),
            cursorBrush = SolidColor(c.accent),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("field:memory-edit")
        )
        Spacer(Modifier.height(AxiomTheme.space.lg))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDismiss) {
                Text("Cancel", style = AxiomTheme.type.uiLabel, color = c.inkMuted)
            }
            TextButton(onClick = { onSave(text) }, enabled = text.isNotBlank()) {
                Text("Save", style = AxiomTheme.type.uiLabel, color = c.accent)
            }
        }
    }
}

private fun kindLabel(kind: MemoryKind): String = when (kind) {
    MemoryKind.PERSON -> "People"
    MemoryKind.GOAL -> "Goals"
    MemoryKind.THEME -> "Recurring themes"
    MemoryKind.FACT -> "Facts"
    MemoryKind.EVENT -> "Events"
    MemoryKind.PREFERENCE -> "Preferences"
}

private val DATE = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
