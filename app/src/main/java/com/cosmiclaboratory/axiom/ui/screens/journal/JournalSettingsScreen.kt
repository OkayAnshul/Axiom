package com.cosmiclaboratory.axiom.ui.screens.journal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cosmiclaboratory.axiom.ui.viewmodels.JournalSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalSettingsScreen(
    onBack: () -> Unit,
    viewModel: JournalSettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Journal Settings") },
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("Persona", style = MaterialTheme.typography.titleMedium)
            state.personas.forEach { persona ->
                val selected = persona.key == state.activePersona
                Surface(
                    tonalElevation = if (selected) 4.dp else 0.dp,
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selected,
                            onClick = { viewModel.setPersona(persona.key) },
                            role = Role.RadioButton
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(selected = selected, onClick = null)
                        Column(Modifier.weight(1f)) {
                            Text(persona.displayName, style = MaterialTheme.typography.titleSmall)
                            Text(
                                persona.systemPromptFragment,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            Text("Groq API Key", style = MaterialTheme.typography.titleMedium)
            Text(
                "AI summaries and prompt batches use your free Groq key. Get one at console.groq.com.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = state.groqApiKey,
                onValueChange = viewModel::updateApiKey,
                singleLine = true,
                visualTransformation = if (state.keyMasked) PasswordVisualTransformation() else VisualTransformation.None,
                trailingIcon = {
                    IconButton(onClick = viewModel::toggleMask) {
                        Icon(
                            imageVector = if (state.keyMasked) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = "Toggle visibility"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("gsk_…") }
            )
            Button(onClick = viewModel::saveApiKey, modifier = Modifier.align(Alignment.End)) {
                Text("Save key")
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Weekly recap", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "A short reflection summary every Sunday evening (Phase 2).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = state.weeklyRecapEnabled,
                    onCheckedChange = viewModel::setWeeklyRecap
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
