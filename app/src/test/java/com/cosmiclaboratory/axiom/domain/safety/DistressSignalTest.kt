package com.cosmiclaboratory.axiom.domain.safety

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The negative cases matter more than the positive ones here. A missed signal
 * is a missed offer of help; a false one tells someone having an ordinary bad
 * day that the app thinks they might be suicidal, which is both patronising and
 * the fastest way to teach them to stop being honest with it.
 */
class DistressSignalTest {

    // ---- acute -------------------------------------------------------------

    @Test
    fun `stated intent is acute`() {
        listOf(
            "i want to kill myself",
            "I don't want to live anymore",
            "there's no reason to live",
            "everyone would be better without me",
            "I've been thinking about ending my life",
            "i want to die"
        ).forEach { assertEquals(it, CareLevel.Acute, DistressSignal.detect(it)) }
    }

    @Test
    fun `acute is detected in hinglish and devanagari`() {
        listOf(
            "mujhe marna chahta hoon",
            "ab jeena nahi chahta",
            "मुझे जीना नहीं चाहता"
        ).forEach { assertEquals(it, CareLevel.Acute, DistressSignal.detect(it)) }
    }

    @Test
    fun `curly apostrophes do not defeat detection`() {
        assertEquals(CareLevel.Acute, DistressSignal.detect("I don’t want to live"))
    }

    // ---- struggling --------------------------------------------------------

    @Test
    fun `distress without stated intent is struggling`() {
        listOf(
            "I can't go on like this",
            "I feel completely worthless",
            "everything is hopeless",
            "i hate myself today"
        ).forEach { assertEquals(it, CareLevel.Struggling, DistressSignal.detect(it)) }
    }

    // ---- the cases that must NOT fire --------------------------------------

    @Test
    fun `an ordinary bad day is not a signal`() {
        listOf(
            "Today was rough. Back-to-back standups again.",
            "I'm tired and a bit low, but okay",
            "work is killing me lately",
            "That deadline nearly finished me off",
            "I'm dying to see the new season",
            "I could kill for a coffee right now",
            "she absolutely killed it in the presentation"
        ).forEach { assertNull(it, DistressSignal.detect(it)) }
    }

    @Test
    fun `negated phrasing means the opposite and must not fire`() {
        listOf(
            "I don't want to die, I just want this to stop hurting",
            "I'm not suicidal, just exhausted"
        ).forEach { assertNull(it, DistressSignal.detect(it)) }
    }

    @Test
    fun `talking about someone else is not disclosure`() {
        listOf(
            "my friend said he wants to die and I'm scared for him",
            "in the movie a character kills himself"
        ).forEach { assertNull(it, DistressSignal.detect(it)) }
    }

    @Test
    fun `first person survives a third-party mention in the same message`() {
        // "my friend" is present, but so is the speaker's own disclosure.
        assertEquals(
            CareLevel.Acute,
            DistressSignal.detect("my friend is worried but i want to kill myself")
        )
    }

    @Test
    fun `blank and trivial input yields nothing`() {
        assertNull(DistressSignal.detect(""))
        assertNull(DistressSignal.detect("   "))
        assertNull(DistressSignal.detect("ok"))
    }
}
