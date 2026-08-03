package com.cosmiclaboratory.axiom.ui.screens.patterns

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmiclaboratory.axiom.data.repository.JournalRepository
import com.cosmiclaboratory.axiom.data.repository.MemoryRepository
import com.cosmiclaboratory.axiom.domain.model.Emotion
import com.cosmiclaboratory.axiom.domain.model.Entry
import com.cosmiclaboratory.axiom.domain.model.MemoryItem
import com.cosmiclaboratory.axiom.domain.model.MemoryKind
import com.cosmiclaboratory.axiom.domain.patterns.Finding
import com.cosmiclaboratory.axiom.domain.patterns.PatternFinder
import com.cosmiclaboratory.axiom.domain.patterns.dominantEmotions
import com.cosmiclaboratory.axiom.domain.streak.StreakCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.roundToInt

/** Window over which every module is computed. */
enum class PatternRange(val label: String, val days: Int?) {
    Days30("30 days", 30),
    Days90("90 days", 90),
    All("All time", null)
}

data class PatternsUiState(
    val isLoading: Boolean = true,
    val range: PatternRange = PatternRange.Days30,
    val entryCount: Int = 0,
    val streak: StreakCalculator.Result = StreakCalculator.Result(0, 0, List(7) { false }),
    val writtenDates: Set<LocalDate> = emptySet(),
    /** One slot per day in the window; null = no mood recorded that day. */
    val moodSeries: List<Float?> = emptyList(),
    val moodDaysRecorded: Int = 0,
    val totalWords: Int = 0,
    val entriesPerWeek: Float = 0f,
    /** Entry counts bucketed by hour of day, 24 slots. */
    val hourHistogram: List<Int> = List(24) { 0 },
    val topThemes: List<Pair<String, Int>> = emptyList(),
    /** Named feelings inferred from the writing, most frequent first. */
    val dominantEmotions: List<Pair<Emotion, Int>> = emptyList(),
    /** How many of the recorded moods came from inference rather than a tap. */
    val inferredMoodCount: Int = 0,
    /** Things the companion noticed, strongest first. */
    val findings: List<Finding> = emptyList()
) {
    /** Below this, statistics are noise rather than pattern. */
    val hasEnoughData: Boolean get() = entryCount >= MIN_ENTRIES

    companion object { const val MIN_ENTRIES = 3 }
}

@HiltViewModel
class PatternsViewModel @Inject constructor(
    private val entries: JournalRepository,
    private val memories: MemoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PatternsUiState())
    val state: StateFlow<PatternsUiState> = _state.asStateFlow()

    /** Latest corpus, held so a range change can recompute without re-querying. */
    private var latest: List<Entry> = emptyList()
    private var latestMemories: List<MemoryItem> = emptyList()

    init {
        viewModelScope.launch {
            entries.observeCompleted().collect { all ->
                latest = all
                recompute(all)
            }
        }
        viewModelScope.launch {
            memories.observeAll().collect { all ->
                latestMemories = all
                recompute(latest)
            }
        }
    }

    fun setRange(range: PatternRange) {
        _state.update { it.copy(range = range) }
        // Recompute from the corpus already in memory. Re-collecting the flow
        // here would spawn a second collector on every chip tap.
        recompute(latest)
    }

    private fun recompute(all: List<Entry>) {
        val range = _state.value.range
        val today = LocalDate.now()
        val cutoff = range.days?.let { today.minusDays(it.toLong() - 1) }
        val windowed = if (cutoff == null) all else all.filter { !it.createdAt.toLocalDate().isBefore(cutoff) }

        val byDate = windowed.groupBy { it.createdAt.toLocalDate() }

        // One slot per day so gaps stay gaps. Averaging away missing days would
        // imply a mood on days the user never recorded one.
        val days = range.days ?: (byDate.keys.minOrNull()?.let {
            java.time.temporal.ChronoUnit.DAYS.between(it, today).toInt() + 1
        } ?: 0)
        val series = (0 until days.coerceAtMost(365)).map { offset ->
            val date = today.minusDays((days - 1 - offset).toLong())
            byDate[date]?.mapNotNull { it.mood }?.takeIf { it.isNotEmpty() }?.average()?.toFloat()
        }

        val weeks = (days / 7f).coerceAtLeast(1f)
        val histogram = MutableList(24) { 0 }
        windowed.forEach { histogram[it.createdAt.hour] += 1 }

        _state.update {
            it.copy(
                isLoading = false,
                entryCount = windowed.size,
                streak = StreakCalculator.compute(all.map { e -> e.createdAt.toLocalDate() }),
                writtenDates = byDate.keys,
                moodSeries = series,
                moodDaysRecorded = series.count { v -> v != null },
                totalWords = windowed.sumOf { e -> e.wordCount },
                entriesPerWeek = (windowed.size / weeks * 10).roundToInt() / 10f,
                hourHistogram = histogram,
                topThemes = windowed
                    .flatMap { e -> e.tags.map { t -> t.name } }
                    .groupingBy { name -> name }
                    .eachCount()
                    .entries
                    .sortedByDescending { entry -> entry.value }
                    .take(6)
                    .map { entry -> entry.key to entry.value },
                dominantEmotions = windowed.dominantEmotions(),
                // moodCapturedAt is null exactly when the feeling was inferred.
                inferredMoodCount = windowed.count { e -> e.mood != null && e.moodCapturedAt == null },
                findings = PatternFinder.find(
                    entries = windowed,
                    memories = latestMemories,
                    personMentions = personMentions(windowed),
                    today = today
                )
            )
        }
    }

    /**
     * Which entries name which person, resolved in memory against the corpus
     * already loaded rather than through FTS: the window is at most a year of a
     * personal journal, and a query per remembered person would be far more
     * expensive than one pass of contains().
     */
    private fun personMentions(windowed: List<Entry>): Map<String, List<Entry>> {
        val names = latestMemories
            .filter { it.kind == MemoryKind.PERSON }
            .mapNotNull { nameFrom(it.text) }
            .distinct()
            .take(MAX_PEOPLE)
        if (names.isEmpty()) return emptyMap()

        return names.associateWith { name ->
            windowed.filter { entry ->
                entry.content.contains(name, ignoreCase = true) ||
                    entry.title.contains(name, ignoreCase = true)
            }
        }.filterValues { it.isNotEmpty() }
    }

    /**
     * Memories read "Riya is the user's younger sister" — the person's name is
     * the leading capitalised word. Words the extractor itself uses are skipped
     * so "The user's manager" does not become a person called "The".
     */
    private fun nameFrom(memoryText: String): String? = memoryText
        .split(NON_NAME)
        .firstOrNull { token ->
            token.length >= MIN_NAME_LENGTH &&
                token.first().isUpperCase() &&
                token.lowercase() !in NON_NAMES
        }

    private companion object {
        const val MAX_PEOPLE = 5
        const val MIN_NAME_LENGTH = 3
        val NON_NAME = Regex("[^\\p{L}]+")
        val NON_NAMES = setOf("the", "their", "they", "user", "his", "her", "and", "has", "was")
    }
}
