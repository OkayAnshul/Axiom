package com.cosmiclaboratory.axiom.domain.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Assert.assertNull

class VoiceProfileTest {

    @Test
    fun `every dial contributes a line`() {
        val everythingOn = VoiceProfile(
            register = Register.Blunt,
            humour = Humour.Dark,
            profanity = Profanity.Unrestrained,
            emoji = Emoji.Free,
            pushback = Pushback.Challenge,
            advice = Advice.Freely
        )
        val lines = everythingOn.instructionLines()
        // Derived rather than hardcoded: adding a dial should extend this test,
        // not break it. Register, pushback and advice always speak; humour,
        // profanity and emoji speak only when not Off.
        val optional = listOfNotNull(
            everythingOn.humour.instruction,
            everythingOn.profanity.instruction,
            everythingOn.emoji.instruction
        )
        assertEquals(lines.toString(), ALWAYS_ON_DIALS + optional.size, lines.size)
    }

    /**
     * An "off" dial must be absent, not negated. Telling a model "no humour" or
     * "do not swear" spends prompt budget planting the very idea, and reads to
     * the model as a topic worth attending to.
     */
    @Test
    fun `off dials omit their line rather than negating it`() {
        val lines = VoiceProfile(
            humour = Humour.None,
            profanity = Profanity.Off,
            emoji = Emoji.Off
        ).instructionLines()
        assertEquals(lines.toString(), ALWAYS_ON_DIALS, lines.size)
        assertFalse(lines.toString(), lines.any { it.contains("swear", ignoreCase = true) })
        assertFalse(lines.toString(), lines.any { it.contains("humour", ignoreCase = true) })
        assertFalse(lines.toString(), lines.any { it.contains("emoji", ignoreCase = true) })
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

    // ---- emoji ------------------------------------------------------------

    @Test
    fun `emoji off emits nothing rather than a prohibition`() {
        // Same reasoning as Profanity.Off: "do not use emoji" spends prompt
        // weight teaching the model about the thing it should ignore.
        assertNull(Emoji.Off.instruction)
        val lines = VoiceProfile(emoji = Emoji.Off).instructionLines()
        assertFalse(lines.toString(), lines.any { it.contains("emoji", ignoreCase = true) })
    }

    @Test
    fun `emoji is permission with a limit, never an instruction to decorate`() {
        val line = Emoji.Sparing.instruction!!
        assertTrue(line, line.contains("never more than one"))
        assertTrue(line, line.contains("never in a heavy moment"))
    }

    @Test
    fun `sparing emoji reaches the prompt lines`() {
        val lines = VoiceProfile(emoji = Emoji.Sparing).instructionLines()
        assertTrue(lines.toString(), lines.any { it.contains("emoji", ignoreCase = true) })
    }

    // ---- answering a direct question --------------------------------------

    @Test
    fun `even the gentlest pushback answers a direct question`() {
        // Tested against a live model before this existed: asked "what do you
        // think is actually going on with me", the shipped default replied
        // "I'm not here to figure that out". It was obeying the prompt.
        val line = Pushback.Never.instruction
        assertTrue(line, line.contains("ask you outright"))
    }

    @Test
    fun `the default voice is willing to disagree`() {
        assertEquals(Pushback.Sometimes, VoicePreset.Warm.profile().pushback)
    }

    private companion object {
        /** Register, pushback and advice have no "off" — they always emit a line. */
        const val ALWAYS_ON_DIALS = 3
    }
}
