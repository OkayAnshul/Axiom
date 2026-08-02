package com.cosmiclaboratory.axiom.data.repository

import com.cosmiclaboratory.axiom.data.database.dao.NoteDao
import com.cosmiclaboratory.axiom.data.database.dao.TagDao
import com.cosmiclaboratory.axiom.data.database.entity.*
import com.cosmiclaboratory.axiom.domain.model.Note
import com.cosmiclaboratory.axiom.domain.model.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotesRepository @Inject constructor(
    private val noteDao: NoteDao,
    private val tagDao: TagDao
) {
    
    fun getAllNotes(): Flow<List<Note>> {
        return noteDao.getAllNotes().map { entities ->
            entities.map { entity ->
                val tags = noteDao.getTagsForNote(entity.id).map { it.toDomainModel() }
                entity.toDomainModel(tags)
            }
        }
    }
    
    suspend fun getNoteById(noteId: Long): Note? {
        return noteDao.getNoteById(noteId)?.let { entity ->
            val tags = noteDao.getTagsForNote(entity.id).map { it.toDomainModel() }
            entity.toDomainModel(tags)
        }
    }
    
    fun getFavoriteNotes(): Flow<List<Note>> {
        return noteDao.getFavoriteNotes().map { entities ->
            entities.map { entity ->
                val tags = noteDao.getTagsForNote(entity.id).map { it.toDomainModel() }
                entity.toDomainModel(tags)
            }
        }
    }
    
    fun getArchivedNotes(): Flow<List<Note>> {
        return noteDao.getArchivedNotes().map { entities ->
            entities.map { entity ->
                val tags = noteDao.getTagsForNote(entity.id).map { it.toDomainModel() }
                entity.toDomainModel(tags)
            }
        }
    }
    
    suspend fun insertNote(note: Note): Long {
        val updatedNote = note.copy(
            createdAt = if (note.id == 0L) LocalDateTime.now() else note.createdAt,
            updatedAt = LocalDateTime.now()
        )
        val noteId = noteDao.insertNote(updatedNote.toEntity())
        
        // Insert tag relationships
        note.tags.forEach { tag ->
            val tagId = if (tag.id == 0L) {
                tagDao.insertTag(tag.toEntity())
            } else {
                tag.id
            }
            noteDao.insertNoteTagCrossRef(NoteTagCrossRef(noteId, tagId))
        }
        
        return noteId
    }
    
    suspend fun updateNote(note: Note) {
        val updatedNote = note.copy(updatedAt = LocalDateTime.now())
        noteDao.updateNote(updatedNote.toEntity())
        
        // Update tag relationships - simple approach: delete all and re-insert
        // In production, you might want a more sophisticated approach
        note.tags.forEach { tag ->
            val tagId = if (tag.id == 0L) {
                tagDao.insertTag(tag.toEntity())
            } else {
                tag.id
            }
            noteDao.insertNoteTagCrossRef(NoteTagCrossRef(note.id, tagId))
        }
    }
    
    suspend fun deleteNote(note: Note) {
        noteDao.deleteNote(note.toEntity())
    }
    
    suspend fun deleteNoteById(noteId: Long) {
        noteDao.deleteNoteById(noteId)
    }
    
    suspend fun searchNotes(query: String): List<Note> {
        return noteDao.searchNotes(query).map { entity ->
            val tags = noteDao.getTagsForNote(entity.id).map { it.toDomainModel() }
            entity.toDomainModel(tags)
        }
    }
    
    fun getNotesByTag(tagId: Long): Flow<List<Note>> {
        return noteDao.getNotesByTag(tagId).map { entities ->
            entities.map { entity ->
                val tags = noteDao.getTagsForNote(entity.id).map { it.toDomainModel() }
                entity.toDomainModel(tags)
            }
        }
    }
    
    // Tag operations
    fun getAllTags(): Flow<List<Tag>> {
        return tagDao.getAllTags().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    suspend fun insertTag(tag: Tag): Long {
        return tagDao.insertTag(tag.toEntity())
    }
    
    suspend fun deleteTag(tag: Tag) {
        tagDao.deleteTag(tag.toEntity())
    }
}