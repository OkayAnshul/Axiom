package com.cosmiclaboratory.axiom.domain.memory

/**
 * What counts as a memory worth keeping, in one place.
 *
 * These rules existed only inside the local conversation digester, and only at
 * write time. Rows written before they were added stayed forever, so a live
 * prompt this session genuinely contained:
 *
 * ```
 * Recurring themes:
 * - Keeps coming back to long
 * - Keeps coming back to message
 * ```
 *
 * The model was being told, in the same breath as real facts about a person,
 * that they keep coming back to "message". Applying the same rules on the way
 * *out* as on the way in means no stored row can reach a prompt, whenever it was
 * written and whatever wrote it.
 */
object MemoryHygiene {

    /**
     * Below this a word is a fragment, not a subject. "long" and "sad" say
     * nothing about a life; "money" and "sleep" do.
     */
    const val MIN_THEME_LENGTH = 5

    /**
     * Words that repeat because of how people talk, not because of what they
     * are talking about. A frequency count alone will happily conclude that
     * someone's recurring theme is "thing".
     */
    val NON_THEMES = setOf(
        "thing", "things", "really", "actually", "maybe", "think", "thought",
        "feel", "feeling", "going", "still", "quite", "pretty", "little",
        "message", "number", "today", "yesterday", "tomorrow", "again",
        "something", "anything", "nothing", "everything", "someone", "people"
    )

    /** How a theme memory is phrased, and therefore how its subject is recovered. */
    private const val THEME_PREFIX = "keeps coming back to "

    /**
     * Above this a memory is a transcript, not a memory.
     *
     * The digester once joined every turn together before looking for
     * commitments, so a whole conversation became one "sentence" and was stored
     * verbatim. That was fixed at write time, but a truncated wall of it was
     * still being sent months later:
     *
     * ```
     * Recent events:
     * - Testing the send button Message number 1 to make the thread long Message number 2 to…
     * ```
     *
     * Real memories are one clause about a person. The longest genuine one in a
     * populated database runs to about 55 characters, and the write-time cap is
     * 200, so this floor sits comfortably between them.
     */
    const val MAX_MEMORY_LENGTH = 160

    /**
     * True when a stored memory is not worth sending.
     *
     * Two different failures, in opposite directions. A theme can be too *thin*
     * — a filler word promoted to a life pattern. Anything can be too *long* —
     * a transcript that was never a memory at all.
     *
     * Short facts and events are deliberately not judged: "They cooked dinner."
     * is short because life is short, and second-guessing those would throw
     * away real memories to fix a cosmetic problem.
     */
    fun isDegenerate(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.length > MAX_MEMORY_LENGTH) return true
        val subject = trimmed.lowercase().substringAfter(THEME_PREFIX, missingDelimiterValue = "")
        if (subject.isBlank()) return false
        val word = subject.trim().trimEnd('.').trim()
        return word.length < MIN_THEME_LENGTH || word in NON_THEMES
    }
}
