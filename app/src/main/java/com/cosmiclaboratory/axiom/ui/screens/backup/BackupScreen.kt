package com.cosmiclaboratory.axiom.ui.screens.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cosmiclaboratory.axiom.ui.design.components.AxiomCard
import com.cosmiclaboratory.axiom.ui.design.components.AxiomIconButton
import com.cosmiclaboratory.axiom.ui.design.components.AxiomScaffold
import com.cosmiclaboratory.axiom.ui.design.components.CardTone
import com.cosmiclaboratory.axiom.ui.design.components.SectionHeader
import com.cosmiclaboratory.axiom.ui.theme.AxiomTheme

/**
 * Backup and restore.
 *
 * Uses the system file picker rather than writing to a fixed folder, so the app
 * needs no storage permission and the user chooses where their journal lives —
 * their own device, a USB stick, whichever cloud they already trust. Axiom
 * never uploads a backup anywhere itself.
 *
 * Restore merges rather than replaces, which is stated on screen: the fear when
 * tapping "restore" is that it will overwrite what you have now, and that fear
 * stops people using backups at all.
 */
@Composable
fun BackupScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BackupViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = AxiomTheme.colors
    var passphraseHidden by remember { mutableStateOf(true) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let(viewModel::export) }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(viewModel::restore) }

    AxiomScaffold(
        title = "Take everything with you",
        screenTag = "screen:backup",
        modifier = modifier,
        navigationIcon = { AxiomIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AxiomTheme.space.screenH),
            verticalArrangement = Arrangement.spacedBy(AxiomTheme.space.base)
        ) {
            AxiomCard(tone = CardTone.Ai) {
                Text("Your journal, in one file", style = AxiomTheme.type.uiTitleSmall, color = c.ink)
                Spacer(Modifier.height(AxiomTheme.space.xs))
                Text(
                    "Everything you've written, every remembered detail and your whole " +
                        "conversation history, saved wherever you choose. Axiom never uploads " +
                        "it anywhere — moving it somewhere safe is up to you.",
                    style = AxiomTheme.type.uiBodySmall,
                    color = c.inkMuted
                )
            }

            SectionHeader("Passphrase")
            AxiomCard {
                Text(
                    "Leave this empty and the backup is plain readable JSON — portable, and " +
                        "openable by anything, forever. Set one and the file is encrypted, " +
                        "which also means losing the passphrase loses the backup.",
                    style = AxiomTheme.type.uiBodySmall,
                    color = c.inkMuted
                )
                Spacer(Modifier.height(AxiomTheme.space.base))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) {
                        if (state.passphrase.isEmpty()) {
                            Text(
                                "Optional",
                                style = AxiomTheme.type.uiBody,
                                color = c.inkFaint
                            )
                        }
                        BasicTextField(
                            value = state.passphrase,
                            onValueChange = viewModel::setPassphrase,
                            singleLine = true,
                            textStyle = AxiomTheme.type.uiBody.copy(color = c.ink),
                            cursorBrush = SolidColor(c.accent),
                            visualTransformation = if (passphraseHidden) {
                                PasswordVisualTransformation()
                            } else {
                                VisualTransformation.None
                            },
                            modifier = Modifier.fillMaxWidth().testTag("field:backup-passphrase")
                        )
                    }
                    TextButton(onClick = { passphraseHidden = !passphraseHidden }) {
                        Text(
                            if (passphraseHidden) "Show" else "Hide",
                            style = AxiomTheme.type.uiLabel,
                            color = c.inkMuted
                        )
                    }
                }
            }

            SectionHeader("Take it with you")
            Button(
                onClick = { exportLauncher.launch(viewModel.suggestedFileName()) },
                enabled = !state.busy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = c.accent,
                    contentColor = c.onAccent
                ),
                modifier = Modifier.fillMaxWidth().testTag("button:export")
            ) {
                Text(if (state.busy) "Gathering it up…" else "Save it all to a file", style = AxiomTheme.type.uiLabel)
            }

            SectionHeader("Bring it back")
            AxiomCard {
                Text(
                    "Restoring adds what's missing and leaves everything already here " +
                        "untouched, so it is safe to run twice and safe to run on a journal " +
                        "you've kept writing in.",
                    style = AxiomTheme.type.uiBodySmall,
                    color = c.inkMuted
                )
                Spacer(Modifier.height(AxiomTheme.space.base))
                TextButton(
                    onClick = { restoreLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                    enabled = !state.busy,
                    modifier = Modifier.testTag("button:restore")
                ) {
                    Text("Choose a backup file", style = AxiomTheme.type.uiLabel, color = c.accent)
                }
            }

            state.message?.let { message ->
                AxiomCard(tone = if (state.isError) CardTone.Caution else CardTone.Neutral) {
                    Text(
                        message,
                        style = AxiomTheme.type.uiBody,
                        color = if (state.isError) c.ink else c.ink
                    )
                    Spacer(Modifier.height(AxiomTheme.space.sm))
                    TextButton(onClick = viewModel::dismissMessage) {
                        Text("Dismiss", style = AxiomTheme.type.uiLabel, color = c.inkMuted)
                    }
                }
            }

            Spacer(Modifier.height(AxiomTheme.space.huge))
        }
    }
}
