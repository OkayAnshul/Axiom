package com.cosmiclaboratory.axiom.data.repository

import com.cosmiclaboratory.axiom.data.database.dao.AiPromptCacheDao
import com.cosmiclaboratory.axiom.data.database.dao.QuestionDao
import com.cosmiclaboratory.axiom.data.database.entity.AiPromptCacheEntity
import com.cosmiclaboratory.axiom.data.database.entity.QuestionEntity
import com.cosmiclaboratory.axiom.domain.model.PersonaKey
import com.cosmiclaboratory.axiom.domain.model.QuestionSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/**
 * Covers the batch draw behind the swipeable prompt.
 *
 * The bug this exists to keep fixed is not in this class: the daily opener used
 * to persist a question's *text* and drop its id, so `entries.questionId` stayed
 * null, the LEFT JOIN that decides "already answered" never matched, and the
 * fifty curated prompts recycled forever. Offering five at a time makes that
 * repetition impossible to miss, so the batch has to carry ids and must not
 * spend anything to produce alternatives.
 */
class QuestionRepositoryTest {

    @Test
    fun `alternatives never repeat the question being offered`() = runTest {
        val dao = FakeQuestionDao(
            curated = (1L..10L).map { curated(it, "curated $it") }
        )
        val repo = QuestionRepository(dao, FakeCacheDao())

        val drawn = repo.nextQuestions(PersonaKey.CALM, count = 5)

        assertEquals(5, drawn.size)
        assertEquals(
            "the same question was offered twice in one batch",
            drawn.size,
            drawn.map { it.id }.distinct().size
        )
    }

    @Test
    fun `every offered prompt carries its id so the answer can be credited`() = runTest {
        val dao = FakeQuestionDao(curated = (1L..10L).map { curated(it, "curated $it") })
        val repo = QuestionRepository(dao, FakeCacheDao())

        val drawn = repo.nextQuestions(PersonaKey.CALM, count = 4)

        assertTrue(drawn.isNotEmpty())
        assertTrue(
            "a prompt with no id can never be marked answered",
            drawn.all { it.id != 0L }
        )
    }

    @Test
    fun `drawing alternatives does not consume generated prompts`() = runTest {
        val cache = FakeCacheDao(
            unconsumed = mutableListOf(
                cached(1L, "generated one"),
                cached(2L, "generated two"),
                cached(3L, "generated three")
            )
        )
        val dao = FakeQuestionDao(curated = (10L..20L).map { curated(it, "curated $it") })
        val repo = QuestionRepository(dao, cache)

        repo.nextQuestions(PersonaKey.CALM, count = 5)

        // Exactly one initiator is promoted — the one actually shown. Drawing the
        // alternatives through nextQuestion() would have burned four prompts the
        // reader never saw.
        assertEquals(
            "alternatives spent generated prompts that were never shown",
            1,
            cache.consumedIds.size
        )
    }

    @Test
    fun `a bank with nothing left still yields no duplicates`() = runTest {
        val dao = FakeQuestionDao(curated = listOf(curated(1L, "the only one")))
        val repo = QuestionRepository(dao, FakeCacheDao())

        val drawn = repo.nextQuestions(PersonaKey.CALM, count = 5)

        assertEquals(1, drawn.size)
        assertEquals("the only one", drawn.single().text)
    }

    @Test
    fun `asking for one falls back to the single-question path`() = runTest {
        val dao = FakeQuestionDao(curated = (1L..5L).map { curated(it, "curated $it") })
        val repo = QuestionRepository(dao, FakeCacheDao())

        val drawn = repo.nextQuestions(PersonaKey.CALM, count = 1)

        assertEquals(1, drawn.size)
        assertNotNull(drawn.single())
        assertTrue(
            "count = 1 should not touch the alternatives query at all",
            !dao.alternativesQueried
        )
    }

    private fun curated(id: Long, text: String) = QuestionEntity(
        id = id,
        packId = null,
        theme = null,
        text = text,
        source = QuestionSource.CURATED.name,
        parentAnswerId = null,
        createdAt = LocalDateTime.now()
    )

    private fun cached(id: Long, text: String) = AiPromptCacheEntity(
        id = id,
        batchId = "batch",
        text = text,
        personaKey = PersonaKey.CALM.storageValue,
        generatedAt = LocalDateTime.now(),
        consumed = false
    )
}

/**
 * Stands in for Room. The queries modelled here are the ones the repository
 * actually leans on; the rest throw so a future caller cannot silently rely on
 * behaviour this fake never simulated.
 */
private class FakeQuestionDao(
    private val curated: List<QuestionEntity> = emptyList(),
    private val followUp: QuestionEntity? = null,
    /** Ids treated as already answered recently. */
    private val answered: Set<Long> = emptySet()
) : QuestionDao {

    var alternativesQueried = false
        private set

    private val inserted = mutableMapOf<Long, QuestionEntity>()
    private var nextId = 1000L

    override suspend fun countBySource(source: String): Int =
        curated.count { it.source == source }

    override suspend fun insertAll(questions: List<QuestionEntity>): List<Long> =
        questions.map { insert(it) }

    override suspend fun insert(question: QuestionEntity): Long {
        val id = if (question.id != 0L) question.id else nextId++
        inserted[id] = question.copy(id = id)
        return id
    }

    override suspend fun getById(id: Long): QuestionEntity? =
        inserted[id] ?: curated.firstOrNull { it.id == id }

    override suspend fun pendingFollowUp(): QuestionEntity? = followUp

    override suspend fun randomCuratedNotRecent(
        theme: String?,
        cutoffIso: String
    ): QuestionEntity? = curated.firstOrNull { it.id !in answered }

    override suspend fun randomCuratedUnanswered(): QuestionEntity? =
        curated.firstOrNull { it.id !in answered }

    override suspend fun curatedAlternatives(
        limit: Int,
        excludeIds: List<Long>,
        cutoffIso: String
    ): List<QuestionEntity> {
        alternativesQueried = true
        return curated
            .filter { it.id !in excludeIds && it.id !in answered }
            .take(limit)
    }
}

private class FakeCacheDao(
    private val unconsumed: MutableList<AiPromptCacheEntity> = mutableListOf()
) : AiPromptCacheDao {

    val consumedIds = mutableListOf<Long>()

    override fun observeUnconsumedFor(personaKey: String): Flow<List<AiPromptCacheEntity>> =
        flowOf(unconsumed.toList())

    override suspend fun nextUnconsumed(personaKey: String): AiPromptCacheEntity? =
        unconsumed.firstOrNull { it.id !in consumedIds }

    override suspend fun unconsumedCount(personaKey: String): Int =
        unconsumed.count { it.id !in consumedIds }

    override suspend fun insertAll(prompts: List<AiPromptCacheEntity>): List<Long> =
        prompts.map { it.id }

    override suspend fun markConsumed(id: Long) {
        consumedIds += id
    }

    override suspend fun pruneOlderThan(personaKey: String, cutoffIso: String) = Unit
}
