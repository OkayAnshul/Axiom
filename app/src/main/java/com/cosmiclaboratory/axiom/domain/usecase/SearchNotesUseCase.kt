package com.cosmiclaboratory.axiom.domain.usecase

import com.cosmiclaboratory.axiom.data.repository.NotesRepository
import com.cosmiclaboratory.axiom.domain.model.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class SearchNotesUseCase @Inject constructor(
    private val notesRepository: NotesRepository
) {
    suspend operator fun invoke(query: String): List<Note> {
        return if (query.isBlank()) {
            emptyList()
        } else {
            notesRepository.searchNotes(query.trim())
        }
    }
    
    suspend fun searchWithHistory(query: String, recentSearches: List<String>): List<Note> {
        return if (query.isBlank()) {
            // Could return notes based on recent searches or popular notes
            emptyList()
        } else {
            notesRepository.searchNotes(query.trim())
        }
    }
}