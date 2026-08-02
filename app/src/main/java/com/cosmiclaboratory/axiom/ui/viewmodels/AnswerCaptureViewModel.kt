package com.cosmiclaboratory.axiom.ui.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmiclaboratory.axiom.data.database.dao.QuestionDao
import com.cosmiclaboratory.axiom.data.database.entity.toDomainModel
import com.cosmiclaboratory.axiom.domain.model.Question
import com.cosmiclaboratory.axiom.domain.usecase.journal.CompleteAnswerUseCase
import com.cosmiclaboratory.axiom.domain.usecase.journal.SaveAnswerUseCase
import com.cosmiclaboratory.axiom.ui.navigation.AxiomScreen
import com.cosmiclaboratory.axiom.utils.PlainTextToMarkdownConverter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AnswerCaptureUiState(
    val question: Question? = null,
    val content: String = "",
    val isSaving: Boolean = false,
    val isCompleting: Boolean = false,
    val savedEntryId: Long? = null,
    val completedEntryId: Long? = null
)

@HiltViewModel
class AnswerCaptureViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val questionDao: QuestionDao,
    private val saveAnswer: SaveAnswerUseCase,
    private val completeAnswer: CompleteAnswerUseCase
) : ViewModel() {

    private val questionId: Long = savedStateHandle[AxiomScreen.AnswerCapture.QUESTION_ID_ARG] ?: -1L
    private var autosaveJob: Job? = null
    private val startedAt = System.currentTimeMillis()

    private val _state = MutableStateFlow(AnswerCaptureUiState())
    val state: StateFlow<AnswerCaptureUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val q = questionDao.getById(questionId)?.toDomainModel()
            _state.update { it.copy(question = q) }
        }
    }

    fun setContent(text: String) {
        _state.update { it.copy(content = text) }
        scheduleAutosave()
    }

    fun appendVoiceTranscript(text: String) {
        if (text.isBlank()) return
        val current = _state.value.content
        val joined = if (current.isBlank()) text else "$current $text"
        _state.update { it.copy(content = joined) }
        scheduleAutosave()
    }

    fun complete(onCompleted: (Long) -> Unit) {
        if (_state.value.isCompleting) return
        viewModelScope.launch {
            autosaveJob?.cancel()
            _state.update { it.copy(isCompleting = true) }
            val id = persist()
            completeAnswer(id)
            _state.update { it.copy(isCompleting = false, completedEntryId = id) }
            onCompleted(id)
        }
    }

    private fun scheduleAutosave() {
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch {
            delay(1000)
            if (_state.value.content.isBlank()) return@launch
            _state.update { it.copy(isSaving = true) }
            val id = persist()
            _state.update { it.copy(isSaving = false, savedEntryId = id) }
        }
    }

    private suspend fun persist(): Long {
        val text = _state.value.content
        val q = _state.value.question
        val markdown = PlainTextToMarkdownConverter.convert(
            plainText = text,
            title = q?.text
        )
        return saveAnswer(
            existingId = _state.value.savedEntryId,
            questionId = q?.id,
            questionTextSnapshot = q?.text.orEmpty(),
            markdown = markdown,
            plainText = text,
            durationMs = System.currentTimeMillis() - startedAt
        )
    }
}
