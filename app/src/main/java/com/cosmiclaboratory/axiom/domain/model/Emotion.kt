package com.cosmiclaboratory.axiom.domain.model

/**
 * A named feeling, inferred from what the user wrote rather than tapped on a
 * scale.
 *
 * [valence] is a deliberately lossy projection onto the existing 1..5 mood
 * scale, kept only so sparklines and averages have a number to work with. The
 * label is the truth: "anxious" and "angry" both sit at 2 but are not the same
 * day, and anything user-facing should say the word, never the digit.
 */
enum class Emotion(val label: String, val valence: Int) {
    GRIEF("grief", 1),
    DESPAIR("despair", 1),
    SADNESS("sadness", 2),
    LONELINESS("loneliness", 2),
    ANGER("anger", 2),
    ANXIETY("anxiety", 2),
    STRESS("stress", 2),
    BURNOUT("burnout", 2),
    CONFUSION("confusion", 3),
    STEADY("steady", 3),
    CALM("calm", 4),
    RELIEF("relief", 4),
    MOTIVATION("motivation", 4),
    CONFIDENCE("confidence", 4),
    GRATITUDE("gratitude", 4),
    EXCITEMENT("excitement", 5),
    JOY("joy", 5);

    companion object {
        fun fromStorage(raw: String?): Emotion? =
            raw?.let { value -> runCatching { valueOf(value) }.getOrNull() }
    }
}

/**
 * Maps the free-text mood word a model returns onto an [Emotion].
 *
 * The model is asked for "one mood word" and obliges with whatever it likes —
 * "drained", "hopeful", "meh". A closed synonym table is used rather than
 * letting arbitrary strings through, because everything downstream (patterns,
 * sparklines, the companion's own phrasing) needs a bounded vocabulary. An
 * unrecognised word maps to null rather than to a neutral default: a wrong
 * feeling recorded confidently is worse than no feeling recorded.
 */
object EmotionMapper {

    fun fromWord(raw: String?): Emotion? {
        val word = raw?.trim()?.lowercase()?.trim('.', '!', ',', '"', '\'') ?: return null
        if (word.isEmpty()) return null
        SYNONYMS[word]?.let { return it }
        // Models like to answer "quietly hopeful" or "a bit anxious".
        return word.split(NON_LETTERS).firstNotNullOfOrNull { SYNONYMS[it] }
    }

    private val NON_LETTERS = Regex("[^\\p{L}]+")

    private val SYNONYMS: Map<String, Emotion> = buildMap {
        fun put(emotion: Emotion, vararg words: String) {
            put(emotion.label, emotion)
            words.forEach { put(it, emotion) }
        }
        put(Emotion.GRIEF, "grieving", "bereaved", "heartbroken", "loss", "mourning")
        put(Emotion.DESPAIR, "hopeless", "despairing", "miserable", "awful", "terrible", "devastated", "empty")
        put(Emotion.SADNESS, "sad", "down", "low", "blue", "unhappy", "disappointed", "melancholy", "dejected")
        put(Emotion.LONELINESS, "lonely", "alone", "isolated", "disconnected", "left out")
        put(Emotion.ANGER, "angry", "frustrated", "annoyed", "irritated", "resentful", "furious", "bitter")
        put(Emotion.ANXIETY, "anxious", "worried", "nervous", "afraid", "scared", "uneasy", "apprehensive", "dread")
        put(Emotion.STRESS, "stressed", "overwhelmed", "pressured", "tense", "strained", "frazzled")
        put(Emotion.BURNOUT, "burntout", "burnt", "exhausted", "drained", "depleted", "tired", "weary", "spent")
        put(Emotion.CONFUSION, "confused", "conflicted", "uncertain", "unsure", "torn", "lost", "ambivalent")
        put(Emotion.STEADY, "neutral", "okay", "ok", "fine", "alright", "even", "mixed", "meh", "flat", "ordinary")
        put(Emotion.CALM, "calm", "peaceful", "settled", "content", "rested", "quiet", "serene", "grounded")
        put(Emotion.RELIEF, "relieved", "lighter", "unburdened", "reassured")
        put(Emotion.MOTIVATION, "motivated", "determined", "driven", "focused", "productive", "energized", "inspired")
        put(Emotion.CONFIDENCE, "confident", "capable", "proud", "assured", "accomplished", "strong")
        put(Emotion.GRATITUDE, "grateful", "thankful", "appreciative", "blessed")
        put(Emotion.EXCITEMENT, "excited", "eager", "thrilled", "buzzing", "elated")
        put(Emotion.JOY, "joyful", "happy", "delighted", "great", "wonderful", "glad", "cheerful")
    }
}
