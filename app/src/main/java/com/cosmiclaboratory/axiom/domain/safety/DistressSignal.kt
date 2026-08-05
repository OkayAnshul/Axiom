package com.cosmiclaboratory.axiom.domain.safety

/**
 * How much someone is struggling, when they've said so plainly.
 *
 * [Acute] means stated intent to die or to hurt themselves. [Struggling] means
 * real distress without stated intent. Nothing else is a level — this is not a
 * mood classifier and must never be used as one.
 */
enum class CareLevel { Struggling, Acute }

/**
 * On-device detection of plainly-stated distress.
 *
 * Axiom invites emotional disclosure and had no handling of this at all: a user
 * could type the worst sentence of their life and the companion would answer it
 * like a diary prompt. That is the gap this closes.
 *
 * Three deliberate constraints:
 *
 * **No model, no network, no key.** Someone in crisis at 3am with no API key
 * configured is exactly the person who must not hit a dead end. This is plain
 * string matching precisely so it cannot fail.
 *
 * **High precision, lower recall.** It matches only unambiguous first-person
 * phrasing. Missing an oblique hint is a real cost, but wrongly telling someone
 * having a bad Tuesday that you think they might be suicidal is patronising, and
 * it teaches them the app over-reacts — after which they stop telling it things.
 * A companion that cries wolf is worse than one that stays quiet.
 *
 * **It never gates the conversation.** The signal only adds a gentle offer of
 * help and softens the companion's instructions. It does not block sending,
 * change the subject, or refuse to engage.
 */
object DistressSignal {

    fun detect(raw: String): CareLevel? {
        if (raw.isBlank()) return null
        val text = normalise(raw)
        // Strip the phrasings that merely *contain* an alarming substring before
        // matching. "I don't want to die" contains "want to die" and means the
        // opposite of it.
        val cleaned = NOT_DISTRESS.fold(text) { acc, phrase -> acc.replace(phrase, " ") }
        if (cleaned.isBlank()) return null
        if (isAboutSomeoneElse(cleaned)) return null

        if (ACUTE.any { cleaned.contains(it) }) return CareLevel.Acute
        if (STRUGGLING.any { cleaned.contains(it) }) return CareLevel.Struggling
        return null
    }

    /**
     * Lowercase, collapse whitespace, and normalise the apostrophes people
     * actually type — a curly quote must not defeat "don't want to live".
     */
    private fun normalise(raw: String): String = raw
        .lowercase()
        .replace('’', '\'')
        .replace('ʼ', '\'')
        .replace(Regex("\\s+"), " ")
        .trim()

    /**
     * Someone recounting a film, the news, or a worry about a friend is not
     * disclosing their own intent. Crude, and deliberately so: it only fires
     * when the sentence is overtly about a third party.
     */
    private fun isAboutSomeoneElse(text: String): Boolean =
        THIRD_PARTY.any { text.contains(it) } && !text.contains(" i ") && !text.startsWith("i ")

    /** Stated intent. Phrases carry their own first-person framing. */
    private val ACUTE = listOf(
        "kill myself", "killing myself", "end my life", "ending my life",
        "take my own life", "want to die", "wanna die", "want to be dead",
        "don't want to live", "dont want to live", "do not want to live",
        "no reason to live", "nothing to live for", "better off dead",
        "better off without me", "everyone would be better without me",
        "hurt myself", "harm myself", "cut myself", "suicidal", "suicide",
        "end it all",
        // Hindi / Hinglish, romanised and Devanagari
        "khudkushi", "aatmahatya", "atmahatya", "marna chahta", "marna chahti",
        "mar jaun", "jeena nahi chahta", "jeena nahi chahti", "khatam kar du",
        "आत्महत्या", "मरना चाहता", "मरना चाहती", "जीना नहीं चाहता", "जीना नहीं चाहती"
    )

    /** Real distress, no stated intent. */
    private val STRUGGLING = listOf(
        "can't go on", "cant go on", "can't do this anymore", "cant do this anymore",
        "can't take it anymore", "cant take it anymore", "no way out",
        "hopeless", "worthless", "hate myself", "i'm a burden", "im a burden",
        "falling apart", "breaking down", "give up on everything",
        "nothing matters anymore", "completely alone",
        "himmat nahi", "bardaasht nahi", "bardasht nahi", "tut gaya", "tut gayi",
        "हिम्मत नहीं", "बर्दाश्त नहीं"
    )

    /**
     * Phrasings that contain an alarming substring while meaning the opposite,
     * or that are plainly figurative. Removed before matching.
     */
    private val NOT_DISTRESS = listOf(
        "don't want to die", "dont want to die", "do not want to die",
        "not suicidal", "wasn't suicidal", "wasnt suicidal",
        // Everyday hyperbole. Common enough that matching it would train people
        // that the card is noise.
        "dying to", "die laughing", "died laughing", "kill for", "could kill for",
        "killing it", "killed it", "dying for"
    )

    private val THIRD_PARTY = listOf(
        "he said", "she said", "they said", "my friend", "his ", "her ",
        "in the movie", "in the film", "in the book", "on the news", "a character"
    )
}
