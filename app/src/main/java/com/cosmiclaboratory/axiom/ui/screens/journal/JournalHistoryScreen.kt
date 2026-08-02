package com.cosmiclaboratory.axiom.ui.screens.journal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cosmiclaboratory.axiom.ui.viewmodels.JournalHistoryViewModel
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalHistoryScreen(
    onBack: () -> Unit,
    onEntryClick: (entryId: Long) -> Unit,
    viewModel: JournalHistoryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d, h:mm a") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search entries…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true
            )

            if (state.entries.isEmpty()) {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        if (state.query.isBlank()) "No entries yet. Tap a question to start."
                        else "No matches.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.entries, key = { it.id }) { entry ->
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onEntryClick(entry.id) }
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    entry.createdAt.format(dateFormatter),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    entry.promptSnapshot ?: "",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontStyle = FontStyle.Italic
                                )
                                Text(
                                    entry.content.take(160),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 3
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

