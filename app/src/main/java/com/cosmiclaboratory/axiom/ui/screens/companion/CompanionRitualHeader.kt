package com.cosmiclaboratory.axiom.ui.screens.companion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.cosmiclaboratory.axiom.domain.streak.StreakCalculator
import com.cosmiclaboratory.axiom.ui.design.components.MoodDot
import com.cosmiclaboratory.axiom.ui.design.components.MoodPicker
import com.cosmiclaboratory.axiom.ui.design.components.StreakStrip
import com.cosmiclaboratory.axiom.ui.design.components.moodLabel
import com.cosmiclaboratory.axiom.ui.theme.AxiomTheme

/**
 * The Today remnant: one thin row above the conversation, not a dashboard.
 * Streak dots, a tappable mood chip (expands to the picker), a continue-draft
 * chip when one exists, and the "What gets sent?" privacy affordance. All of
 * it local — this row must work identically with no API key and no network.
 */
@Composable
fun CompanionRitualHeader(
    streak: StreakCalculator.Result,
    todayMood: Int?,
    todayEmotion: Emotion?,
    todayMoodInferred: Boolean,
    writingDraft: Entry?,
    onRecordMood: (Int) -> Unit,
    onContinueDraft: (Long) -> Unit,
    onDisclosure: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = AxiomTheme.colors
    var showMoodPicker by remember { mutableStateOf(false) }

    Column(
        modifier
            .fillMaxWidth()
            .background(c.surface)
            .padding(horizontal = AxiomTheme.space.screenH, vertical = AxiomTheme.space.sm)
            .testTag("header:ritual")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (streak.current > 0) {
                StreakStrip(result = streak, compact = true)
                Spacer(Modifier.width(AxiomTheme.space.md))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(AxiomTheme.shapes.sm)
                    .clickable { showMoodPicker = !showMoodPicker }
                    .background(c.surfaceSunken)
                    .padding(horizontal = AxiomTheme.space.sm, vertical = AxiomTheme.space.xs)
            ) {
                MoodDot(mood = todayMood)
                Spacer(Modifier.width(AxiomTheme.space.xs))
                Text(
                    // An inferred feeling is offered as a reading to correct, not
                    // stated as fact — hence the word, and the hedge.
                    text = when {
                        todayMoodInferred && todayEmotion != null -> "Sounds like ${todayEmotion.label}"
                        todayMood != null -> moodLabel(todayMood)
                        else -> "How's today?"
                    },
                    style = AxiomTheme.type.uiLabelSmall,
                    color = if (todayMood != null) c.ink else c.inkMuted
                )
            }

            writingDraft?.let { draft ->
                Spacer(Modifier.width(AxiomTheme.space.sm))
                Text(
                    text = "Continue writing",
                    style = AxiomTheme.type.uiLabelSmall,
                    color = c.accent,
                    modifier = Modifier
                        .clip(AxiomTheme.shapes.sm)
                        .clickable { onContinueDraft(draft.id) }
                        .background(c.surfaceSunken)
                        .padding(horizontal = AxiomTheme.space.sm, vertical = AxiomTheme.space.xs)
                )
            }

            Spacer(Modifier.weight(1f))
            // Against three cloud-subscription competitors, this affordance IS
            // the differentiator — first-class, not fine print.
            Text(
                text = "What gets sent?",
                style = AxiomTheme.type.uiLabelSmall,
                color = c.inkFaint,
                modifier = Modifier
                    .clip(AxiomTheme.shapes.sm)
                    .clickable(onClick = onDisclosure)
                    .padding(horizontal = AxiomTheme.space.xs, vertical = AxiomTheme.space.xs)
            )
        }

        AnimatedVisibility(visible = showMoodPicker) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = AxiomTheme.space.sm),
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
