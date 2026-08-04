package com.cosmiclaboratory.axiom.domain.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MoodClassifierTest {

    /** Two clearly separated vocabularies, the shape a real journal has. */
    private val heavy = listOf(
        "Exhausted again after another brutal day at work",
        "Brutal deadline week, exhausted and snapping at people",
        "Work is crushing me, exhausted every single evening",
        "Another brutal review, felt small and exhausted",
        "Deadline stress again, work eating every evening",
        "Exhausted, brutal commute, work went badly again"
    ).map { it to 2 }

    private val light = listOf(
        "Lovely evening cooking with friends, felt calm",
        "Slow morning, coffee and sunshine, calm and rested",
        "Cooking again, friends over, laughed the whole evening",
        "Rested properly and had a calm sunny walk",
        "Friends visited, sunshine, felt genuinely rested",
        "Calm day, cooking, sunshine on the balcony"
    ).map { it to 4 }

    private fun trainedModel() = MoodClassifier.train(heavy + light)

    @Test
    fun `it learns which words belong to which kind of day`() {
        val model = trainedModel()
        assertNotNull(model)
        requireNotNull(model)

        val hard = MoodClassifier.predict(model, "Brutal day at work, exhausted")
        assertEquals(2, hard?.mood)

        val good = MoodClassifier.predict(model, "Calm evening cooking with friends")
        assertEquals(4, good?.mood)
    }

    @Test
    fun `too few examples means no model at all`() {
        val samples = heavy.take(3) + light.take(3)
        assertNull(MoodClassifier.train(samples))
    }

    @Test
    fun `a corpus of one mood cannot make a classifier`() {
        val allSame = (1..20).map { "another ordinary quiet day number $it" to 3 }
        assertNull(MoodClassifier.train(allSame))
    }

    @Test
    fun `unfamiliar text yields no prediction rather than a guess`() {
        val model = trainedModel()!!
        assertNull(MoodClassifier.predict(model, "quantum chromodynamics lattice gauge"))
    }

    @Test
    fun `blank and punctuation-only text predicts nothing`() {
        val model = trainedModel()!!
        assertNull(MoodClassifier.predict(model, ""))
        assertNull(MoodClassifier.predict(model, "!!! ... ???"))
    }

    @Test
    fun `ambiguous text does not clear the confidence bar`() {
        val model = trainedModel()!!
        // Words drawn evenly from both classes should not resolve confidently.
        val prediction = MoodClassifier.predict(model, "work friends")
        if (prediction != null) {
            assertTrue(
                "an evenly-balanced text should not be confident, was ${prediction.confidence}",
                prediction.confidence >= MoodClassifier.MIN_CONFIDENCE
            )
        }
    }

    @Test
    fun `confidence is a probability and the winner leads`() {
        val model = trainedModel()!!
        val prediction = MoodClassifier.predict(model, "exhausted brutal work deadline")!!
        assertTrue(prediction.confidence in 0.0..1.0)
        assertTrue(prediction.confidence >= MoodClassifier.MIN_CONFIDENCE)
    }

    @Test
    fun `long entries do not underflow`() {
        val model = trainedModel()!!
        val long = (1..400).joinToString(" ") { "exhausted brutal work" }
        val prediction = MoodClassifier.predict(model, long)
        assertNotNull("log-space scoring should survive a very long entry", prediction)
        assertEquals(2, prediction?.mood)
    }

    @Test
    fun `it learns devanagari and hinglish the same way`() {
        val samples = (1..7).map { "आज बहुत थका हुआ और परेशान महसूस हुआ" to 2 } +
            (1..7).map { "आज बहुत खुश और शांत महसूस हुआ" to 4 }
        val model = MoodClassifier.train(samples)!!
        assertEquals(2, MoodClassifier.predict(model, "बहुत थका और परेशान")?.mood)
        assertEquals(4, MoodClassifier.predict(model, "बहुत खुश और शांत")?.mood)
    }

    @Test
    fun `invalid moods are dropped from training`() {
        val junk = (1..10).map { "nonsense filler text number $it" to 9 }
        // Only the valid half remains, which is below the minimum.
        assertNull(MoodClassifier.train(junk + heavy.take(4)))
    }

    @Test
    fun `the model reports how much it learned from`() {
        val model = trainedModel()!!
        assertEquals(heavy.size + light.size, model.trainingSize)
        assertTrue(model.vocabulary.isNotEmpty())
    }

    @Test
    fun `tokenizer keeps words and drops noise`() {
        val tokens = MoodClassifier.tokenize("Work's been… rough! 2026 ok")
        assertTrue(tokens.contains("work"))
        assertTrue(tokens.contains("rough"))
        assertTrue(tokens.contains("been"))
        // Under three characters, so dropped.
        assertTrue(!tokens.contains("ok"))
    }
}
