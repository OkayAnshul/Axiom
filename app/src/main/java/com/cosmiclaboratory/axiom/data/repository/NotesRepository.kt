package com.cosmiclaboratory.axiom.data.repository

import com.cosmiclaboratory.axiom.data.database.dao.EntryDao
import com.cosmiclaboratory.axiom.data.database.dao.TagDao
import com.cosmiclaboratory.axiom.data.database.entity.*
import com.cosmiclaboratory.axiom.domain.model.Entry
import com.cosmiclaboratory.axiom.domain.model.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotesRepository @Inject constructor(
    private val entryDao: EntryDao,
    private val tagDao: TagDao
) {
    
    fun getAllNotes(): Flow<List<Entry>> {
        return entryDao.getAllNotes().map { entities ->
            entities.map { entity ->
                val tags = entryDao.getTagsForNote(entity.id).map { it.toDomainModel() }
                entity.toDomainModel(tags)
            }
        }
    }
    
    suspend fun getNoteById(noteId: Long): Entry? {
        return entryDao.getNoteById(noteId)?.let { entity ->
            val tags = entryDao.getTagsForNote(entity.id).map { it.toDomainModel() }
            entity.toDomainModel(tags)
        }
    }
    
    fun getFavoriteNotes(): Flow<List<Entry>> {
        return entryDao.getFavoriteNotes().map { entities ->
            entities.map { entity ->
                val tags = entryDao.getTagsForNote(entity.id).map { it.toDomainModel() }
                entity.toDomainModel(tags)
            }
        }
    }
    
    fun getArchivedNotes(): Flow<List<Entry>> {
        return entryDao.getArchivedNotes().map { entities ->
            entities.map { entity ->
                val tags = entryDao.getTagsForNote(entity.id).map { it.toDomainModel() }
                entity.toDomainModel(tags)
            }
        }
    }
    
    suspend fun insertNote(note: Entry): Long {
        val updatedNote = note.copy(
            createdAt = if (note.id == 0L) LocalDateTime.now() else note.createdAt,
            updatedAt = LocalDateTime.now()
        )
        val noteId = entryDao.insertNote(updatedNote.toEntity())
        
        // Insert tag relationships
        note.tags.forEach { tag ->
            val tagId = if (tag.id == 0L) {
                tagDao.insertTag(tag.toEntity())
            } else {
                tag.id
            }
            entryDao.insertNoteTagCrossRef(EntryTagCrossRef(noteId, tagId))
        }
        
        return noteId
    }
    
    suspend fun updateNote(note: Entry) {
        val updatedNote = note.copy(updatedAt = LocalDateTime.now())
        entryDao.updateNote(updatedNote.toEntity())
        
        // Update tag relationships - simple approach: delete all and re-insert
        // In production, you might want a more sophisticated approach
        note.tags.forEach { tag ->
            val tagId = if (tag.id == 0L) {
                tagDao.insertTag(tag.toEntity())
            } else {
                tag.id
            }
            entryDao.insertNoteTagCrossRef(EntryTagCrossRef(note.id, tagId))
        }
    }
    
    suspend fun deleteNote(note: Entry) {
        entryDao.deleteNote(note.toEntity())
    }
    
    suspend fun deleteNoteById(noteId: Long) {
        entryDao.deleteNoteById(noteId)
    }
    
    suspend fun searchNotes(query: String): List<Entry> {
        return entryDao.searchNotes(query).map { entity ->
            val tags = entryDao.getTagsForNote(entity.id).map { it.toDomainModel() }
            entity.toDomainModel(tags)
        }
    }
    
    fun getNotesByTag(tagId: Long): Flow<List<Entry>> {
        return entryDao.getNotesByTag(tagId).map { entities ->
            entities.map { entity ->
                val tags = entryDao.getTagsForNote(entity.id).map { it.toDomainModel() }
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