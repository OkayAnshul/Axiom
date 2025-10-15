package com.cosmiclaboratory.axiom.domain.usecase

import com.cosmiclaboratory.axiom.data.repository.NotesRepository
import com.cosmiclaboratory.axiom.domain.model.Note
import java.time.LocalDateTime
import javax.inject.Inject

class SaveNoteUseCase @Inject constructor(
    private val notesRepository: NotesRepository
) {
    suspend operator fun invoke(note: Note): Result<Long> {
        return try {
            val noteToSave = if (note.id == 0L) {
                // New note
                note.copy(
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now(),
                    version = 1
                )
            } else {
                // Existing note - update timestamp and version
                note.copy(
                    updatedAt = LocalDateTime.now(),
                    version = note.version + 1
                )
            }
            
            // Validate note before saving
            when {
                noteToSave.title.isBlank() && noteToSave.content.isBlank() -> {
                    Result.failure(IllegalArgumentException("Note cannot be empty"))
                }
                noteToSave.title.length > 200 -> {
                    Result.failure(IllegalArgumentException("Note title too long (max 200 characters)"))
                }
                noteToSave.content.length > 50000 -> {
                    Result.failure(IllegalArgumentException("Note content too long (max 50,000 characters)"))
                }
                else -> {
                    val noteId = notesRepository.insertNote(noteToSave)
                    Result.success(noteId)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}