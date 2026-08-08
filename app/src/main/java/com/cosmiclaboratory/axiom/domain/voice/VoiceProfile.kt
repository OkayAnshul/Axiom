package com.cosmiclaboratory.axiom.domain.voice

import com.cosmiclaboratory.axiom.domain.model.PersonaKey

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
 * Whether the companion may use emoji.
 *
 * Nothing in the prompt mentioned them, so the model used none — zero across
 * every reply in a live test. Like [Profanity] this is permission rather than
 * instruction, and [Off] emits nothing at all: telling a model "do not use
 * emoji" spends prompt weight teaching it about the thing you want ignored.
 */
enum class Emoji(val instruction: String?) {
    Off(null),
    Sparing(
        "An emoji occasionally, where it genuinely adds warmth — never more than one in a " +
            "reply, never decorative, and never in a heavy moment."
    ),
    Free("Emoji are welcome wherever they fit the feeling. Still never in a heavy moment.")
}

/**
 * Whether the companion may disagree.
 *
 * The old prompt's "At most one question per reply" and "No advice unless they
 * ask" removed the *mechanics* of pushback — disagreement almost always arrives
 * as either unsolicited advice or a second question — so no persona could argue
 * even in principle.
 *
 * [Never] carries an explicit carve-out because without it the two lines
 * contradict each other the moment a question is direct. Tested against a live
 * model: asked "what do you think is actually going on with me", the shipped
 * default replied "I'm not here to figure that out". It was obeying the prompt.
 * A companion that won't answer when asked is not being gentle, it is being
 * absent.
 */
enum class Pushback(val instruction: String) {
    Never(
        "Don't challenge them. Reflect and stay alongside. At most one question per reply. " +
            "But if they ask you outright what you think, tell them — refusing a direct " +
            "question is its own kind of distance."
    ),
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
        VoiceProfile(
            register = Register.Gentle, humour = Humour.None, profanity = Profanity.Off,
            emoji = Emoji.Sparing, pushback = Pushback.Never, advice = Advice.OnlyIfAsked
        )
    }),
    Warm("Warm", "A close friend who's glad you're here", {
        VoiceProfile(
            register = Register.Warm, humour = Humour.Light, profanity = Profanity.Off,
            // A close friend does sometimes disagree. Never meant the default
            // companion could only ever reflect, which is a mirror, not a friend.
            emoji = Emoji.Sparing, pushback = Pushback.Sometimes, advice = Advice.OnlyIfAsked
        )
    }),
    Level("Level", "Even and plain, no performance", {
        VoiceProfile(
            register = Register.Level, humour = Humour.None, profanity = Profanity.Off,
            emoji = Emoji.Off, pushback = Pushback.Sometimes, advice = Advice.OnlyIfAsked
        )
    }),
    Dry("Dry", "Deadpan, understated, quietly funny", {
        VoiceProfile(
            register = Register.Dry, humour = Humour.Dry, profanity = Profanity.Mild,
            // Deadpan undercuts itself with a smiley attached.
            emoji = Emoji.Off, pushback = Pushback.Sometimes, advice = Advice.OnlyIfAsked
        )
    }),
    Blunt("Blunt", "Tells you what it actually thinks", {
        VoiceProfile(
            register = Register.Blunt, humour = Humour.Dry, profanity = Profanity.Mild,
            emoji = Emoji.Off, pushback = Pushback.Challenge, advice = Advice.Freely
        )
    }),
    Unfiltered("Unfiltered", "Swears, argues, goes dark with you", {
        VoiceProfile(
            register = Register.Blunt, humour = Humour.Dark, profanity = Profanity.Unrestrained,
            emoji = Emoji.Sparing, pushback = Pushback.Challenge, advice = Advice.Freely
        )
    });

    /**
     * The persona this voice files its generated openers under.
     *
     * A persona is no longer a voice — this file is. What survives of it is a
     * partition key: [com.cosmiclaboratory.axiom.data.repository.QuestionRepository]
     * caches a batch of generated conversation openers per persona, and two
     * people with different voices should not be handed the same batch.
     *
     * So this mapping only has to be stable and injective. The names on either
     * side are not claims about each other, and nothing reads the persona's
     * prompt fragment any more.
     */
    fun toPersonaKey(): PersonaKey = when (this) {
        Gentle -> PersonaKey.CALM
        Warm -> PersonaKey.EMOTIONAL
        Level -> PersonaKey.ANALYTICAL
        Dry -> PersonaKey.SARCASTIC
        Blunt -> PersonaKey.ENERGETIC
        Unfiltered -> PersonaKey.DEEP
    }

    companion object {
        fun fromStorage(value: String?): VoicePreset =
            entries.firstOrNull { it.name == value } ?: Warm
    }
}

data class VoiceProfile(
    val register: Register = Register.Warm,
    val humour: Humour = Humour.Light,
    val profanity: Profanity = Profanity.Off,
    val emoji: Emoji = Emoji.Sparing,
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
        emoji.instruction?.let { add(it) }
        add(pushback.instruction)
        add(advice.instruction)
    }

    val hasCustomInstruction: Boolean get() = customInstruction.isNotBlank()

    companion object {
        /** Long enough to be a real instruction, short enough not to be a novel. */
        const val MAX_CUSTOM_LENGTH = 300
    }
}
