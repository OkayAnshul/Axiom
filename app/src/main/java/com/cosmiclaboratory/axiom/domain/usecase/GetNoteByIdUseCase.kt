package com.cosmiclaboratory.axiom.domain.usecase

import com.cosmiclaboratory.axiom.data.repository.NotesRepository
import com.cosmiclaboratory.axiom.domain.model.Note
import javax.inject.Inject

class GetNoteByIdUseCase @Inject constructor(
    private val notesRepository: NotesRepository
) {
    suspend operator fun invoke(noteId: Long): Note? {
        return notesRepository.getNoteById(noteId)
    }
}