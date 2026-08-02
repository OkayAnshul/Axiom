package com.cosmiclaboratory.axiom.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkdownCheatsheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    "Markdown cheatsheet",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            item {
                Text(
                    "Quick reference. There's no tutorial — just write, and Axiom renders.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(SECTIONS) { section ->
                CheatsheetSection(section)
            }
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text("Voice tip", fontWeight = FontWeight.SemiBold)
                Text(
                    "Long-press the mic FAB to switch language: English (US/India), हिंदी, or Hinglish — Hinglish uses Groq Whisper for accurate code-mix transcription.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private data class CheatsheetEntry(val syntax: String, val rendered: String)
private data class CheatsheetSectionData(val title: String, val entries: List<CheatsheetEntry>)

@Composable
private fun CheatsheetSection(section: CheatsheetSectionData) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(section.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
        section.entries.forEach { e ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    e.syntax,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                BasicText(
                    text = androidx.compose.ui.text.AnnotatedString(e.rendered),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private val SECTIONS = listOf(
    CheatsheetSectionData(
        "Headings & emphasis",
        listOf(
            CheatsheetEntry("# Heading 1", "Heading 1"),
            CheatsheetEntry("## Heading 2", "Heading 2"),
            CheatsheetEntry("**bold**", "bold"),
            CheatsheetEntry("*italic*", "italic"),
            CheatsheetEntry("~~strike~~", "strike")
        )
    ),
    CheatsheetSectionData(
        "Lists & tasks",
        listOf(
            CheatsheetEntry("- bullet", "• bullet"),
            CheatsheetEntry("1. ordered", "1. ordered"),
            CheatsheetEntry("- [ ] todo", "☐ todo"),
            CheatsheetEntry("- [x] done", "☑ done")
        )
    ),
    CheatsheetSectionData(
        "Quotes & code",
        listOf(
            CheatsheetEntry("> quote", "▍ quote"),
            CheatsheetEntry("`inline code`", "inline code"),
            CheatsheetEntry("```\\nblock\\n```", "code block")
        )
    ),
    CheatsheetSectionData(
        "Links",
        listOf(
            CheatsheetEntry("[label](url)", "label"),
            CheatsheetEntry("![alt](image.png)", "[image]")
        )
    )
)
