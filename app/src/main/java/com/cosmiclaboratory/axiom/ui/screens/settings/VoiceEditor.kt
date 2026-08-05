package com.cosmiclaboratory.axiom.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import com.cosmiclaboratory.axiom.domain.voice.Advice
import com.cosmiclaboratory.axiom.domain.voice.Humour
import com.cosmiclaboratory.axiom.domain.voice.Profanity
import com.cosmiclaboratory.axiom.domain.voice.Pushback
import com.cosmiclaboratory.axiom.domain.voice.Register
import com.cosmiclaboratory.axiom.domain.voice.VoicePreset
import com.cosmiclaboratory.axiom.domain.voice.VoiceProfile
import com.cosmiclaboratory.axiom.ui.design.components.AxiomCard
import com.cosmiclaboratory.axiom.ui.design.components.AxiomSegmented
import com.cosmiclaboratory.axiom.ui.design.components.CardTone
import com.cosmiclaboratory.axiom.ui.design.components.SectionHeader
import com.cosmiclaboratory.axiom.ui.theme.AxiomTheme
import com.cosmiclaboratory.axiom.domain.voice.Emoji

/**
 * Where the companion's voice is actually chosen.
 *
 * This replaces a radio list over six frozen sentences that the prompt then told
 * the model to ignore in conflicts. A preset is now a starting point rather than
 * the whole choice: the dials underneath are what make one companion different
 * from another, and the free-text box outranks all of them.
 *
 * Every control here maps to exactly one line of the system prompt, so what the
 * user sets is legible in what they get back.
 */
@Composable
fun VoiceEditor(
    voice: VoiceProfile,
    onPreset: (VoicePreset) -> Unit,
    onVoice: (VoiceProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = AxiomTheme.colors

    Column(modifier.testTag("section:voice")) {
        SectionHeader("How I talk to you")
        Text(
            "Pick somewhere to start, then change anything you like.",
            style = AxiomTheme.type.uiBodySmall,
            color = c.inkMuted
        )
        Spacer(Modifier.height(AxiomTheme.space.md))

        VoicePreset.entries.forEach { preset ->
            AxiomCard(
                tone = if (preset == voice.preset) CardTone.Accent else CardTone.Neutral,
                onClick = { onPreset(preset) },
                modifier = Modifier.padding(bottom = AxiomTheme.space.sm)
            ) {
                Text(preset.displayName, style = AxiomTheme.type.uiTitleSmall, color = c.ink)
                Text(preset.blurb, style = AxiomTheme.type.uiBodySmall, color = c.inkMuted)
            }
        }

        Spacer(Modifier.height(AxiomTheme.space.base))
        SectionHeader("Fine-tune it")

        Dial(
            label = "Manner",
            options = Register.entries.map { it.name },
            selected = Register.entries.indexOf(voice.register),
            onSelect = { onVoice(voice.copy(register = Register.entries[it])) }
        )
        Dial(
            label = "Humour",
            options = Humour.entries.map { it.name },
            selected = Humour.entries.indexOf(voice.humour),
            onSelect = { onVoice(voice.copy(humour = Humour.entries[it])) }
        )
        Dial(
            label = "Swearing",
            options = Profanity.entries.map { it.name },
            selected = Profanity.entries.indexOf(voice.profanity),
            onSelect = { onVoice(voice.copy(profanity = Profanity.entries[it])) }
        )
        Dial(
            label = "Emoji",
            options = Emoji.entries.map { it.name },
            selected = Emoji.entries.indexOf(voice.emoji),
            onSelect = { onVoice(voice.copy(emoji = Emoji.entries[it])) }
        )
        Dial(
            label = "Pushback",
            options = Pushback.entries.map { it.name },
            selected = Pushback.entries.indexOf(voice.pushback),
            onSelect = { onVoice(voice.copy(pushback = Pushback.entries[it])) }
        )
        Dial(
            label = "Advice",
            options = listOf("If asked", "Freely"),
            selected = Advice.entries.indexOf(voice.advice),
            onSelect = { onVoice(voice.copy(advice = Advice.entries[it])) }
        )

        Spacer(Modifier.height(AxiomTheme.space.base))
        CustomInstructionField(
            current = voice.customInstruction,
            onSave = { onVoice(voice.copy(customInstruction = it)) }
        )

        Spacer(Modifier.height(AxiomTheme.space.base))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Soften when I'm really struggling",
                    style = AxiomTheme.type.uiTitleSmall,
                    color = c.ink
                )
                Text(
                    // Said plainly rather than reassuringly: someone turning this
                    // off should understand exactly what they are turning off.
                    "If you say something that sounds like real distress, I drop the " +
                        "act for that reply. Turn this off and I stay in character. " +
                        "Either way you'll still see the helplines.",
                    style = AxiomTheme.type.uiBodySmall,
                    color = c.inkMuted
                )
            }
            Switch(
                checked = voice.softenWhenStruggling,
                onCheckedChange = { onVoice(voice.copy(softenWhenStruggling = it)) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AxiomTheme.colors.onAccent,
                    checkedTrackColor = AxiomTheme.colors.accent
                )
            )
        }
    }
}

@Composable
private fun Dial(
    label: String,
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    Column(Modifier.padding(bottom = AxiomTheme.space.md)) {
        Text(label, style = AxiomTheme.type.uiLabel, color = AxiomTheme.colors.inkMuted)
        Spacer(Modifier.height(AxiomTheme.space.xs))
        AxiomSegmented(
            options = options,
            selectedIndex = selected.coerceAtLeast(0),
            onSelect = onSelect
        )
    }
}

/**
 * Held locally and committed on blur rather than on every keystroke — the voice
 * is read on the next turn, and rewriting it mid-sentence would mean the
 * companion briefly obeyed half an instruction.
 */
@Composable
private fun CustomInstructionField(current: String, onSave: (String) -> Unit) {
    var draft by remember(current) { mutableStateOf(current) }
    Column {
        Text("In your own words", style = AxiomTheme.type.uiLabel, color = AxiomTheme.colors.inkMuted)
        Spacer(Modifier.height(AxiomTheme.space.xs))
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it.take(VoiceProfile.MAX_CUSTOM_LENGTH) },
            placeholder = {
                Text(
                    "Talk to me like my oldest friend who thinks I'm being an idiot.",
                    style = AxiomTheme.type.uiBody
                )
            },
            textStyle = AxiomTheme.type.uiBody,
            minLines = 2,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focus ->
                    if (!focus.isFocused && draft != current) onSave(draft)
                }
        )
        Text(
            "This beats everything above it.",
            style = AxiomTheme.type.uiBodySmall,
            color = AxiomTheme.colors.inkFaint
        )
    }
}
