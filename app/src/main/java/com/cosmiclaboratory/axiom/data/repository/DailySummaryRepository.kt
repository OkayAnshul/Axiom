package com.cosmiclaboratory.axiom.data.repository

import com.cosmiclaboratory.axiom.data.database.dao.DailySummaryDao
import com.cosmiclaboratory.axiom.data.database.dao.EntryDao
import com.cosmiclaboratory.axiom.data.database.entity.DailySummaryEntity
import com.cosmiclaboratory.axiom.domain.model.EntryKind
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailySummaryRepository @Inject constructor(
    private val dao: DailySummaryDao,
    private val entryDao: EntryDao
) {
    fun observeAll(): Flow<List<DailySummaryEntity>> = dao.observeAll()

    fun observeRange(start: LocalDate, end: LocalDate): Flow<List<DailySummaryEntity>> =
        dao.observeRange(start.toString(), end.toString())

    /** Recompute and persist the summary for [date] from the underlying notes table. */
    suspend fun rebuildFor(date: LocalDate) {
        val start = date.atStartOfDay()
        val end = date.plusDays(1).atStartOfDay()
        val entries = entryDao.forDay(start, end).filter { !it.isArchived }
        if (entries.isEmpty()) {
            dao.upsert(DailySummaryEntity(date.toString(), 0, null, 0))
            return
        }
        val moods = entries.mapNotNull { it.mood }
        val dominant = moods.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
        val wordCount = entries.sumOf { it.content.split(Regex("\\s+")).count { w -> w.isNotBlank() } }
        dao.upsert(
            DailySummaryEntity(
                dateIso = date.toString(),
                entryCount = entries.size,
                dominantMood = dominant,
                wordCount = wordCount
            )
        )
    }

    suspend fun rebuildToday() {
        rebuildFor(LocalDate.now())
    }

    /** All distinct dates with at least one non-archived entry. */
    suspend fun entryDates(): List<LocalDate> {
        return entryDao.distinctEntryDates().mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
    }
}
