package com.cosmiclaboratory.axiom.ui.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.cosmiclaboratory.axiom.domain.model.TaskItem
import com.cosmiclaboratory.axiom.ui.theme.AxiomTheme
import com.cosmiclaboratory.axiom.utils.MarkdownParser
import com.cosmiclaboratory.axiom.utils.MarkdownParser.Element

/**
 * The one markdown renderer.
 *
 * Replaces three near-duplicate implementations (MarkdownPreview 691 lines,
 * EnhancedMarkdownPreview 740, and a third copy inside ModernSplitPaneEditor)
 * that had drifted apart — the card renderer had no Table/CodeBlock/Quote
 * branches at all, so the same note rendered differently in a list and in the
 * reader.
 *
 * Handles all 18 [Element] types. Anything the parser can emit, this draws.
 */
@Immutable
data class MarkdownStyle(
    val body: TextStyle,
    val quote: TextStyle,
    val code: TextStyle,
    val codeBlock: TextStyle,
    val paragraphGap: androidx.compose.ui.unit.Dp,
    val blockGap: androidx.compose.ui.unit.Dp,
    val header: (Int) -> TextStyle
) {
    companion object {
        /** Full reading treatment — reader, composer preview. */
        @Composable
        fun reading(): MarkdownStyle {
            val t = AxiomTheme.type
            return MarkdownStyle(
                body = t.readingBody,
                quote = t.readingQuote,
                code = t.codeInline,
                codeBlock = t.codeBlock,
                paragraphGap = AxiomTheme.space.md,
                blockGap = AxiomTheme.space.base,
                header = { level -> t.markdownHeader(level) }
            )
        }

        /** Compressed, for card snippets where vertical space is scarce. */
        @Composable
        fun compact(): MarkdownStyle {
            val t = AxiomTheme.type
            return MarkdownStyle(
                body = t.uiBody,
                quote = t.uiBody.copy(fontStyle = FontStyle.Italic),
                code = t.codeInline,
                codeBlock = t.codeInline,
                paragraphGap = AxiomTheme.space.xs,
                blockGap = AxiomTheme.space.sm,
                header = { level -> t.markdownHeader(level.coerceAtLeast(4)) }
            )
        }
    }
}

@Composable
fun AxiomMarkdown(
    source: String,
    modifier: Modifier = Modifier,
    style: MarkdownStyle = MarkdownStyle.reading(),
    onTaskToggle: ((TaskItem) -> Unit)? = null
) {
    // Parse is pure and cheap; memoise on the text so scrolling doesn't re-parse.
    val elements = remember(source) { MarkdownParser.parse(source) }
    val uriHandler = LocalUriHandler.current
    val c = AxiomTheme.colors

    Column(modifier = modifier) {
        elements.forEach { element ->
            when (element) {
                is Element.EmptyLine -> Spacer(Modifier.height(style.paragraphGap))

                is Element.Header -> {
                    Spacer(Modifier.height(style.blockGap))
                    Text(element.text, style = style.header(element.level), color = c.ink)
                    Spacer(Modifier.height(AxiomTheme.space.xs))
                }

                is Element.HorizontalRule -> {
                    Spacer(Modifier.height(style.blockGap))
                    HorizontalDivider(color = c.hairline)
                    Spacer(Modifier.height(style.blockGap))
                }

                is Element.Quote -> {
                    Row(Modifier.padding(vertical = AxiomTheme.space.xs)) {
                        Box(
                            Modifier
                                .width(3.dp)
                                .heightIn(min = 20.dp)
                                .background(c.accent.copy(alpha = 0.6f))
                        )
                        Spacer(Modifier.width(AxiomTheme.space.md))
                        Text(element.text, style = style.quote, color = c.inkMuted)
                    }
                }

                is Element.CodeBlock -> {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = AxiomTheme.space.xs)
                            .clip(AxiomTheme.shapes.sm)
                            .background(c.surfaceSunken)
                            .padding(AxiomTheme.space.md)
                    ) {
                        if (element.language.isNotBlank()) {
                            Text(
                                element.language,
                                style = AxiomTheme.type.uiOverline,
                                color = c.inkFaint
                            )
                            Spacer(Modifier.height(AxiomTheme.space.xs))
                        }
                        // Code must not wrap — horizontal scroll instead, so the
                        // page body never scrolls sideways.
                        Box(Modifier.horizontalScroll(rememberScrollState())) {
                            Text(element.code, style = style.codeBlock, color = c.ink)
                        }
                    }
                }

                is Element.Task -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Checkbox(
                            checked = element.completed,
                            onCheckedChange = if (onTaskToggle != null) {
                                { onTaskToggle(TaskItem(element.text, element.completed, 0, 0, 0)) }
                            } else null,
                            colors = CheckboxDefaults.colors(
                                checkedColor = c.accent,
                                uncheckedColor = c.outline
                            )
                        )
                        Text(
                            text = element.text,
                            style = style.body,
                            color = if (element.completed) c.inkFaint else c.ink,
                            textDecoration = if (element.completed) TextDecoration.LineThrough else null
                        )
                    }
                }

                is Element.ListItem -> BulletRow(
                    marker = "•",
                    text = element.text,
                    level = element.level,
                    style = style
                )

                is Element.NumberedListItem -> BulletRow(
                    marker = "${element.number}.",
                    text = element.text,
                    level = element.level,
                    style = style
                )

                is Element.Table -> MarkdownTable(element, style)

                is Element.Image -> {
                    // No image loader is bundled on purpose — a journal that works
                    // offline should not pull in a network image stack. Render a
                    // labelled placeholder that opens externally instead.
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = AxiomTheme.space.xs)
                            .clip(AxiomTheme.shapes.sm)
                            .background(c.surfaceSunken)
                            .padding(AxiomTheme.space.md)
                    ) {
                        Text(
                            text = "Image: ${element.alt.ifBlank { element.url }}",
                            style = AxiomTheme.type.uiBodySmall,
                            color = c.inkMuted
                        )
                    }
                }

                is Element.Paragraph -> InlineText(element.elements, style, onLink = uriHandler::openUri)

                // Inline elements can also appear at top level.
                else -> InlineText(listOf(element), style, onLink = uriHandler::openUri)
            }
        }
    }
}

@Composable
private fun BulletRow(marker: String, text: String, level: Int, style: MarkdownStyle) {
    Row(Modifier.padding(start = (level * 16).dp, top = 2.dp, bottom = 2.dp)) {
        Text(
            text = marker,
            style = style.body,
            color = AxiomTheme.colors.inkFaint,
            modifier = Modifier.widthIn(min = 20.dp)
        )
        Text(text, style = style.body, color = AxiomTheme.colors.ink)
    }
}

@Composable
private fun MarkdownTable(table: Element.Table, style: MarkdownStyle) {
    val c = AxiomTheme.colors
    // Tables scroll inside their own container so the page never scrolls sideways.
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = AxiomTheme.space.sm)
            .horizontalScroll(rememberScrollState())
    ) {
        Column(
            Modifier
                .border(1.dp, c.hairline, AxiomTheme.shapes.sm)
                .clip(AxiomTheme.shapes.sm)
        ) {
            Row(Modifier.background(c.surfaceSunken)) {
                table.headers.forEachIndexed { i, header ->
                    TableCell(header, style, table.alignments.getOrNull(i), bold = true)
                }
            }
            table.rows.forEach { row ->
                HorizontalDivider(color = c.hairline)
                Row {
                    row.forEachIndexed { i, cell ->
                        TableCell(cell, style, table.alignments.getOrNull(i))
                    }
                }
            }
        }
    }
}

@Composable
private fun TableCell(
    text: String,
    style: MarkdownStyle,
    alignment: MarkdownParser.TableAlignment?,
    bold: Boolean = false
) {
    Text(
        text = text,
        style = style.body.copy(fontWeight = if (bold) FontWeight.SemiBold else null),
        color = AxiomTheme.colors.ink,
        textAlign = when (alignment) {
            MarkdownParser.TableAlignment.CENTER -> TextAlign.Center
            MarkdownParser.TableAlignment.RIGHT -> TextAlign.End
            else -> TextAlign.Start
        },
        modifier = Modifier
            .widthIn(min = 88.dp)
            .padding(horizontal = AxiomTheme.space.md, vertical = AxiomTheme.space.sm)
    )
}

/** Flattens a run of inline elements into one styled string. */
@Composable
private fun InlineText(
    elements: List<Element>,
    style: MarkdownStyle,
    onLink: (String) -> Unit
) {
    val c = AxiomTheme.colors
    val annotated = buildAnnotatedString {
        elements.forEach { el ->
            when (el) {
                is Element.Text -> append(el.text)
                is Element.Bold -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(el.text) }
                is Element.Italic -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(el.text) }
                is Element.Strikethrough ->
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) { append(el.text) }
                is Element.Underline ->
                    withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) { append(el.text) }
                is Element.Highlight ->
                    withStyle(SpanStyle(background = c.highlight, color = c.ink)) { append(el.text) }
                is Element.Code -> withStyle(
                    SpanStyle(
                        fontFamily = style.code.fontFamily,
                        background = c.surfaceSunken,
                        color = c.ink
                    )
                ) { append(el.text) }
                is Element.Link -> withLink(el.text, el.url, c.accent)
                // Nested Paragraph never occurs (parser guarantees one level).
                else -> Unit
            }
        }
    }
    if (annotated.isEmpty()) return
    Text(text = annotated, style = style.body, color = c.ink)
}

private fun androidx.compose.ui.text.AnnotatedString.Builder.withLink(
    text: String,
    url: String,
    color: androidx.compose.ui.graphics.Color
) {
    pushStringAnnotation("URL", url)
    withStyle(SpanStyle(color = color, textDecoration = TextDecoration.Underline)) { append(text) }
    pop()
}
