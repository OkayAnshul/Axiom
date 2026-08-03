package com.cosmiclaboratory.axiom.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EmotionMapperTest {

    @Test
    fun `canonical labels map to themselves`() {
        Emotion.entries.forEach { emotion ->
            assertEquals(emotion, EmotionMapper.fromWord(emotion.label))
        }
    }

    @Test
    fun `common model words map to the right feeling`() {
        assertEquals(Emotion.ANXIETY, EmotionMapper.fromWord("anxious"))
        assertEquals(Emotion.BURNOUT, EmotionMapper.fromWord("drained"))
        assertEquals(Emotion.SADNESS, EmotionMapper.fromWord("down"))
        assertEquals(Emotion.JOY, EmotionMapper.fromWord("happy"))
        assertEquals(Emotion.STEADY, EmotionMapper.fromWord("meh"))
        assertEquals(Emotion.MOTIVATION, EmotionMapper.fromWord("productive"))
    }

    @Test
    fun `case punctuation and stray whitespace do not matter`() {
        assertEquals(Emotion.ANGER, EmotionMapper.fromWord("  Frustrated. "))
        assertEquals(Emotion.EXCITEMENT, EmotionMapper.fromWord("\"Excited\""))
    }

    @Test
    fun `hedged phrases still resolve`() {
        assertEquals(Emotion.ANXIETY, EmotionMapper.fromWord("a bit anxious"))
        assertEquals(Emotion.CALM, EmotionMapper.fromWord("quietly calm"))
    }

    @Test
    fun `an unrecognised word yields null rather than a neutral guess`() {
        assertNull(EmotionMapper.fromWord("zorbling"))
        assertNull(EmotionMapper.fromWord(""))
        assertNull(EmotionMapper.fromWord("   "))
        assertNull(EmotionMapper.fromWord(null))
    }

    @Test
    fun `valence spans the whole mood scale and stays in range`() {
        val valences = Emotion.entries.map { it.valence }
        assertTrue(valences.all { it in 1..5 })
        assertEquals(1, valences.min())
        assertEquals(5, valences.max())
    }

    @Test
    fun `distinct feelings can share a valence`() {
        // The label is the truth; the number is only a projection.
        assertEquals(Emotion.ANXIETY.valence, Emotion.ANGER.valence)
        assertTrue(Emotion.ANXIETY != Emotion.ANGER)
    }

    @Test
    fun `storage round-trips and tolerates junk`() {
        assertEquals(Emotion.GRIEF, Emotion.fromStorage("GRIEF"))
        assertNull(Emotion.fromStorage("NOT_A_FEELING"))
        assertNull(Emotion.fromStorage(null))
    }
}
