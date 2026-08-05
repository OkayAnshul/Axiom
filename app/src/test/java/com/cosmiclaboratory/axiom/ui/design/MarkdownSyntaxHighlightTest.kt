package com.cosmiclaboratory.axiom.ui.design

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import com.cosmiclaboratory.axiom.ui.design.components.MarkdownSyntaxHighlight
import com.cosmiclaboratory.axiom.ui.theme.AxiomTypography
import com.cosmiclaboratory.axiom.ui.theme.NightInkColors
import com.cosmiclaboratory.axiom.ui.theme.MorningPaperColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the one property the composer depends on: the transformation STYLES
 * text without changing it.
 *
 * TextEditorStateManager, SmartDeletionHandler and TemplatePatternMatcher all
 * reason about absolute character offsets. If this ever inserted or removed a
 * character, template insertion and smart backspace would corrupt entries in
 * ways that only appear while typing — the worst kind of bug to find late.
 */
class MarkdownSyntaxHighlightTest {

    private val transform = MarkdownSyntaxHighlight(NightInkColors, AxiomTypography())

    private val samples = listOf(
        "",
        "plain text",
        "**bold**",
        "*italic*",
        "~~strike~~",
        "==highlight==",
        "__underline__",
        "`code`",
        "# Heading",
        "###### Small heading",
        "> quoted line",
        "- bullet",
        "- [ ] open task",
        "- [x] done task",
        "[label](https://example.com)",
        "---",
        "mixed **bold** and *italic* and `code` together",
        "**unclosed bold",
        "# \n\n> \n\n- ",
        "multi\nline\ncontent with **bold** on line three"
    )

    @Test
    fun `text is never altered`() {
        samples.forEach { input ->
            val result = transform.filter(AnnotatedString(input))
            assertEquals(
                "Transformation changed the text for: '$input'",
                input,
                result.text.text
            )
        }
    }

    @Test
    fun `length is preserved exactly`() {
        samples.forEach { input ->
            val result = transform.filter(AnnotatedString(input))
            assertEquals(
                "Length changed for: '$input'",
                input.length,
                result.text.length
            )
        }
    }

    @Test
    fun `offset mapping is identity`() {
        samples.forEach { input ->
            val result = transform.filter(AnnotatedString(input))
            assertSame(
                "Non-identity mapping for: '$input'",
                OffsetMapping.Identity,
                result.offsetMapping
            )
            // Belt and braces: verify every offset round-trips.
            for (offset in 0..input.length) {
                assertEquals(offset, result.offsetMapping.originalToTransformed(offset))
                assertEquals(offset, result.offsetMapping.transformedToOriginal(offset))
            }
        }
    }

    @Test
    fun `styling is actually applied`() {
        // If nothing were styled the identity tests above would pass vacuously.
        val result = transform.filter(AnnotatedString("**bold** and `code`"))
        assertTrue("Expected span styles to be applied", result.text.spanStyles.isNotEmpty())
    }

    @Test
    fun `spans stay within bounds`() {
        samples.forEach { input ->
            val result = transform.filter(AnnotatedString(input))
            result.text.spanStyles.forEach { span ->
                assertTrue(
                    "Span ${span.start}..${span.end} out of bounds for '$input' (len ${input.length})",
                    span.start >= 0 && span.end <= input.length && span.start <= span.end
                )
            }
        }
    }

    @Test
    fun `oversized input skips styling but keeps text intact`() {
        val huge = "**bold** ".repeat(5000)
        val result = transform.filter(AnnotatedString(huge))
        assertEquals(huge, result.text.text)
        assertTrue("Styling should be skipped past the cap", result.text.spanStyles.isEmpty())
    }

    @Test
    fun `works with both palettes`() {
        val light = MarkdownSyntaxHighlight(MorningPaperColors, AxiomTypography())
        val input = "# Title with **bold**"
        assertEquals(input, light.filter(AnnotatedString(input)).text.text)
    }
}
