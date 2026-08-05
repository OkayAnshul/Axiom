package com.cosmiclaboratory.axiom.ui.screens.talks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cosmiclaboratory.axiom.data.database.entity.CompanionMessageEntity
import com.cosmiclaboratory.axiom.ui.design.components.AxiomCard
import com.cosmiclaboratory.axiom.ui.design.components.AxiomEmptyState
import com.cosmiclaboratory.axiom.ui.design.components.AxiomIconButton
import com.cosmiclaboratory.axiom.ui.design.components.AxiomScaffold
import com.cosmiclaboratory.axiom.ui.design.components.CardTone
import com.cosmiclaboratory.axiom.ui.design.components.swipeBetween
import com.cosmiclaboratory.axiom.ui.design.rememberLocalized
import com.cosmiclaboratory.axiom.ui.theme.AxiomTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.cosmiclaboratory.axiom.ui.design.components.axiomItemMotion
import com.cosmiclaboratory.axiom.ui.design.components.axiomCollapse
import com.cosmiclaboratory.axiom.ui.design.components.axiomExpand

/**
 * Every conversation, findable.
 *
 * The thread was one continuous scroll with no way to reach a particular day,
 * and the rolling summary — written on every digest — was rendered nowhere at
 * all. Both are here: the summary at the top, editable because it is a
 * description of the user, and the talks below grouped by day with what each one
 * actually produced.
 */
@Composable
fun TalksScreen(
    onBackToCompanion: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TalksViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val expanded = remember { mutableStateMapOf<LocalDate, Boolean>() }

    AxiomScaffold(
        title = "Our talks",
        screenTag = "screen:talks",
        modifier = modifier.swipeBetween(onSwipeLeft = onBackToCompanion),
        navigationIcon = {
            AxiomIconButton(
                Icons.AutoMirrored.Filled.ArrowBack,
                "Back to the conversation",
                onBackToCompanion
            )
        }
    ) { padding ->
        if (state.loaded && state.days.isEmpty()) {
            AxiomEmptyState(
                title = "We haven't talked yet",
                body = "Whenever we do, every conversation will be here — by the day it happened, " +
                    "so you can find the one you're thinking of.",
                modifier = Modifier.fillMaxSize().padding(padding)
            )
            return@AxiomScaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).testTag("list:talks"),
            contentPadding = PaddingValues(
                start = AxiomTheme.space.screenH,
                end = AxiomTheme.space.screenH,
                bottom = AxiomTheme.space.huge
            ),
            verticalArrangement = Arrangement.spacedBy(AxiomTheme.space.md)
        ) {
            item("summary") {
                SummaryCard(
                    summary = state.rollingSummary,
                    onSave = viewModel::saveSummary
                )
            }

            items(state.days, key = { it.date.toString() }) { day ->
                TalkDayCard(
                    modifier = axiomItemMotion(),
                    day = day,
                    expanded = expanded[day.date] == true,
                    landmarks = state.landmarkMessageIds,
                    onToggle = { expanded[day.date] = expanded[day.date] != true }
                )
            }
        }
    }
}

/**
 * What the companion believes your conversations have been about, in its own
 * words — and yours to correct. Editing follows the memory screen's precedent:
 * anything the app decided about you is something you can change.
 */
@Composable
private fun SummaryCard(summary: String, onSave: (String) -> Unit) {
    val c = AxiomTheme.colors
    var editing by remember { mutableStateOf(false) }
    var draft by remember(summary) { mutableStateOf(summary) }

    AxiomCard(tone = CardTone.Ai) {
        Text("Where we've been lately", style = AxiomTheme.type.uiOverline, color = c.aiTint)
        Spacer(Modifier.height(AxiomTheme.space.sm))

        if (editing) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                textStyle = AxiomTheme.type.readingBody,
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            Row {
                TextButton(onClick = { onSave(draft.trim()); editing = false }) {
                    Text("Save", style = AxiomTheme.type.uiLabel, color = c.accent)
                }
                TextButton(onClick = { draft = summary; editing = false }) {
                    Text("Cancel", style = AxiomTheme.type.uiLabel, color = c.inkMuted)
                }
            }
        } else {
            Text(
                text = summary.ifBlank {
                    "Nothing yet. After we've talked a few times I'll keep a short note here " +
                        "about where things have been going."
                },
                style = AxiomTheme.type.readingBody,
                color = if (summary.isBlank()) c.inkMuted else c.ink
            )
            if (summary.isNotBlank()) {
                TextButton(onClick = { editing = true }) {
                    Text("Change this", style = AxiomTheme.type.uiLabel, color = c.accent)
                }
            }
        }
    }
}

@Composable
private fun TalkDayCard(
    day: TalkDay,
    expanded: Boolean,
    landmarks: Set<Long>,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = AxiomTheme.colors
    val formatter = rememberLocalized { locale -> DateTimeFormatter.ofPattern("EEEE d MMMM", locale) }
    val label = when (day.date) {
        LocalDate.now() -> "Today"
        LocalDate.now().minusDays(1) -> "Yesterday"
        else -> day.date.format(formatter)
    }

    AxiomCard(onClick = onToggle, modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = AxiomTheme.type.uiTitle, color = c.ink, modifier = Modifier.weight(1f))
            Text(
                "${day.messageCount}",
                style = AxiomTheme.type.uiNumeric,
                color = c.inkFaint
            )
        }
        Spacer(Modifier.height(AxiomTheme.space.xs))
        Text(
            day.opening.take(OPENING_PREVIEW),
            style = AxiomTheme.type.uiBodySmall,
            color = c.inkMuted,
            maxLines = 2
        )

        val outcomes = buildList {
            if (day.entryWritten) add("written up")
            if (day.memoriesFormed > 0) add("${day.memoriesFormed} remembered")
            if (day.loopsOpened > 0) add("${day.loopsOpened} to ask about")
        }
        if (outcomes.isNotEmpty()) {
            Spacer(Modifier.height(AxiomTheme.space.xs))
            Text(
                outcomes.joinToString(" · "),
                style = AxiomTheme.type.uiMeta,
                color = c.aiTint
            )
        }

        AnimatedVisibility(visible = expanded, enter = axiomExpand(), exit = axiomCollapse()) {
            Column(Modifier.padding(top = AxiomTheme.space.md)) {
                day.messages.forEach { message ->
                    TranscriptLine(message, isLandmark = message.id in landmarks)
                }
            }
        }
    }
}

/**
 * One turn, read-only. User turns are indented right and companion turns keep
 * the aiTint rule, so the transcript reads the same way the live conversation
 * does rather than becoming an undifferentiated log.
 */
@Composable
private fun TranscriptLine(message: CompanionMessageEntity, isLandmark: Boolean) {
    val c = AxiomTheme.colors
    val isUser = message.role == CompanionMessageEntity.Role.USER.name

    Row(
        Modifier.fillMaxWidth().padding(vertical = AxiomTheme.space.xs),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                Modifier
                    .width(2.dp)
                    .heightIn(min = 18.dp)
                    .clip(AxiomTheme.shapes.full)
                    .background(c.aiTint.copy(alpha = 0.5f))
            )
            Spacer(Modifier.width(AxiomTheme.space.sm))
        }
        Column(horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
            Text(
                text = message.content,
                style = if (isUser) AxiomTheme.type.uiBody else AxiomTheme.type.readingBody,
                color = c.ink,
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    // The bubble is load-bearing, not decoration. Without it a
                    // long user turn wraps to the full width and stops looking
                    // right-aligned, and the only remaining cue for who wrote a
                    // sentence is sans-versus-serif — too subtle for a journal
                    // where that question must never need asking.
                    .then(
                        if (isUser) {
                            Modifier
                                .clip(AxiomTheme.shapes.lg)
                                .background(c.accentSoft)
                                .padding(
                                    horizontal = AxiomTheme.space.md,
                                    vertical = AxiomTheme.space.sm
                                )
                        } else {
                            Modifier
                        }
                    )
            )
            if (isLandmark) {
                Text(
                    "something stayed with me here",
                    style = AxiomTheme.type.uiMeta,
                    color = c.inkFaint
                )
            }
        }
    }
}

private const val OPENING_PREVIEW = 120
