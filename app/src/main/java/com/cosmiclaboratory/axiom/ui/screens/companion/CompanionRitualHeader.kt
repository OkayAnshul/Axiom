package com.cosmiclaboratory.axiom.ui.screens.companion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import com.cosmiclaboratory.axiom.domain.model.Emotion
import com.cosmiclaboratory.axiom.domain.model.Entry
import com.cosmiclaboratory.axiom.ui.design.components.AxiomIconButton
import com.cosmiclaboratory.axiom.ui.design.components.MoodPicker
import com.cosmiclaboratory.axiom.ui.theme.AxiomTheme
import com.cosmiclaboratory.axiom.ui.design.components.axiomCollapse
import com.cosmiclaboratory.axiom.ui.design.components.axiomExpand

/**
 * One quiet line between the greeting and the conversation.
 *
 * The streak strip used to live here. It is gone from home deliberately: a
 * counter above the conversation turns showing up into a score to protect, and
 * the first bad week then costs you the app. The number still exists — it is in
 * "What I've noticed", behind the numbers disclosure, where someone who wants it
 * can go looking.
 *
 * What remains is the one thing worth asking every day, phrased as a question
 * rather than a metric, and a way back into an unfinished entry. All of it local:
 * this row must work identically with no API key and no network.
 */
@Composable
fun CompanionRitualHeader(
    todayMood: Int?,
    todayEmotion: Emotion?,
    todayMoodInferred: Boolean,
    writingDraft: Entry?,
    handsFree: Boolean,
    ttsAvailable: Boolean,
    onRecordMood: (Int) -> Unit,
    onContinueDraft: (Long) -> Unit,
    onToggleHandsFree: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = AxiomTheme.colors
    var showMoodPicker by remember { mutableStateOf(false) }

    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = AxiomTheme.space.screenH)
            .testTag("header:ritual")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                // An inferred feeling is offered as a reading to correct, not
                // stated as fact — hence the word, and the hedge.
                text = when {
                    todayMoodInferred && todayEmotion != null ->
                        "Today sounded ${todayEmotion.label.lowercase()}."
                    todayMood != null -> "You called today ${moodSentence(todayMood)}."
                    else -> "How is today going?"
                },
                style = AxiomTheme.type.uiBodySmall,
                color = if (todayMood != null) c.inkMuted else c.inkFaint,
                modifier = Modifier
                    .clip(AxiomTheme.shapes.sm)
                    .clickable { showMoodPicker = !showMoodPicker }
                    .padding(vertical = AxiomTheme.space.xs)
            )

            writingDraft?.let { draft ->
                Spacer(Modifier.width(AxiomTheme.space.md))
                Text(
                    text = "Finish what you started",
                    style = AxiomTheme.type.uiBodySmall,
                    color = c.accent,
                    modifier = Modifier
                        .clip(AxiomTheme.shapes.sm)
                        .clickable { onContinueDraft(draft.id) }
                        .padding(vertical = AxiomTheme.space.xs)
                )
            }

            Spacer(Modifier.weight(1f))

            if (ttsAvailable) {
                AxiomIconButton(
                    icon = if (handsFree) {
                        Icons.Filled.RecordVoiceOver
                    } else {
                        Icons.Outlined.RecordVoiceOver
                    },
                    label = if (handsFree) "Leave hands-free" else "Talk without typing",
                    onClick = onToggleHandsFree,
                    tint = if (handsFree) c.accent else c.inkFaint
                )
            }
        }

        AnimatedVisibility(visible = showMoodPicker, enter = axiomExpand(), exit = axiomCollapse()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = AxiomTheme.space.sm),
                horizontalArrangement = Arrangement.Center
            ) {
                MoodPicker(
                    value = todayMood,
                    onChange = { mood ->
                        onRecordMood(mood)
                        showMoodPicker = false
                    },
                    compact = true
                )
            }
        }
    }
}

/**
 * The mood scale, said rather than labelled. "You called today rough" is a
 * sentence a person could say; "Rough" beside a dot is a data point.
 */
private fun moodSentence(mood: Int?): String = when (mood) {
    1 -> "rough"
    2 -> "low"
    3 -> "okay"
    4 -> "good"
    5 -> "great"
    else -> "hard to name"
}
