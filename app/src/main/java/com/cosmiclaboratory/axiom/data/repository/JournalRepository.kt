package com.cosmiclaboratory.axiom.data.repository

import com.cosmiclaboratory.axiom.data.database.FtsQuerySanitizer
import com.cosmiclaboratory.axiom.data.database.dao.AIInsightDao
import com.cosmiclaboratory.axiom.data.database.dao.EntryDao
import com.cosmiclaboratory.axiom.data.database.dao.TagDao
import com.cosmiclaboratory.axiom.data.database.entity.AIInsightEntity
import com.cosmiclaboratory.axiom.data.database.entity.EntryTagCrossRef
import com.cosmiclaboratory.axiom.data.database.entity.toDomainModel
import com.cosmiclaboratory.axiom.data.database.entity.toEntity
import com.cosmiclaboratory.axiom.domain.model.AIInsight
import com.cosmiclaboratory.axiom.domain.model.Entry
import com.cosmiclaboratory.axiom.domain.model.EntryKind
import com.cosmiclaboratory.axiom.domain.model.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single repository over entries, their tags and their AI insights.
 *
 * Replaces NotesRepository and the old answer-entry half of this class.
 * One corpus means the companion can finally see guided journal answers and the
 * summarizer can finally see free-form notes — both were structurally blind
 * before.
 */
@Singleton
class JournalRepository @Inject constructor(
    private val entryDao: EntryDao,
    private val insightDao: AIInsightDao,
    private val tagDao: TagDao
) {

    // ---- observing ---------------------------------------------------------

    fun observeAll(): Flow<List<Entry>> =
        entryDao.observeAllWithTags().map { rows -> rows.map { it.toDomainModel() } }

    fun observeCompleted(): Flow<List<Entry>> =
        entryDao.observeCompletedWithTags().map { rows -> rows.map { it.toDomainModel() } }

    fun observeFavorites(): Flow<List<Entry>> =
        entryDao.observeFavoritesWithTags().map { rows -> rows.map { it.toDomainModel() } }

    fun observeArchived(): Flow<List<Entry>> =
        entryDao.observeArchivedWithTags().map { rows -> rows.map { it.toDomainModel() } }

    fun observeByKind(kind: EntryKind): Flow<List<Entry>> =
        entryDao.observeByKindWithTags(kind.name).map { rows -> rows.map { it.toDomainModel() } }

    fun observeByTag(tagId: Long): Flow<List<Entry>> =
        entryDao.observeByTagWithTags(tagId).map { rows -> rows.map { it.toDomainModel() } }

    fun observeById(id: Long): Flow<Entry?> =
        entryDao.observeWithTags(id).map { it?.toDomainModel() }

    suspend fun getById(id: Long): Entry? = entryDao.getWithTags(id)?.toDomainModel()

    suspend fun drafts(limit: Int = 5): List<Entry> =
        entryDao.drafts(limit).map { it.toDomainModel() }

    suspend fun forDay(date: LocalDate): List<Entry> =
        entryDao.forDay(date.atStartOfDay(), date.plusDays(1).atStartOfDay())
            .map { it.toDomainModel() }

    suspend fun entryDates(): List<LocalDate> =
        entryDao.distinctEntryDates().mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }

    suspend fun count(): Int = entryDao.count()

    // ---- writing -----------------------------------------------------------

    suspend fun upsert(entry: Entry): Long {
        val now = LocalDateTime.now()
        val row = entry.copy(
            createdAt = if (entry.id == 0L) now else entry.createdAt,
            updatedAt = now
        ).toEntity()
        val id = entryDao.insert(row)
        val entryId = if (entry.id == 0L) id else entry.id
        syncTags(entryId, entry.tags)
        return entryId
    }

    suspend fun delete(entry: Entry) = entryDao.delete(entry.toEntity())

    suspend fun deleteById(id: Long) = entryDao.deleteById(id)

    suspend fun markComplete(id: Long) = entryDao.markComplete(id, LocalDateTime.now())

    suspend fun setMood(id: Long, mood: Int?) =
        entryDao.setMood(id, mood, LocalDateTime.now())

    suspend fun setFavorite(id: Long, favorite: Boolean) =
        entryDao.setFavorite(id, favorite, LocalDateTime.now())

    suspend fun setArchived(id: Long, archived: Boolean) =
        entryDao.setArchived(id, archived, LocalDateTime.now())

    /**
     * Replaces an entry's tags wholesale. The previous implementation only
     * inserted cross-refs and never deleted them, so a removed tag stayed
     * attached forever.
     */
    private suspend fun syncTags(entryId: Long, tags: List<Tag>) {
        entryDao.deleteTagCrossRefsFor(entryId)
        tags.forEach { tag ->
            val tagId = if (tag.id == 0L) tagDao.insertTag(tag.toEntity()) else tag.id
            entryDao.insertTagCrossRef(EntryTagCrossRef(entryId = entryId, tagId = tagId))
        }
    }

    // ---- search ------------------------------------------------------------

    /** Free-text search. Input is sanitized here; callers pass raw user text. */
    suspend fun search(rawQuery: String): List<Entry> {
        val q = FtsQuerySanitizer.forSearch(rawQuery)
        if (q.isBlank()) return emptyList()
        return entryDao.search(q).map { it.toDomainModel() }
    }

    /** Retrieval for the AI companion — stopwords dropped, no prefix globbing. */
    suspend fun searchForRetrieval(rawQuestion: String): List<Entry> {
        val q = FtsQuerySanitizer.forRetrieval(rawQuestion)
        if (q.isBlank()) return emptyList()
        return entryDao.search(q).map { it.toDomainModel() }
    }

    // ---- tags --------------------------------------------------------------

    fun observeAllTags(): Flow<List<Tag>> =
        tagDao.getAllTags().map { rows -> rows.map { it.toDomainModel() } }

    suspend fun insertTag(tag: Tag): Long = tagDao.insertTag(tag.toEntity())

    suspend fun deleteTag(tag: Tag) = tagDao.deleteTag(tag.toEntity())

    // ---- AI insights -------------------------------------------------------

    fun observeInsight(entryId: Long): Flow<AIInsight?> =
        insightDao.observeByEntryId(entryId).map { it?.toDomainModel() }

    suspend fun getInsight(entryId: Long): AIInsight? =
        insightDao.getByEntryId(entryId)?.toDomainModel()

    suspend fun saveInsight(insight: AIInsightEntity): Long = insightDao.insert(insight)

    /** Summaries of recently-summarized entries, used as context for prompt generation. */
    suspend fun recentSummariesForContext(limit: Int = 3): List<String> =
        entryDao.recentSummarized(limit).mapNotNull { insightDao.getByEntryId(it.id)?.summary }
}
