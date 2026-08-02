package com.cosmiclaboratory.axiom.ui.screens.journal

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cosmiclaboratory.axiom.ui.viewmodels.InsightsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    onBack: () -> Unit,
    viewModel: InsightsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reflection") },
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
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            state.entry?.let { entry ->
                Surface(
                    tonalElevation = 1.dp,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            entry.questionTextSnapshot,
                            style = MaterialTheme.typography.titleSmall,
                            fontStyle = FontStyle.Italic
                        )
                        Text(entry.plainText, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            val insight = state.insight
            if (insight == null) {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Text(
                            "Reflecting on your entry…",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "If you haven't added a Groq API key, summaries won't generate. Add one in Settings.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                ElevatedCard {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Summary", style = MaterialTheme.typography.labelLarge)
                        Text(insight.summary, style = MaterialTheme.typography.bodyLarge)

                        if (insight.followUpQuestionText.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text("A question to sit with", style = MaterialTheme.typography.labelLarge)
                            Text(insight.followUpQuestionText, style = MaterialTheme.typography.bodyMedium)
                        }

                        val themes = insight.themesCsv.split(',').map { it.trim() }.filter { it.isNotBlank() }
                        if (themes.isNotEmpty() || insight.mood.isNotBlank()) {
                            Row(
                                modifier = Modifier
                                    .horizontalScroll(rememberScrollState())
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (insight.mood.isNotBlank()) {
                                    AssistChip(onClick = {}, label = { Text(insight.mood) })
                                }
                                themes.forEach { theme ->
                                    AssistChip(onClick = {}, label = { Text(theme) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
