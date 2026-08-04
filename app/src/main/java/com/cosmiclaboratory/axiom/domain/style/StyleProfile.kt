package com.cosmiclaboratory.axiom.domain.style

import com.cosmiclaboratory.axiom.domain.model.MemoryItem
import java.time.LocalDateTime
import com.cosmiclaboratory.axiom.domain.text.TextTokens

/** How long the companion's replies should run to match this person. */
enum class ReplyLength(val instruction: String) {
    BRIEF("They write in short bursts. One or two sentences back, never a paragraph."),
    MODERATE("They write a few sentences at a time. Two to four sentences back."),
    EXPANSIVE("They write at length. You can follow them there, but stay under six sentences.")
}

/** Which language the companion should answer in, mirroring how they write. */
enum class LanguageMix(val instruction: String) {
    ENGLISH("They write in English. Answer in English."),
    HINDI("They write in Hindi. Answer in Hindi, in Devanagari script."),
    HINGLISH("They mix Hindi and English the way people actually text. Mix it back, naturally — do not translate them.")
}

/**
 * How to talk to this particular person — as distinct from what to remember
 * about them.
 *
 * Length and language are measured from what they actually write rather than
 * guessed by a model: they are objective, free, need no key, and cannot
 * hallucinate. [statedPreferences] carries the things only they can tell us,
 * whether they said it in conversation or typed it into "What I remember".
 */
data class StyleProfile(
    val replyLength: ReplyLength = ReplyLength.MODERATE,
    val languageMix: LanguageMix = LanguageMix.ENGLISH,
    /** Preferences in their own words, strongest first. */
    val statedPreferences: List<String> = emptyList(),
    /** Set once there is enough writing to measure; until then the defaults are only assumptions. */
    val measured: Boolean = false
)

/**
 * Derives a [StyleProfile] from the user's own messages. Pure, so the
 * thresholds are testable and the whole thing runs with no key and no network.
 */
object StyleProfiler {

    /** Below this many messages, one long vent would set the tone forever. */
    const val MIN_MESSAGES = 4

    /** Median word counts, chosen from how people actually text versus journal. */
    const val BRIEF_MAX_WORDS = 12
    const val MODERATE_MAX_WORDS = 45

    /** Devanagari anywhere is decisive; below this share of Latin it stops being a mix. */
    private const val MIN_LATIN_SHARE_FOR_MIX = 0.15

    fun profile(
        userMessages: List<String>,
        preferences: List<MemoryItem> = emptyList(),
        now: LocalDateTime = LocalDateTime.now()
    ): StyleProfile {
        val stated = preferences
            .sortedByDescending { if (it.userEdited) 1 else 0 }
            .map { it.text }

        val usable = userMessages.filter { it.isNotBlank() }
        if (usable.size < MIN_MESSAGES) {
            return StyleProfile(statedPreferences = stated, measured = false)
        }

        return StyleProfile(
            replyLength = replyLengthFor(usable),
            languageMix = languageMixFor(usable),
            statedPreferences = stated,
            measured = true
        )
    }

    /**
     * Median rather than mean: a single furious 400-word message should not
     * convince the companion that this person wants essays.
     */
    internal fun replyLengthFor(messages: List<String>): ReplyLength {
        val counts = messages.map { message -> message.trim().split(WHITESPACE).count { it.isNotBlank() } }
            .sorted()
        val median = counts[counts.size / 2]
        return when {
            median <= BRIEF_MAX_WORDS -> ReplyLength.BRIEF
            median <= MODERATE_MAX_WORDS -> ReplyLength.MODERATE
            else -> ReplyLength.EXPANSIVE
        }
    }

    /**
     * Script beats vocabulary: Devanagari present means Hindi is in play, and
     * whether it is Hindi or Hinglish depends on how much Latin sits beside it.
     * Romanised Hinglish has no Devanagari at all, so it is caught by a small
     * marker list instead — these are function words, which survive topic
     * changes in a way that content words do not.
     */
    internal fun languageMixFor(messages: List<String>): LanguageMix {
        val text = messages.joinToString(" ")
        val devanagari = text.count { it in DEVANAGARI_RANGE }
        val latin = text.count { it in 'a'..'z' || it in 'A'..'Z' }

        if (devanagari > 0) {
            val latinShare = latin.toDouble() / (latin + devanagari).coerceAtLeast(1)
            return if (latinShare >= MIN_LATIN_SHARE_FOR_MIX) LanguageMix.HINGLISH else LanguageMix.HINDI
        }

        val words = TextTokens.words(text)
        if (words.isEmpty()) return LanguageMix.ENGLISH
        val markers = words.count { it in ROMAN_HINDI_MARKERS }
        return if (markers >= 2 && markers.toDouble() / words.size >= 0.04) {
            LanguageMix.HINGLISH
        } else {
            LanguageMix.ENGLISH
        }
    }

    private val DEVANAGARI_RANGE = 'ऀ'..'ॿ'
    private val WHITESPACE = Regex("\\s+")

    /**
     * Romanised Hindi function words. Every entry must NOT be an English word:
     * "the" and "mere" are both romanisations of Hindi words and ordinary
     * English ones, so including them turned any English sentence with two
     * "the"s into Hinglish. Homographs are excluded even when they cost recall.
     */
    private val ROMAN_HINDI_MARKERS = setOf(
        "hai", "hain", "tha", "thi", "nahi", "nahin", "kya", "kyun", "kyu",
        "mera", "meri", "tera", "teri", "aur", "lekin", "phir", "bahut",
        "thoda", "matlab", "yaar", "acha", "accha", "theek", "raha", "rahi", "rahe",
        "karna", "karta", "karti", "hona", "hoga", "hogi", "abhi", "aaj", "kal"
    )
}
