package com.cosmiclaboratory.axiom.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class PlainTextToMarkdownConverterTest {

    @Test
    fun convert_keepsPlainParagraphsReadable() {
        val result = PlainTextToMarkdownConverter.convert(
            """
            I felt scattered today.

            Walking helped me slow down.
            """.trimIndent()
        )

        assertEquals(
            "I felt scattered today.\n\nWalking helped me slow down.",
            result
        )
    }

    @Test
    fun convert_detectsSectionHeadingsAndLists() {
        val result = PlainTextToMarkdownConverter.convert(
            """
            Morning reset:
            I woke up late but recovered the day.

            todo call mom
            done cleaned desk
            list: read for ten minutes
            1) Drink water
            - Read for ten minutes
            """.trimIndent()
        )

        assertEquals(
            """
            ## Morning reset
            I woke up late but recovered the day.

            - [ ] Call mom
            - [x] Cleaned desk
            - Read for ten minutes
            1. Drink water
            - Read for ten minutes
            """.trimIndent(),
            result
        )
    }

    @Test
    fun convert_autolinksUrlsAndPreservesExistingMarkdown() {
        val result = PlainTextToMarkdownConverter.convert(
            """
            # Existing title
            Visit https://example.com today.
            - [ ] Already a task
            """.trimIndent()
        )

        assertEquals(
            """
            # Existing title
            Visit <https://example.com> today.
            - [ ] Already a task
            """.trimIndent(),
            result
        )
    }

    @Test
    fun convert_canIncludeTitleForStandaloneMarkdown() {
        val result = PlainTextToMarkdownConverter.convert(
            plainText = "A clear entry.",
            title = "Daily Note",
            includeTitle = true
        )

        assertEquals("# Daily Note\n\nA clear entry.", result)
    }
}
