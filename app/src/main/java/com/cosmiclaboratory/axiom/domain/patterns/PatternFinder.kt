package com.cosmiclaboratory.axiom.domain.patterns

import com.cosmiclaboratory.axiom.domain.model.Emotion
import com.cosmiclaboratory.axiom.domain.model.Entry
import com.cosmiclaboratory.axiom.domain.model.MemoryItem
import com.cosmiclaboratory.axiom.domain.model.MemoryKind
import com.cosmiclaboratory.axiom.domain.model.humanizedMemory
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

/**
 * Something the companion has noticed, phrased the way a friend would say it.
 *
 * [text] is deliberately the finished sentence rather than a bag of numbers:
 * every consumer — the Patterns tab, the companion's prompt — wants the same
 * gentle phrasing, and generating it once keeps a clinical version from
 * leaking into the conversation. [confidence] drives ordering, not wording.
 */
data class Finding(
    val kind: Kind,
    val text: String,
    val confidence: Float
) {
    enum class Kind { DAY_OF_WEEK, MOOD_TREND, RECURRING, PERSON }
}

/**
 * Finds patterns worth mentioning, entirely on this device: no key, no
 * network, no model. Every rule has a minimum-evidence gate, because the
 * failure mode here is not missing a pattern — it is confidently telling
 * someone a made-up fact about their own life.
 */
object PatternFinder {

    /** Below this many observations a "pattern" is a coincidence. */
    const val MIN_DAY_SAMPLES = 3
    const val MIN_TREND_DAYS = 3
    const val MIN_RECURRENCE = 3
    const val MIN_PERSON_MENTIONS = 3

    /** How much a day or a week has to differ before it is worth saying aloud. */
    private const val MEANINGFUL_MOOD_GAP = 0.6

    fun find(
        entries: List<Entry>,
        memories: List<MemoryItem>,
        personMentions: Map<String, List<Entry>> = emptyMap(),
        today: LocalDate = LocalDate.now()
    ): List<Finding> = buildList {
        hardestDayOfWeek(entries)?.let { add(it) }
        moodTrend(entries, today)?.let { add(it) }
        addAll(recurringThemes(memories))
        addAll(personCorrelations(personMentions, entries))
    }.sortedByDescending { it.confidence }

    /**
     * "Mondays are usually harder." Compares each weekday's mean mood against
     * the mean of all other days, so a uniformly low stretch does not make
     * every day look bad.
     */
    internal fun hardestDayOfWeek(entries: List<Entry>): Finding? {
        val byDay = entries.mapNotNull { entry ->
            entry.mood?.let { entry.createdAt.dayOfWeek to it }
        }.groupBy({ it.first }, { it.second })

        val eligible = byDay.filterValues { it.size >= MIN_DAY_SAMPLES }
        if (eligible.size < 2) return null

        val worst = eligible.minByOrNull { it.value.average() } ?: return null
        val others = eligible.filterKeys { it != worst.key }.values.flatten()
        if (others.isEmpty()) return null

        val gap = others.average() - worst.value.average()
        if (gap < MEANINGFUL_MOOD_GAP) return null

        val day = worst.key.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        return Finding(
            kind = Finding.Kind.DAY_OF_WEEK,
            text = "${day}s tend to be harder than the rest of your week.",
            confidence = confidenceFrom(worst.value.size, gap)
        )
    }

    /**
     * "You've sounded lighter this week." Last seven days against the seven
     * before, and only when both windows have enough recorded days to compare.
     */
    internal fun moodTrend(entries: List<Entry>, today: LocalDate): Finding? {
        fun window(fromDaysAgo: Int, toDaysAgo: Int): List<Int> = entries.mapNotNull { entry ->
            val date = entry.createdAt.toLocalDate()
            val age = java.time.temporal.ChronoUnit.DAYS.between(date, today).toInt()
            entry.mood?.takeIf { age in toDaysAgo..fromDaysAgo }
        }

        val recent = window(6, 0)
        val previous = window(13, 7)
        if (recent.size < MIN_TREND_DAYS || previous.size < MIN_TREND_DAYS) return null

        val delta = recent.average() - previous.average()
        if (abs(delta) < MEANINGFUL_MOOD_GAP) return null

        val text = if (delta > 0) {
            "You've sounded lighter this week than last."
        } else {
            "This week has been heavier for you than last week."
        }
        return Finding(
            kind = Finding.Kind.MOOD_TREND,
            text = text,
            confidence = confidenceFrom(recent.size + previous.size, abs(delta))
        )
    }

    /**
     * "You keep coming back to X." Reads the companion's own memory rather than
     * re-analysing text: a memory reinforced many times is by definition
     * something the user keeps raising.
     *
     * Only themes and goals can recur. A stable fact — where someone lives, who
     * their sister is — gets reinforced just as often without ever being a
     * pattern, and reporting it as one both states the obvious and duplicates
     * whatever the person-level finding already said.
     */
    internal fun recurringThemes(memories: List<MemoryItem>): List<Finding> =
        memories.filter { it.kind in RECURRABLE && it.timesSeen >= MIN_RECURRENCE }
            .sortedByDescending { it.timesSeen }
            .take(2)
            .map { memory ->
                Finding(
                    kind = Finding.Kind.RECURRING,
                    text = "This keeps coming up: ${memory.text.humanizedMemory().trimEnd('.')} " +
                        "(${memory.timesSeen} times).",
                    confidence = confidenceFrom(memory.timesSeen, 1.0)
                )
            }

    private val RECURRABLE = setOf(MemoryKind.THEME, MemoryKind.GOAL)

    /**
     * "You sound better on days you mention your sister." [personMentions] maps
     * a person's name to the entries naming them — resolved by the caller,
     * which owns search. Only reports when the difference is real and the
     * person comes up often enough to be a pattern rather than an anecdote.
     */
    internal fun personCorrelations(
        personMentions: Map<String, List<Entry>>,
        allEntries: List<Entry>
    ): List<Finding> {
        val overall = allEntries.mapNotNull { it.mood }
        if (overall.size < MIN_PERSON_MENTIONS) return emptyList()
        val overallMean = overall.average()

        return personMentions.mapNotNull { (name, mentioned) ->
            val moods = mentioned.mapNotNull { it.mood }
            if (moods.size < MIN_PERSON_MENTIONS) return@mapNotNull null
            val delta = moods.average() - overallMean
            if (abs(delta) < MEANINGFUL_MOOD_GAP) return@mapNotNull null

            val text = if (delta > 0) {
                "Your days tend to read brighter when $name comes up."
            } else {
                "$name comes up most on your harder days."
            }
            Finding(Finding.Kind.PERSON, text, confidenceFrom(moods.size, abs(delta)))
        }.sortedByDescending { it.confidence }.take(2)
    }

    /**
     * More evidence and a wider gap both raise confidence, and neither alone
     * can max it out — six observations of a tiny difference should not
     * outrank three observations of an obvious one.
     */
    private fun confidenceFrom(samples: Int, magnitude: Double): Float {
        val evidence = (samples / 10f).coerceAtMost(1f)
        val strength = (magnitude / 2.0).coerceAtMost(1.0).toFloat()
        return (0.4f * evidence + 0.6f * strength).coerceIn(0f, 1f)
    }
}

/** Emotion words most often seen in a set of entries, for a plain-language summary. */
fun List<Entry>.dominantEmotions(limit: Int = 3): List<Pair<Emotion, Int>> =
    mapNotNull { it.emotion }
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedByDescending { it.value }
        .take(limit)
        .map { it.key to it.value }

/** Weekday label used by both the finder and the Patterns UI. */
fun DayOfWeek.shortLabel(locale: Locale = Locale.ENGLISH): String =
    getDisplayName(TextStyle.SHORT, locale)
