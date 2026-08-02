package com.cosmiclaboratory.axiom.utils

import com.cosmiclaboratory.axiom.utils.MarkdownParser.Element
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers every Element type the parser can emit.
 *
 * The renderer switches exhaustively over these, so an unhandled type would
 * render as nothing — silent data loss. There were no parser tests before this
 * despite ~310 lines of parsing logic and three renderers depending on it.
 */
class MarkdownParserTest {

    private fun parse(md: String) = MarkdownParser.parse(md)

    private inline fun <reified T> assertProduces(md: String) {
        val flat = parse(md).flatMap { el ->
            if (el is Element.Paragraph) el.elements else listOf(el)
        }
        assertTrue(
            "Expected ${T::class.simpleName} from: $md — got ${flat.map { it::class.simpleName }}",
            flat.any { it is T }
        )
    }

    // ---- block elements ----

    @Test fun `blank input yields nothing`() = assertEquals(emptyList<Element>(), parse(""))

    @Test fun `headers parse at every level`() {
        (1..6).forEach { level ->
            val el = parse("${"#".repeat(level)} Title").first()
            assertTrue(el is Element.Header)
            assertEquals(level, (el as Element.Header).level)
            assertEquals("Title", el.text)
        }
    }

    @Test fun `code block keeps language and body`() {
        val el = parse("```kotlin\nval x = 1\n```").first()
        assertTrue(el is Element.CodeBlock)
        assertEquals("kotlin", (el as Element.CodeBlock).language)
        assertTrue(el.code.contains("val x = 1"))
    }

    @Test fun `quote is recognised`() = assertProduces<Element.Quote>("> a quiet line")

    @Test fun `horizontal rule is recognised`() = assertProduces<Element.HorizontalRule>("---")

    @Test fun `bullet list is recognised`() = assertProduces<Element.ListItem>("- one")

    @Test fun `numbered list keeps its number`() {
        val el = parse("3. third").first()
        assertTrue(el is Element.NumberedListItem)
        assertEquals(3, (el as Element.NumberedListItem).number)
    }

    @Test fun `task captures completion state`() {
        val open = parse("- [ ] wash up").first() as Element.Task
        val done = parse("- [x] wash up").first() as Element.Task
        assertTrue(!open.completed)
        assertTrue(done.completed)
        assertEquals("wash up", done.text)
    }

    @Test fun `table keeps headers and rows`() {
        val el = parse("| a | b |\n|---|---|\n| 1 | 2 |").first()
        assertTrue(el is Element.Table)
        val table = el as Element.Table
        assertEquals(listOf("a", "b"), table.headers)
        assertEquals(1, table.rows.size)
    }

    @Test fun `empty line is preserved as structure`() =
        assertProduces<Element.EmptyLine>("a\n\nb")

    // ---- inline elements ----

    @Test fun `bold`() = assertProduces<Element.Bold>("some **strong** words")
    @Test fun `italic`() = assertProduces<Element.Italic>("some *soft* words")
    @Test fun `strikethrough`() = assertProduces<Element.Strikethrough>("some ~~gone~~ words")
    @Test fun `highlight`() = assertProduces<Element.Highlight>("some ==lit== words")
    @Test fun `underline`() = assertProduces<Element.Underline>("some __under__ words")
    @Test fun `inline code`() = assertProduces<Element.Code>("call `foo()` here")
    @Test fun `link`() = assertProduces<Element.Link>("see [docs](https://example.com)")
    @Test fun `image`() = assertProduces<Element.Image>("![alt](https://example.com/a.png)")
    @Test fun `plain text`() = assertProduces<Element.Text>("just words")

    // ---- robustness ----

    @Test fun `malformed markup degrades to text rather than vanishing`() {
        // Unclosed markers must not swallow content — losing a user's words is
        // far worse than rendering an asterisk.
        listOf("**unclosed bold", "[link with no target", "```unterminated fence", "| ragged |")
            .forEach { input ->
                val out = parse(input)
                assertTrue("'$input' produced nothing", out.isNotEmpty())
            }
    }

    @Test fun `nested paragraph never contains another paragraph`() {
        // The renderer recurses exactly one level; deeper nesting would drop content.
        parse("a **b** c\n\nd *e* f").filterIsInstance<Element.Paragraph>().forEach { p ->
            assertTrue(
                "Paragraph must not nest",
                p.elements.none { it is Element.Paragraph }
            )
        }
    }

    @Test fun `long document parses without error`() {
        val doc = buildString { repeat(500) { appendLine("- item $it") } }
        assertNotNull(parse(doc))
        assertTrue(parse(doc).size >= 500)
    }
}
