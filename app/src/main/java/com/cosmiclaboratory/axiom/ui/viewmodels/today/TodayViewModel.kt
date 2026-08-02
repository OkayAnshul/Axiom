package com.cosmiclaboratory.axiom.ui.viewmodels.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmiclaboratory.axiom.data.preferences.UserPreferences
import com.cosmiclaboratory.axiom.data.repository.JournalRepository
import com.cosmiclaboratory.axiom.data.repository.QuestionRepository
import com.cosmiclaboratory.axiom.domain.model.Entry
import com.cosmiclaboratory.axiom.domain.model.EntryKind
import com.cosmiclaboratory.axiom.domain.model.QuestionSource
import com.cosmiclaboratory.axiom.domain.streak.StreakCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

data class TodayUiState(
    val isLoading: Boolean = true,
    val displayName: String = "",
    val greeting: String = "Welcome",
    val streak: StreakCalculator.Result = StreakCalculator.Result(0, 0, List(7) { false }),
    val recent: List<Entry> = emptyList(),
    val todayPrompt: String? = null,
    val todayQuestionId: Long? = null,
    /** True when the prompt came from the AI initiator cache rather than the curated set. */
    val promptIsPersonalised: Boolean = false,
    /** Today's recorded mood, if any. Drives the check-in card. */
    val todayMood: Int? = null,
    /** Most recent unfinished entry, surfaced as "continue writing". */
    val draft: Entry? = null
)

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val entries: JournalRepository,
    private val questions: QuestionRepository,
    private val prefs: UserPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(TodayUiState())
    val state: StateFlow<TodayUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { refresh() }
        viewModelScope.launch {
            entries.observeAll().collect { all ->
                _state.update {
                    it.copy(
                        recent = all.filter { e -> e.isComplete }.take(5),
                        isLoading = false
                    )
                }
                // Streak and today's mood both derive from the corpus, so they
                // must recompute whenever it changes — not only on first load.
                refreshDerived()
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val name = prefs.displayName.first()
            _state.update {
                it.copy(displayName = name, greeting = greetingFor(LocalTime.now(), name))
            }
            loadPrompt()
            refreshDerived()
        }
    }

    private suspend fun refreshDerived() {
        val today = LocalDate.now()
        val todaysEntries = entries.forDay(today)
        _state.update {
            it.copy(
                streak = StreakCalculator.compute(entries.entryDates()),
                todayMood = todaysEntries.firstNotNullOfOrNull { e -> e.mood },
                draft = entries.drafts(1).firstOrNull()
            )
        }
    }

    /**
     * Records a mood for today with a single tap.
     *
     * If nothing has been written today there is no row to attach it to, so we
     * create a content-less entry that exists purely to carry the mood. That is
     * deliberate: most days a user will not write, and without this the pattern
     * engine would only ever see labels from days that were already good enough
     * to write about — a badly biased sample.
     */
    fun recordMood(mood: Int) {
        viewModelScope.launch {
            val today = LocalDate.now()
            val existing = entries.forDay(today).firstOrNull()
            if (existing != null) {
                entries.setMood(existing.id, mood)
            } else {
                val now = LocalDateTime.now()
                entries.upsert(
                    Entry(
                        content = "",
                        kind = EntryKind.FREE_FORM,
                        createdAt = now,
                        updatedAt = now,
                        isComplete = true,
                        mood = mood,
                        moodCapturedAt = now
                    )
                )
            }
            refreshDerived()
        }
    }

    fun shufflePrompt() {
        viewModelScope.launch { loadPrompt(forceNew = true) }
    }

    private suspend fun loadPrompt(forceNew: Boolean = false) {
        val persona = prefs.activePersonaKey.first()
        val question = runCatching { questions.nextQuestion(persona) }.getOrNull()
        if (question != null) {
            _state.update {
                it.copy(
                    todayPrompt = question.text,
                    todayQuestionId = question.id,
                    promptIsPersonalised = question.source != QuestionSource.CURATED
                )
            }
        } else {
            // Never leave the surface empty. The curated set is seeded on first
            // run, so this only fires if seeding has not completed yet.
            _state.update {
                it.copy(
                    todayPrompt = FALLBACK_PROMPTS.random(),
                    todayQuestionId = null,
                    promptIsPersonalised = false
                )
            }
        }
    }

    private fun greetingFor(time: LocalTime, name: String): String {
        val period = when (time.hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..20 -> "Good evening"
            else -> "Hello"
        }
        return if (name.isBlank()) period else "$period, $name"
    }

    private companion object {
        val FALLBACK_PROMPTS = listOf(
            "What surprised you this week?",
            "What's a small win from today that's worth remembering?",
            "What are you avoiding, and what's the smallest first step?",
            "Who or what gave you energy today?",
            "What's a pattern you've noticed in yourself lately?"
        )
    }
}
