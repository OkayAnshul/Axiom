package com.cosmiclaboratory.axiom.ui.design.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.cosmiclaboratory.axiom.domain.model.AiVendor
import com.cosmiclaboratory.axiom.ui.theme.AxiomTheme

/**
 * Connecting a key, in one place.
 *
 * Settings and onboarding both need this, and a key field duplicated across two
 * screens is a key field that gets fixed in one of them. The card owns the
 * input, the masking toggle, the live test and the link out to the provider;
 * the caller owns the state and the surrounding copy, which is the part that
 * genuinely differs between "I came here to set this up" and "I have just been
 * offered this".
 *
 * [showSteps] is the onboarding difference: someone who navigated to Settings →
 * AI knows what an API key is, and numbered instructions there would be
 * condescending. Someone being offered one for the first time, thirty seconds
 * after installing a journal, mostly does not.
 */
@Composable
fun ApiKeyConnectCard(
    vendor: AiVendor,
    keyDraft: String,
    masked: Boolean,
    testing: Boolean,
    onKeyDraftChange: (String) -> Unit,
    onToggleMask: () -> Unit,
    onSaveAndTest: () -> Unit,
    modifier: Modifier = Modifier,
    showSteps: Boolean = false
) {
    val c = AxiomTheme.colors
    val uriHandler = LocalUriHandler.current

    AxiomCard(modifier = modifier) {
        if (showSteps) {
            Step(1, "Open ${vendor.keyUrl.removePrefix("https://")}")
            Step(2, "Sign in, then press Create Key")
            Step(3, "Paste it below")
            Spacer(Modifier.height(AxiomTheme.space.base))
        }

        Box {
            if (keyDraft.isEmpty()) {
                Text(vendor.keyHint, style = AxiomTheme.type.uiBody, color = c.inkFaint)
            }
            BasicTextField(
                value = keyDraft,
                onValueChange = onKeyDraftChange,
                textStyle = AxiomTheme.type.uiBody.copy(color = c.ink),
                cursorBrush = SolidColor(c.accent),
                singleLine = true,
                visualTransformation = if (masked) {
                    PasswordVisualTransformation()
                } else VisualTransformation.None,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("field:api_key")
            )
        }
        Spacer(Modifier.height(AxiomTheme.space.md))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onSaveAndTest,
                enabled = keyDraft.isNotBlank() && !testing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = c.accent, contentColor = c.onAccent
                ),
                shape = AxiomTheme.shapes.sm
            ) {
                Text(
                    if (testing) "Testing…" else "Save and test",
                    style = AxiomTheme.type.uiLabel
                )
            }
            Spacer(Modifier.width(AxiomTheme.space.sm))
            AxiomIconButton(
                icon = if (masked) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                label = if (masked) "Show key" else "Hide key",
                onClick = onToggleMask,
                tint = c.inkMuted
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { uriHandler.openUri(vendor.keyUrl) }) {
                Text("Get a free key", style = AxiomTheme.type.uiLabel, color = c.accent)
            }
        }
    }
}

@Composable
private fun Step(number: Int, text: String) {
    val c = AxiomTheme.colors
    Row(
        Modifier.padding(bottom = AxiomTheme.space.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(20.dp)
                .clip(AxiomTheme.shapes.full)
                .padding(1.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("$number", style = AxiomTheme.type.uiNumeric, color = c.accent)
        }
        Spacer(Modifier.width(AxiomTheme.space.sm))
        Text(text, style = AxiomTheme.type.uiBodySmall, color = c.inkMuted)
    }
}
