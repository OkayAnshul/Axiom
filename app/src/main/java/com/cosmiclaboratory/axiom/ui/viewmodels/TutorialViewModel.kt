package com.cosmiclaboratory.axiom.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmiclaboratory.axiom.domain.model.LessonDifficulty
import com.cosmiclaboratory.axiom.domain.model.TutorialLesson
import com.cosmiclaboratory.axiom.domain.model.TutorialLessons
import com.cosmiclaboratory.axiom.domain.model.TutorialStep
import com.cosmiclaboratory.axiom.utils.MarkdownParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TutorialUiState(
    val availableLessons: Map<LessonDifficulty, List<TutorialLesson>> = emptyMap(),
    val currentLesson: TutorialLesson? = null,
    val currentStep: TutorialStep? = null,
    val currentStepIndex: Int = 0,
    val userInput: String = "",
    val isCurrentStepCorrect: Boolean? = null,
    val showHint: Boolean = false,
    val lessonProgress: Float = 0f,
    val completedLessons: Set<String> = emptySet(),
    val isLoading: Boolean = false
)

@HiltViewModel
class TutorialViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(TutorialUiState())
    val uiState: StateFlow<TutorialUiState> = _uiState.asStateFlow()

    init {
        loadLessons()
    }

    private fun loadLessons() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                availableLessons = TutorialLessons.getAllLessons(),
                isLoading = false
            )
        }
    }

    fun startLesson(lesson: TutorialLesson) {
        _uiState.value = _uiState.value.copy(
            currentLesson = lesson,
            currentStep = lesson.steps.firstOrNull(),
            currentStepIndex = 0,
            userInput = "",
            isCurrentStepCorrect = null,
            showHint = false,
            lessonProgress = 0f
        )
    }

    fun exitLesson() {
        _uiState.value = _uiState.value.copy(
            currentLesson = null,
            currentStep = null,
            currentStepIndex = 0,
            userInput = "",
            isCurrentStepCorrect = null,
            showHint = false,
            lessonProgress = 0f
        )
    }

    fun updateUserInput(input: String) {
        _uiState.value = _uiState.value.copy(
            userInput = input,
            isCurrentStepCorrect = null // Reset feedback when user types
        )
    }

    fun checkCurrentStep() {
        val currentStep = _uiState.value.currentStep ?: return
        val userInput = _uiState.value.userInput.trim()
        
        val isCorrect = checkAnswer(userInput, currentStep)
        
        _uiState.value = _uiState.value.copy(
            isCurrentStepCorrect = isCorrect
        )
        
        if (isCorrect) {
            // Mark step as completed and update progress
            updateStepCompletion(true)
        }
    }

    private fun checkAnswer(userInput: String, step: TutorialStep): Boolean {
        // Parse both user input and expected output with markdown parser
        val userElements = MarkdownParser.parse(userInput)
        val expectedElements = MarkdownParser.parse(step.example)
        
        // For basic comparison, we'll check if the rendered output matches expectations
        // This is a simplified version - you could make this more sophisticated
        
        return when {
            // For basic text formatting
            step.id.contains("bold") -> userInput.contains("**") && userInput.replace("**", "").trim().isNotEmpty()
            step.id.contains("italic") -> (userInput.contains("*") && !userInput.contains("**")) || userInput.contains("_")
            step.id.contains("strikethrough") -> userInput.contains("~~")
            step.id.contains("highlight") -> userInput.contains("==")
            step.id.contains("underline") -> userInput.contains("__")
            step.id.contains("code") && step.id.contains("inline") -> userInput.contains("`") && !userInput.contains("```")
            step.id.contains("code") && step.id.contains("block") -> userInput.contains("```")
            
            // For headers
            step.id.startsWith("h") -> userInput.startsWith("#")
            
            // For lists
            step.id.contains("bullet") -> userInput.contains("-") || userInput.contains("*") || userInput.contains("+")
            step.id.contains("numbered") -> userInput.matches(Regex(".*\\d+\\..*"))
            step.id.contains("task") || step.id.contains("checkbox") -> userInput.contains("[ ]") || userInput.contains("[x]")
            
            // For links and images
            step.id.contains("link") && !step.id.contains("image") -> userInput.contains("[") && userInput.contains("](")
            step.id.contains("image") -> userInput.startsWith("![") && userInput.contains("](")
            
            // For tables
            step.id.contains("table") -> userInput.contains("|") && userInput.contains("---")
            
            // For quotes
            step.id.contains("quote") -> userInput.contains(">")
            
            // For horizontal rules
            step.id.contains("rule") -> userInput.contains("---") || userInput.contains("***") || userInput.contains("___")
            
            // Default: check if user input contains key elements from the example
            else -> {
                val exampleKeywords = extractKeywords(step.example)
                val userKeywords = extractKeywords(userInput)
                exampleKeywords.any { keyword -> userKeywords.contains(keyword) }
            }
        }
    }

    private fun extractKeywords(text: String): Set<String> {
        return text.lowercase()
            .replace(Regex("[^a-zA-Z0-9\\s]"), " ")
            .split("\\s+".toRegex())
            .filter { it.length > 2 }
            .toSet()
    }

    private fun updateStepCompletion(isCompleted: Boolean) {
        val currentLesson = _uiState.value.currentLesson ?: return
        val currentStepIndex = _uiState.value.currentStepIndex
        
        val updatedSteps = currentLesson.steps.mapIndexed { index, step ->
            if (index == currentStepIndex) {
                step.copy(isCompleted = isCompleted)
            } else {
                step
            }
        }
        
        val updatedLesson = currentLesson.copy(steps = updatedSteps)
        val completedSteps = updatedSteps.count { it.isCompleted }
        val progress = completedSteps.toFloat() / updatedSteps.size
        
        _uiState.value = _uiState.value.copy(
            currentLesson = updatedLesson,
            lessonProgress = progress
        )
    }

    fun nextStep() {
        val currentLesson = _uiState.value.currentLesson ?: return
        val nextIndex = _uiState.value.currentStepIndex + 1
        
        if (nextIndex < currentLesson.steps.size) {
            _uiState.value = _uiState.value.copy(
                currentStepIndex = nextIndex,
                currentStep = currentLesson.steps[nextIndex],
                userInput = "",
                isCurrentStepCorrect = null,
                showHint = false
            )
        }
    }

    fun previousStep() {
        val currentLesson = _uiState.value.currentLesson ?: return
        val prevIndex = _uiState.value.currentStepIndex - 1
        
        if (prevIndex >= 0) {
            _uiState.value = _uiState.value.copy(
                currentStepIndex = prevIndex,
                currentStep = currentLesson.steps[prevIndex],
                userInput = "",
                isCurrentStepCorrect = null,
                showHint = false
            )
        }
    }

    fun showHint() {
        _uiState.value = _uiState.value.copy(showHint = true)
    }

    fun completeLesson() {
        val currentLesson = _uiState.value.currentLesson ?: return
        
        val updatedCompletedLessons = _uiState.value.completedLessons + currentLesson.id
        val updatedLesson = currentLesson.copy(isCompleted = true)
        
        // Update the lesson in the available lessons map
        val updatedLessons = _uiState.value.availableLessons.mapValues { (_, lessons) ->
            lessons.map { lesson ->
                if (lesson.id == currentLesson.id) {
                    updatedLesson
                } else {
                    lesson
                }
            }
        }
        
        _uiState.value = _uiState.value.copy(
            completedLessons = updatedCompletedLessons,
            availableLessons = updatedLessons,
            lessonProgress = 1f
        )
        
        // Exit lesson after completion
        exitLesson()
    }
    
    fun resetProgress() {
        _uiState.value = _uiState.value.copy(
            completedLessons = emptySet(),
            availableLessons = TutorialLessons.getAllLessons()
        )
    }
}