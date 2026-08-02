package com.cosmiclaboratory.axiom.ui.design.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import com.cosmiclaboratory.axiom.ui.theme.AxiomColors
import com.cosmiclaboratory.axiom.ui.theme.AxiomTypography

/**
 * Styles markdown in place while the user types.
 *
 * Identity offset mapping is the whole design constraint. `TextEditorStateManager`,
 * `SmartDeletionHandler` and `TemplatePatternMatcher` all reason about absolute
 * character offsets; anything that inserts or removes characters would silently
 * break template insertion and smart backspace. So this only ever *styles* —
 * `transformedText.length` always equals `text.length`.
 *
 * That is also why this is a VisualTransformation rather than the newer
 * `OutputTransformation`: the latter is designed to change text, which is
 * precisely what we must not do.
 *
 * Markers are DIMMED, never hidden. Hiding `**` makes the caret appear to jump
 * two characters when crossing them, which feels broken.
 */
class MarkdownSyntaxHighlight(
    private val colors: AxiomColors,
    private val type: AxiomTypography,
    /** Above this length, styling is skipped. Journal entries never reach it. */
    private val maxLength: Int = 20_000
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        if (text.isEmpty() || text.length > maxLength) {
            return TransformedText(text, OffsetMapping.Identity)
        }
        return TransformedText(annotate(text.text), OffsetMapping.Identity)
    }

    private fun annotate(raw: String): AnnotatedString = AnnotatedString.Builder(raw).apply {
        applyLineStyles(raw)
        applyInline(raw, Regex("""\*\*(.+?)\*\*"""), SpanStyle(fontWeight = FontWeight.Bold), 2)
        applyInline(raw, Regex("""(?<!\*)\*(?!\*)(.+?)(?<!\*)\*(?!\*)"""), SpanStyle(fontStyle = FontStyle.Italic), 1)
        applyInline(raw, Regex("""~~(.+?)~~"""), SpanStyle(textDecoration = TextDecoration.LineThrough), 2)
        applyInline(raw, Regex("""==(.+?)=="""), SpanStyle(background = colors.highlight), 2)
        applyInline(raw, Regex("""__(.+?)__"""), SpanStyle(textDecoration = TextDecoration.Underline), 2)
        applyInline(
            raw, Regex("""`([^`\n]+)`"""),
            SpanStyle(fontFamily = FontFamily.Monospace, background = colors.surfaceSunken), 1
        )
        applyLinks(raw)
    }.toAnnotatedString()

    /** Headers, quotes, list and task markers — anchored to line starts. */
    private fun AnnotatedString.Builder.applyLineStyles(raw: String) {
        var lineStart = 0
        raw.split("\n").forEach { line ->
            val lineEnd = lineStart + line.length
            val trimmed = line.trimStart()
            val indent = line.length - trimmed.length

            when {
                trimmed.startsWith("#") -> {
                    val level = trimmed.takeWhile { it == '#' }.length.coerceIn(1, 6)
                    if (trimmed.getOrNull(level) == ' ') {
                        val headerStyle = type.markdownHeader(level)
                        // Dim the hashes, style the text.
                        dim(lineStart + indent, lineStart + indent + level + 1)
                        addStyle(
                            SpanStyle(
                                fontSize = headerStyle.fontSize,
                                fontWeight = headerStyle.fontWeight,
                                fontFamily = headerStyle.fontFamily,
                                color = colors.ink
                            ),
                            lineStart + indent + level + 1,
                            lineEnd
                        )
                    }
                }

                trimmed.startsWith("> ") -> {
                    dim(lineStart + indent, lineStart + indent + 2)
                    addStyle(
                        SpanStyle(fontStyle = FontStyle.Italic, color = colors.inkMuted),
                        lineStart + indent + 2, lineEnd
                    )
                }

                trimmed.startsWith("- [ ]") || trimmed.startsWith("- [x]") ||
                    trimmed.startsWith("- [X]") -> {
                    val done = !trimmed.startsWith("- [ ]")
                    addStyle(
                        SpanStyle(color = if (done) colors.positive else colors.accent),
                        lineStart + indent, (lineStart + indent + 5).coerceAtMost(lineEnd)
                    )
                    if (done) {
                        addStyle(
                            SpanStyle(
                                textDecoration = TextDecoration.LineThrough,
                                color = colors.inkFaint
                            ),
                            (lineStart + indent + 5).coerceAtMost(lineEnd), lineEnd
                        )
                    }
                }

                trimmed.startsWith("- ") || trimmed.startsWith("* ") ->
                    addStyle(
                        SpanStyle(color = colors.accent),
                        lineStart + indent, (lineStart + indent + 1).coerceAtMost(lineEnd)
                    )

                trimmed.startsWith("---") || trimmed.startsWith("***") ->
                    dim(lineStart, lineEnd)
            }
            lineStart = lineEnd + 1
        }
    }

    private fun AnnotatedString.Builder.applyInline(
        raw: String,
        pattern: Regex,
        style: SpanStyle,
        markerLength: Int
    ) {
        pattern.findAll(raw).forEach { match ->
            val start = match.range.first
            val end = match.range.last + 1
            if (end - start <= markerLength * 2) return@forEach
            dim(start, start + markerLength)
            addStyle(style, start + markerLength, end - markerLength)
            dim(end - markerLength, end)
        }
    }

    private fun AnnotatedString.Builder.applyLinks(raw: String) {
        Regex("""\[([^\]]+)]\(([^)]+)\)""").findAll(raw).forEach { match ->
            val start = match.range.first
            val end = match.range.last + 1
            val textEnd = start + 1 + match.groupValues[1].length
            dim(start, start + 1)
            addStyle(
                SpanStyle(color = colors.accent, textDecoration = TextDecoration.Underline),
                start + 1, textEnd
            )
            dim(textEnd, end)
        }
    }

    /** Markers stay visible but recede, so the caret never appears to skip. */
    private fun AnnotatedString.Builder.dim(start: Int, end: Int) {
        if (start in 0..end) addStyle(SpanStyle(color = colors.inkFaint), start, end)
    }
}
