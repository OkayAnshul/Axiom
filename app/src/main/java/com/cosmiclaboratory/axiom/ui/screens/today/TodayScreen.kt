package com.cosmiclaboratory.axiom.ui.screens.today

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cosmiclaboratory.axiom.domain.model.Note
import com.cosmiclaboratory.axiom.ui.components.StreakCard
import com.cosmiclaboratory.axiom.ui.viewmodels.today.TodayViewModel

@Composable
fun TodayScreen(
    onNewEntry: () -> Unit,
    onVoiceEntry: () -> Unit,
    onOpenEntry: (Long) -> Unit,
    viewModel: TodayViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.refresh() }

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = state.greeting,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            item { StreakCard(streak = state.streak) }
            item {
                state.todayPrompt?.let { prompt ->
                    PromptCard(prompt = prompt, onStart = onNewEntry)
                }
            }
            item { QuickActionsRow(onNewEntry = onNewEntry, onVoiceEntry = onVoiceEntry) }
            item {
                Text(
                    text = "Recent entries",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            if (state.recent.isEmpty()) {
                item { EmptyRecent() }
            } else {
                items(state.recent, key = { it.id }) { note ->
                    RecentEntryRow(note = note, onClick = { onOpenEntry(note.id) })
                }
            }
        }
    }
}

@Composable
private fun PromptCard(prompt: String, onStart: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Today's prompt",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(prompt, style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = onStart, modifier = Modifier.align(Alignment.End)) {
                Text("Start writing")
            }
        }
    }
}

@Composable
private fun QuickActionsRow(onNewEntry: () -> Unit, onVoiceEntry: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickAction(
            icon = Icons.AutoMirrored.Filled.NoteAdd,
            label = "Write",
            onClick = onNewEntry,
            modifier = Modifier.weight(1f)
        )
        QuickAction(
            icon = Icons.Filled.Mic,
            label = "Voice",
            onClick = onVoiceEntry,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            Text(label, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@Composable
private fun RecentEntryRow(note: Note, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MoodDot(mood = note.mood)
            Spacer(Modifier.padding(start = 12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = note.title.ifBlank { note.content.take(40) },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Text(
                    text = note.content.take(80),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun MoodDot(mood: Int?) {
    val color = when (mood) {
        1 -> MaterialTheme.colorScheme.error
        2 -> MaterialTheme.colorScheme.tertiary
        3 -> MaterialTheme.colorScheme.outline
        4 -> MaterialTheme.colorScheme.primary
        5 -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    Icon(
        Icons.Filled.WaterDrop,
        contentDescription = null,
        tint = color,
        modifier = Modifier.padding(4.dp)
    )
}

@Composable
private fun EmptyRecent() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Text(
            text = "No entries yet. Start with today's prompt above, or write something free-form.",
            modifier = Modifier.padding(20.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
