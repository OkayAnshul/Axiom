package com.cosmiclaboratory.axiom.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cosmiclaboratory.axiom.domain.model.Persona

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeSheet(
    personas: List<Persona>,
    onComplete: (displayName: String, personaKey: com.cosmiclaboratory.axiom.domain.model.PersonaKey) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(personas.firstOrNull()?.key) }

    ModalBottomSheet(
        onDismissRequest = { /* welcome is non-dismissable; user must commit */ },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Welcome to Axiom", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "Axiom is a private journal with an AI companion. Two things to set, then we're in.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("What should the app call you?") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text("Pick a companion voice (you can change this anytime).", color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(personas, key = { it.key.storageValue }) { persona ->
                    PersonaCard(
                        persona = persona,
                        selected = selected == persona.key,
                        onClick = { selected = persona.key }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val key = selected ?: return@Button
                    onComplete(name.trim(), key)
                },
                enabled = name.isNotBlank() && selected != null,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Get started") }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PersonaCard(persona: Persona, selected: Boolean, onClick: () -> Unit) {
    val container = if (selected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
    Card(
        onClick = onClick,
        modifier = Modifier,
        colors = CardDefaults.cardColors(containerColor = container)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(persona.displayName, fontWeight = FontWeight.SemiBold)
            Text(
                persona.systemPromptFragment.take(80),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}
