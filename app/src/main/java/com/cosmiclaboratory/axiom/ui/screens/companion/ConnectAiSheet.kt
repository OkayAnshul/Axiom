package com.cosmiclaboratory.axiom.ui.screens.companion

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cosmiclaboratory.axiom.ui.viewmodels.companion.ConnectAiViewModel
import com.cosmiclaboratory.axiom.ui.viewmodels.companion.TestConnectionState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectAiSheet(
    onDismiss: () -> Unit,
    viewModel: ConnectAiViewModel = hiltViewModel()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var key by remember { mutableStateOf("") }
    val testState by viewModel.testState.collectAsState()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Connect your AI companion",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Axiom runs on your own Groq key. It's free — usage stays on your account, " +
                    "and your key is encrypted on this device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://console.groq.com/keys")))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.OpenInNew, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Open Groq console")
            }
            OutlinedTextField(
                value = key,
                onValueChange = { key = it.trim() },
                label = { Text("Paste your key (gsk_…)") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )
            when (val state = testState) {
                TestConnectionState.Idle -> {}
                TestConnectionState.Testing -> Text("Testing key…", style = MaterialTheme.typography.bodySmall)
                TestConnectionState.Success -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(Modifier.width(6.dp))
                    Text("Connected!", color = MaterialTheme.colorScheme.tertiary)
                }
                is TestConnectionState.Failed -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(6.dp))
                    Text(state.reason, color = MaterialTheme.colorScheme.error)
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = { viewModel.testKey(key) },
                    enabled = key.isNotBlank() && testState !is TestConnectionState.Testing,
                    modifier = Modifier.weight(1f)
                ) { Text("Test") }
                Button(
                    onClick = {
                        viewModel.saveKey(key) {
                            scope.launch {
                                sheetState.hide()
                                onDismiss()
                            }
                        }
                    },
                    enabled = key.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) { Text("Save") }
            }
            Text(
                "Your key is encrypted via Android Keystore. It is sent only to Groq.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}
