package com.cosmiclaboratory.axiom.domain.voice

/**
 * How the companion talks.
 *
 * This replaces the old persona, which was one ~120-character sentence injected
 * under a header reading "a leaning, not a rule" and followed by a line telling
 * the model it lost every conflict. Six presets existed and five of them
 * cancelled themselves in their own second clause — the "Witty Friend" was
 * defined as "tease *gently* … and drop the wit entirely when they're hurting".
 * The net effect was six names for one voice.
 *
 * Each dial maps to exactly one prompt line, the same pattern
 * [com.cosmiclaboratory.axiom.domain.style.ReplyLength] already uses, so the
 * prompt builder stays a dumb assembler and every instruction the model sees is
 * greppable here.
 *
 * Nothing in this file is a content policy. The one thing that overrides a
 * chosen voice is [softenWhenStruggling], and that is the user's switch.
 */

/** The base temperature of the voice. Was hardcoded to "Warm" for everyone. */
enum class Register(val instruction: String) {
    Gentle("Speak softly and unhurried. Leave room; silence is an acceptable answer."),
    Warm("Warm, specific and brief. Match their energy and their length."),
    Level("Even and plain. No performance in either direction — neither cheerful nor cold."),
    Dry("Understated and a little detached. Say the true thing without decorating it."),
    Blunt("Say it straight. Skip the cushioning, skip the preamble, get to the point. " +
        "Being liked is not the goal; being useful and honest is.")
}

enum class Humour(val instruction: String?) {
    None(null),
    Light("A little lightness is welcome when it fits. Never at their expense."),
    Dry("Deadpan wit is welcome. Understate rather than exaggerate, and never explain the joke."),
    Dark(
        "Gallows humour is welcome — they use it themselves and it is how they cope. " +
            "Go as dark as they go, and never one step further than they just did."
    )
}

/**
 * Permission, never obligation.
 *
 * The model default-refuses profanity absent an explicit grant, so an allowance
 * has to be stated. But a model *told* to swear produces forced, cringing
 * output, which is worse than the sanitised version. These lines grant leave.
 */
enum class Profanity(val instruction: String?) {
    Off(null),
    Mild("Mild swearing is fine if it lands naturally. Don't force it."),
    Unrestrained(
        "Swearing is entirely fine — they asked for it. Don't self-censor, soften words, " +
            "or use asterisks. Equally, don't perform it: swear only where a friend " +
            "genuinely would."
    )
}

/**
 * Whether the companion may disagree.
 *
 * The old prompt's "At most one question per reply" and "No advice unless they
 * ask" removed the *mechanics* of pushback — disagreement almost always arrives
 * as either unsolicited advice or a second question — so no persona could argue
 * even in principle.
 */
enum class Pushback(val instruction: String) {
    Never("Don't challenge them. Reflect and stay alongside. At most one question per reply."),
    Sometimes("You can disagree when you actually do, and say so plainly rather than hinting."),
    Challenge(
        "Push back properly when they're kidding themselves. Name the thing they're avoiding, " +
            "argue with them, and don't retreat the moment they resist. Do this because you're " +
            "on their side, not to score points."
    )
}

enum class Advice(val instruction: String) {
    OnlyIfAsked("No advice unless they ask or clearly want it. Often the right move is to just acknowledge."),
    Freely("Say what you'd actually do in their position, without waiting to be asked.")
}

/** The named starting points a user picks before tuning anything. */
enum class VoicePreset(
    val displayName: String,
    val blurb: String,
    val profile: () -> VoiceProfile
) {
    Gentle("Gentle", "Soft, unhurried, lots of room", {
        VoiceProfile(Register.Gentle, Humour.None, Profanity.Off, Pushback.Never, Advice.OnlyIfAsked)
    }),
    Warm("Warm", "A close friend who's glad you're here", {
        VoiceProfile(Register.Warm, Humour.Light, Profanity.Off, Pushback.Never, Advice.OnlyIfAsked)
    }),
    Level("Level", "Even and plain, no performance", {
        VoiceProfile(Register.Level, Humour.None, Profanity.Off, Pushback.Sometimes, Advice.OnlyIfAsked)
    }),
    Dry("Dry", "Deadpan, understated, quietly funny", {
        VoiceProfile(Register.Dry, Humour.Dry, Profanity.Mild, Pushback.Sometimes, Advice.OnlyIfAsked)
    }),
    Blunt("Blunt", "Tells you what it actually thinks", {
        VoiceProfile(Register.Blunt, Humour.Dry, Profanity.Mild, Pushback.Challenge, Advice.Freely)
    }),
    Unfiltered("Unfiltered", "Swears, argues, goes dark with you", {
        VoiceProfile(Register.Blunt, Humour.Dark, Profanity.Unrestrained, Pushback.Challenge, Advice.Freely)
    });

    companion object {
        fun fromStorage(value: String?): VoicePreset =
            entries.firstOrNull { it.name == value } ?: Warm
    }
}

data class VoiceProfile(
    val register: Register = Register.Warm,
    val humour: Humour = Humour.Light,
    val profanity: Profanity = Profanity.Off,
    val pushback: Pushback = Pushback.Never,
    val advice: Advice = Advice.OnlyIfAsked,
    /**
     * The user's own words about how they want to be spoken to. Outranks every
     * dial above — nothing the app inferred should beat what they said outright.
     */
    val customInstruction: String = "",
    /**
     * When true, the dials are suspended for any turn where genuine distress is
     * detected. Default on: a companion that answers "I don't want to be here
     * anymore" in a comedy voice is the one failure this app must not have.
     * The helpline card appears either way — this governs only how it speaks.
     */
    val softenWhenStruggling: Boolean = true,
    /** Which preset this started from, for the settings UI. */
    val preset: VoicePreset = VoicePreset.Warm
) {
    /**
     * The lines that go into "How you talk". Order is deliberate: register sets
     * the base, the modifiers adjust it, and the user's own words land last so
     * they read as the final say.
     */
    fun instructionLines(): List<String> = buildList {
        add(register.instruction)
        humour.instruction?.let { add(it) }
        profanity.instruction?.let { add(it) }
        add(pushback.instruction)
        add(advice.instruction)
    }

    val hasCustomInstruction: Boolean get() = customInstruction.isNotBlank()

    companion object {
        /** Long enough to be a real instruction, short enough not to be a novel. */
        const val MAX_CUSTOM_LENGTH = 300
    }
}
