package com.cosmiclaboratory.axiom.domain.usecase

import com.cosmiclaboratory.axiom.data.repository.NotesRepository
import com.cosmiclaboratory.axiom.domain.model.Note
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetNotesUseCase @Inject constructor(
    private val notesRepository: NotesRepository
) {
    operator fun invoke(): Flow<List<Note>> {
        return notesRepository.getAllNotes()
    }
}