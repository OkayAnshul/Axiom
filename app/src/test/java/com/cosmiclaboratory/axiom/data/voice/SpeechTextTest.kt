package com.cosmiclaboratory.axiom.data.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechTextTest {

    @Test
    fun `complete sentences are extracted and tail kept`() {
        val (sentences, tail) = extractSpeakableSentences(
            "That sounds like a really long day. You handled it well though! And tomo"
        )
        assertEquals(
            listOf("That sounds like a really long day.", "You handled it well though!"),
            sentences
        )
        assertEquals(" And tomo", tail)
    }

    @Test
    fun `short fragments merge into the next sentence instead of stuttering`() {
        val (sentences, _) = extractSpeakableSentences("Dr. Mehta called you back today, right?")
        assertEquals(listOf("Dr. Mehta called you back today, right?"), sentences)
    }

    @Test
    fun `no enders means everything stays in the tail`() {
        val (sentences, tail) = extractSpeakableSentences("still streaming this senten")
        assertTrue(sentences.isEmpty())
        assertEquals("still streaming this senten", tail)
    }

    @Test
    fun `devanagari danda ends a sentence`() {
        val (sentences, _) = extractSpeakableSentences("आज का दिन बहुत अच्छा था। और कल")
        assertEquals(listOf("आज का दिन बहुत अच्छा था।"), sentences)
    }

    @Test
    fun `markdown is stripped for speech`() {
        assertEquals(
            "A bold plan and a link plus code.",
            stripMarkdownForSpeech("A **bold** plan and [a link](https://x.y) plus `code`.")
        )
        assertEquals("Heading Item one", stripMarkdownForSpeech("## Heading\n- Item one"))
    }
}
