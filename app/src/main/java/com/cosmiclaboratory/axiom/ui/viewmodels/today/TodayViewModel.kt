package com.cosmiclaboratory.axiom.ui.viewmodels.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmiclaboratory.axiom.data.preferences.UserPreferences
import com.cosmiclaboratory.axiom.data.repository.DailySummaryRepository
import com.cosmiclaboratory.axiom.data.repository.NotesRepository
import com.cosmiclaboratory.axiom.domain.model.Entry
import com.cosmiclaboratory.axiom.domain.streak.StreakCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

data class TodayUiState(
    val displayName: String = "",
    val greeting: String = "Welcome",
    val streak: StreakCalculator.Result = StreakCalculator.Result(0, 0, List(7) { false }),
    val recent: List<Entry> = emptyList(),
    val todayPrompt: String? = null
)

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val entries: NotesRepository,
    private val summaries: DailySummaryRepository,
    private val prefs: UserPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(TodayUiState())
    val state: StateFlow<TodayUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { refresh() }
        viewModelScope.launch {
            entries.getAllNotes().collect { all ->
                _state.value = _state.value.copy(recent = all.take(5))
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val name = prefs.displayName.first()
            val dates = summaries.entryDates()
            _state.value = _state.value.copy(
                displayName = name,
                greeting = greetingFor(LocalTime.now(), name),
                streak = StreakCalculator.compute(dates),
                todayPrompt = TODAY_PROMPTS.random()
            )
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
        // Fallback prompts when AI is unavailable. The DailyPromptWorker fills the
        // AiPromptCacheEntity on devices with a key; this list keeps the surface
        // never empty.
        val TODAY_PROMPTS = listOf(
            "What surprised you this week?",
            "Name one thing you handled better than you would have a year ago.",
            "What's a small win from today that's worth remembering?",
            "What are you avoiding, and what's the smallest first step?",
            "Who or what gave you energy today?",
            "Describe a moment you felt fully present.",
            "What's a pattern you've noticed in yourself lately?"
        )
    }
}
