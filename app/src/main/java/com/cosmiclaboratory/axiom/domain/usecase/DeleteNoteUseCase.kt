package com.cosmiclaboratory.axiom.domain.usecase

import com.cosmiclaboratory.axiom.data.repository.NotesRepository
import com.cosmiclaboratory.axiom.domain.model.Note
import javax.inject.Inject

class DeleteNoteUseCase @Inject constructor(
    private val notesRepository: NotesRepository
) {
    suspend operator fun invoke(note: Note): Result<Unit> {
        return try {
            notesRepository.deleteNote(note)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend operator fun invoke(noteId: Long): Result<Unit> {
        return try {
            notesRepository.deleteNoteById(noteId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}