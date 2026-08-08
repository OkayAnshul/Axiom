package com.cosmiclaboratory.axiom.ui.screens.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cosmiclaboratory.axiom.domain.model.AIInsight
import com.cosmiclaboratory.axiom.domain.model.Entry
import com.cosmiclaboratory.axiom.ui.design.ReadingColumn
import com.cosmiclaboratory.axiom.ui.design.SharedKeys
import com.cosmiclaboratory.axiom.ui.design.axiomSharedBounds
import com.cosmiclaboratory.axiom.ui.design.components.*
import com.cosmiclaboratory.axiom.ui.theme.AxiomTheme
import com.cosmiclaboratory.axiom.utils.ExportManager
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ReaderScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReaderViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val c = AxiomTheme.colors
    val entry = state.entry

    AxiomScaffold(
        title = "",
        screenTag = "screen:reader",
        modifier = modifier,
        navigationIcon = { AxiomIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack) },
        actions = {
            if (entry != null) {
                AxiomTopBarAction(
                    icon = if (entry.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                    label = if (entry.isFavorite) "Remove favourite" else "Favourite",
                    onClick = viewModel::toggleFavorite
                )
                AxiomTopBarAction(Icons.Outlined.Share, "Share", onClick = {
                    ExportManager(context)
                        .exportNote(entry, ExportManager.ExportFormat.MARKDOWN)
                        ?.let { context.startActivity(it) }
                })
                AxiomTopBarAction(Icons.Filled.Edit, "Edit", onClick = { onEdit(entry.id) })
            }
        }
    ) { padding ->
        when {
            state.isLoading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { Text("…", style = AxiomTheme.type.uiBody, color = c.inkFaint) }

            entry == null -> AxiomEmptyState(
                title = "This entry no longer exists",
                body = "It may have been deleted from another screen.",
                primaryLabel = "Back to journal",
                onPrimary = onBack,
                modifier = Modifier.padding(padding)
            )

            // The other end of the card that was tapped in the timeline. Applied
            // outside the scroll modifier so the bounds animate the container
            // rather than the scrolling content inside it.
            else -> ReadingColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .axiomSharedBounds(SharedKeys.entry(entry.id))
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = AxiomTheme.space.screenH)
                    .testTag("list:reader")
            ) {
                Column {
                    EntryHeader(entry)

                    // A prompted entry should read question-then-answer, so the
                    // prompt is rendered as a quote rather than as metadata.
                    entry.promptSnapshot?.takeIf { it.isNotBlank() }?.let { prompt ->
                        Spacer(Modifier.height(AxiomTheme.space.base))
                        Row {
                            Box(
                                Modifier
                                    .width(3.dp)
                                    .heightIn(min = 24.dp)
                                    .background(c.accent.copy(alpha = 0.5f))
                            )
                            Spacer(Modifier.width(AxiomTheme.space.md))
                            Text(prompt, style = AxiomTheme.type.readingSubtitle, color = c.inkMuted)
                        }
                    }

                    Spacer(Modifier.height(AxiomTheme.space.lg))
                    AxiomMarkdown(
                        source = entry.markdown.ifBlank { entry.content },
                        onTaskToggle = viewModel::toggleTask
                    )

                    state.insight?.let { insight ->
                        Spacer(Modifier.height(AxiomTheme.space.xxl))
                        InsightCard(insight)
                    }

                    Spacer(Modifier.height(AxiomTheme.space.huge))
                }
            }
        }
    }
}

@Composable
private fun EntryHeader(entry: Entry) {
    val c = AxiomTheme.colors
    val stamp = remember(entry.createdAt) {
        entry.createdAt.format(
            DateTimeFormatter.ofPattern("EEEE d MMMM · HH:mm", Locale.getDefault())
        )
    }
    Column {
        Spacer(Modifier.height(AxiomTheme.space.sm))
        Text(stamp, style = AxiomTheme.type.uiMeta, color = c.inkFaint)
        Spacer(Modifier.height(AxiomTheme.space.xs))
        Text(
            entry.displayTitle.ifBlank { "Untitled" },
            style = AxiomTheme.type.readingTitle,
            color = c.ink
        )
        Spacer(Modifier.height(AxiomTheme.space.sm))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AxiomTheme.space.sm)
        ) {
            Text("${entry.wordCount} words", style = AxiomTheme.type.uiMeta, color = c.inkFaint)
            if (entry.mood != null) {
                Text("·", style = AxiomTheme.type.uiMeta, color = c.inkFaint)
                MoodDot(entry.mood, size = 10.dp)
                Text(moodLabel(entry.mood), style = AxiomTheme.type.uiMeta, color = c.inkFaint)
            }
        }
        if (entry.tags.isNotEmpty()) {
            Spacer(Modifier.height(AxiomTheme.space.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(AxiomTheme.space.xs)) {
                entry.tags.take(4).forEach { TagChip(it) }
            }
        }
    }
}

/**
 * AI output renders with the aiTint rule so it is never mistaken for the user's
 * own words. When there is no insight, this shows nothing at all — an app that
 * nags for an API key on every entry is worse than one that stays quiet.
 */
@Composable
private fun InsightCard(insight: AIInsight) {
    val c = AxiomTheme.colors
    AxiomCard(tone = CardTone.Ai) {
        Text("AI note", style = AxiomTheme.type.uiOverline, color = c.aiTint)
        Spacer(Modifier.height(AxiomTheme.space.sm))
        Text(insight.summary, style = AxiomTheme.type.readingLead, color = c.ink)
        if (insight.themes.isNotEmpty()) {
            Spacer(Modifier.height(AxiomTheme.space.md))
            Row(horizontalArrangement = Arrangement.spacedBy(AxiomTheme.space.xs)) {
                insight.themes.take(4).forEach { theme ->
                    Text(
                        theme,
                        style = AxiomTheme.type.uiLabelSmall,
                        color = c.onAiTintSoft,
                        modifier = Modifier
                            .background(c.aiTint.copy(alpha = 0.15f), AxiomTheme.shapes.xs)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
        // Attribution: the user can always see which model wrote this.
        Spacer(Modifier.height(AxiomTheme.space.md))
        Text(
            "${insight.modelName} · ${insight.totalTokens} tokens",
            style = AxiomTheme.type.uiMeta,
            color = c.inkFaint
        )
    }
}
