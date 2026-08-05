package com.cosmiclaboratory.axiom.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cosmiclaboratory.axiom.domain.model.Entry
import com.cosmiclaboratory.axiom.ui.design.components.*
import com.cosmiclaboratory.axiom.ui.theme.AxiomTheme

@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onOpenEntry: (Long) -> Unit,
    onAskInstead: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = AxiomTheme.colors
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(c.canvas)
            .testTag("screen:search")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AxiomTheme.space.sm, vertical = AxiomTheme.space.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AxiomIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack)
            Box(Modifier.weight(1f)) {
                if (state.query.isEmpty()) {
                    Text(
                        "Search ${state.corpusSize} entries",
                        style = AxiomTheme.type.uiBody,
                        color = c.inkFaint
                    )
                }
                BasicTextField(
                    value = state.query,
                    onValueChange = viewModel::setQuery,
                    textStyle = AxiomTheme.type.uiBody.copy(color = c.ink),
                    cursorBrush = SolidColor(c.accent),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onSearch = { viewModel.commitToRecent() }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .testTag("field:search")
                )
            }
            if (state.query.isNotEmpty()) {
                AxiomIconButton(Icons.Filled.Close, "Clear search", viewModel::clear, tint = c.inkMuted)
            }
        }

        if (state.isSearching) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = c.accent,
                trackColor = c.surfaceSunken
            )
        }

        when {
            state.query.isBlank() && state.recent.isNotEmpty() ->
                RecentSearches(state.recent, viewModel::setQuery)

            state.query.isBlank() -> AxiomEmptyState(
                title = "Search your journal",
                body = "Find an entry by a word you remember writing."
            )

            // No literal match, but the meaning index found entries about the
            // same thing in different words — far more useful than "no results".
            state.hasSearched && state.results.isEmpty() && state.related.isNotEmpty() ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize().testTag("list:search-related"),
                    contentPadding = PaddingValues(AxiomTheme.space.screenH),
                    verticalArrangement = Arrangement.spacedBy(AxiomTheme.space.listGap)
                ) {
                    item("related-header") {
                        Column {
                            Text(
                                "Nothing contains \"${state.query}\", but these feel related",
                                style = AxiomTheme.type.uiBodySmall,
                                color = AxiomTheme.colors.inkMuted
                            )
                            Spacer(Modifier.height(AxiomTheme.space.sm))
                        }
                    }
                    items(state.related, key = { it.id }) { entry ->
                        SearchResultCard(entry, "", axiomItemMotion()) { onOpenEntry(entry.id) }
                    }
                    item("ask") {
                        TextButton(onClick = { onAskInstead(state.query) }) {
                            Text(
                                "Ask instead",
                                style = AxiomTheme.type.uiLabel,
                                color = AxiomTheme.colors.accent
                            )
                        }
                    }
                }

            state.hasSearched && state.results.isEmpty() -> AxiomEmptyState(
                title = "No matches",
                body = "Nothing in your entries contains \"${state.query}\".",
                // The best cross-sell in the app: keyword search failed, but a
                // semantic question might still land.
                primaryLabel = "Ask instead",
                onPrimary = { onAskInstead(state.query) }
            )

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("list:search"),
                contentPadding = PaddingValues(AxiomTheme.space.screenH),
                verticalArrangement = Arrangement.spacedBy(AxiomTheme.space.listGap)
            ) {
                items(state.results, key = { it.id }) { entry ->
                    SearchResultCard(entry, state.query, axiomItemMotion()) { onOpenEntry(entry.id) }
                }
            }
        }
    }
}

@Composable
private fun RecentSearches(recent: List<String>, onPick: (String) -> Unit) {
    Column(Modifier.padding(AxiomTheme.space.screenH)) {
        SectionHeader("Recent")
        Spacer(Modifier.height(AxiomTheme.space.sm))
        recent.forEach { query ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPick(query) }
                    .padding(vertical = AxiomTheme.space.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.History,
                    contentDescription = null,
                    tint = AxiomTheme.colors.inkFaint
                )
                Spacer(Modifier.width(AxiomTheme.space.md))
                Text(query, style = AxiomTheme.type.uiBody, color = AxiomTheme.colors.ink)
            }
        }
    }
}

@Composable
private fun SearchResultCard(
    entry: Entry,
    query: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val c = AxiomTheme.colors
    // FTS4 offers no snippet() here, so the match window and highlighting are
    // computed client-side against the raw text.
    val snippet = remember(entry.content, query) { snippetAround(entry.content, query) }

    AxiomCard(onClick = onClick, modifier = modifier) {
        Text(
            entry.displayTitle.ifBlank { "Untitled" },
            style = AxiomTheme.type.uiTitle,
            color = c.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(AxiomTheme.space.xs))
        Text(
            text = highlight(snippet, query, c.highlight, c.ink),
            style = AxiomTheme.type.uiBody,
            color = c.inkMuted,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** A window of text centred on the first match, so results show why they matched. */
private fun snippetAround(content: String, query: String, radius: Int = 90): String {
    val term = query.trim().split(Regex("\\s+")).firstOrNull { it.length >= 2 } ?: return content.take(180)
    val idx = content.indexOf(term, ignoreCase = true)
    if (idx < 0) return content.take(180)
    val start = (idx - radius).coerceAtLeast(0)
    val end = (idx + term.length + radius).coerceAtMost(content.length)
    return buildString {
        if (start > 0) append("…")
        append(content.substring(start, end).trim())
        if (end < content.length) append("…")
    }
}

private fun highlight(
    text: String,
    query: String,
    background: androidx.compose.ui.graphics.Color,
    foreground: androidx.compose.ui.graphics.Color
): AnnotatedString {
    val terms = query.trim().split(Regex("\\s+")).filter { it.length >= 2 }
    if (terms.isEmpty()) return AnnotatedString(text)
    return buildAnnotatedString {
        append(text)
        terms.forEach { term ->
            var index = text.indexOf(term, ignoreCase = true)
            while (index >= 0) {
                addStyle(
                    SpanStyle(background = background, color = foreground),
                    index,
                    (index + term.length).coerceAtMost(text.length)
                )
                index = text.indexOf(term, index + term.length, ignoreCase = true)
            }
        }
    }
}
