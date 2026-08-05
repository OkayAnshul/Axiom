package com.cosmiclaboratory.axiom.domain.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceProfileTest {

    @Test
    fun `every dial contributes a line`() {
        val lines = VoiceProfile(
            register = Register.Blunt,
            humour = Humour.Dark,
            profanity = Profanity.Unrestrained,
            pushback = Pushback.Challenge,
            advice = Advice.Freely
        ).instructionLines()
        // register + humour + profanity + pushback + advice
        assertEquals(lines.toString(), 5, lines.size)
    }

    /**
     * An "off" dial must be absent, not negated. Telling a model "no humour" or
     * "do not swear" spends prompt budget planting the very idea, and reads to
     * the model as a topic worth attending to.
     */
    @Test
    fun `off dials omit their line rather than negating it`() {
        val lines = VoiceProfile(humour = Humour.None, profanity = Profanity.Off).instructionLines()
        assertEquals(lines.toString(), 3, lines.size)
        assertFalse(lines.toString(), lines.any { it.contains("swear", ignoreCase = true) })
        assertFalse(lines.toString(), lines.any { it.contains("humour", ignoreCase = true) })
    }

    @Test
    fun `presets are genuinely different from one another`() {
        val gentle = VoicePreset.Gentle.profile().instructionLines().joinToString(" ")
        val unfiltered = VoicePreset.Unfiltered.profile().instructionLines().joinToString(" ")

        assertTrue(gentle, gentle.contains("softly", ignoreCase = true))
        assertFalse("Gentle must not permit swearing: $gentle", gentle.contains("swear", true))

        assertTrue(unfiltered, unfiltered.contains("swearing", ignoreCase = true))
        assertTrue(unfiltered, unfiltered.contains("straight", ignoreCase = true))
    }

    /**
     * The old system's failure: the "Witty Friend" was defined as "tease
     * *gently* … and drop the wit entirely when they're hurting" — a persona
     * that revoked itself in its own second clause, which is why all six sounded
     * identical.
     */
    @Test
    fun `no register line cancels itself with a hedge`() {
        Register.entries.forEach { register ->
            val text = register.instruction.lowercase()
            listOf("but read the room", "drop it entirely", "never a lecture").forEach { hedge ->
                assertFalse("${register.name} hedges: $text", text.contains(hedge))
            }
        }
    }

    @Test
    fun `profanity reads as permission, not instruction to swear`() {
        val text = Profanity.Unrestrained.instruction.orEmpty()
        // A model told to swear performs it. This must grant leave instead.
        assertTrue(text, text.contains("don't perform it", ignoreCase = true))
        assertTrue(text, text.contains("Don't self-censor", ignoreCase = true))
    }

    @Test
    fun `custom instruction is detected only when meaningful`() {
        assertFalse(VoiceProfile().hasCustomInstruction)
        assertFalse(VoiceProfile(customInstruction = "   ").hasCustomInstruction)
        assertTrue(VoiceProfile(customInstruction = "be mean to me").hasCustomInstruction)
    }

    @Test
    fun `softening is on by default`() {
        assertTrue(VoiceProfile().softenWhenStruggling)
        VoicePreset.entries.forEach {
            assertTrue(it.name, it.profile().softenWhenStruggling)
        }
    }

    @Test
    fun `unknown stored preset falls back rather than crashing`() {
        assertEquals(VoicePreset.Warm, VoicePreset.fromStorage(null))
        assertEquals(VoicePreset.Warm, VoicePreset.fromStorage("SomethingRemoved"))
        assertEquals(VoicePreset.Blunt, VoicePreset.fromStorage("Blunt"))
    }
}
