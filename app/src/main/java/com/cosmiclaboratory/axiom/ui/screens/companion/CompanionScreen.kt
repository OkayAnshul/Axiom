package com.cosmiclaboratory.axiom.ui.screens.companion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cosmiclaboratory.axiom.ui.viewmodels.companion.CompanionMessageUi
import com.cosmiclaboratory.axiom.ui.viewmodels.companion.CompanionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanionScreen(
    viewModel: CompanionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val keyConnected by viewModel.keyConnected.collectAsState()
    var input by remember { mutableStateOf("") }
    var showConnect by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Companion")
                    }
                },
                actions = {
                    if (state.messages.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearThread() }) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = "Clear chat")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (!keyConnected) {
                ConnectAiBanner(onClick = { showConnect = true })
            }

            if (state.messages.isEmpty()) {
                EmptyCompanionState(onSuggestionClick = { input = it })
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp)
                ) {
                    items(state.messages, key = { it.id }) { msg -> MessageBubble(msg) }
                    if (state.sending) item { TypingIndicator() }
                    state.error?.let { err ->
                        item {
                            Text(
                                text = err,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            InputBar(
                input = input,
                enabled = keyConnected && !state.sending,
                onInputChange = { input = it },
                onSend = {
                    if (input.isNotBlank()) {
                        viewModel.ask(input)
                        input = ""
                    }
                }
            )
        }
    }

    if (showConnect) {
        ConnectAiSheet(onDismiss = { showConnect = false })
    }
}

@Composable
private fun ConnectAiBanner(onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Connect your AI companion",
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Free Groq key — tap to connect.",
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun EmptyCompanionState(onSuggestionClick: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(32.dp))
        Text("Ask your journal", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Text(
            "I read what you've written. Ask about themes, patterns, or specific moments.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Text("Try asking…", style = MaterialTheme.typography.labelLarge)
        SUGGESTIONS.forEach { suggestion ->
            AssistChip(onClick = { onSuggestionClick(suggestion) }, label = { Text(suggestion) })
        }
    }
}

@Composable
private fun MessageBubble(msg: CompanionMessageUi) {
    val isUser = msg.role == "USER"
    val bg = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(bg)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Column {
                Text(msg.content, color = fg, style = MaterialTheme.typography.bodyMedium)
                if (!isUser && msg.citedEntryIds.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Read ${msg.citedEntryIds.size} entries",
                        color = fg.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun TypingIndicator() {
    Row(modifier = Modifier.padding(horizontal = 8.dp)) {
        AnimatedVisibility(visible = true) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    "Thinking…",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InputBar(
    input: String,
    enabled: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(tonalElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text(if (enabled) "Ask anything…" else "Connect Groq to chat") },
                enabled = enabled,
                maxLines = 4
            )
            Spacer(Modifier.width(8.dp))
            FilledIconButton(onClick = onSend, enabled = enabled && input.isNotBlank()) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
            }
        }
    }
}

private val SUGGESTIONS = listOf(
    "Summarize the last week of entries",
    "When did I last feel anxious?",
    "What patterns do you see?",
    "What surprised me this month?"
)
