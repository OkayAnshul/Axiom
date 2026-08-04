package com.cosmiclaboratory.axiom.domain.memory

import com.cosmiclaboratory.axiom.domain.model.MemoryItem
import com.cosmiclaboratory.axiom.domain.model.MemoryKind
import com.cosmiclaboratory.axiom.domain.model.MemorySource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class MemoryConsolidatorTest {

    private val now = LocalDateTime.of(2026, 8, 4, 12, 0)

    private fun memory(
        id: Long,
        text: String,
        kind: MemoryKind = MemoryKind.PERSON,
        timesSeen: Int = 1,
        weight: Float = 0.5f,
        userEdited: Boolean = false,
        ageDays: Long = 0,
        dueAt: LocalDateTime? = null
    ) = MemoryItem(
        id = id, kind = kind, text = text, weight = weight, timesSeen = timesSeen,
        createdAt = now.minusDays(ageDays), lastSeenAt = now.minusDays(ageDays),
        source = MemorySource.CONVERSATION, sourceId = null, userEdited = userEdited, dueAt = dueAt
    )

    // ---- merging ------------------------------------------------------------

    @Test
    fun `differently worded versions of one fact are merged`() {
        val plan = MemoryConsolidator.plan(
            listOf(
                memory(1, "Riya is the user's younger sister living in Pune", timesSeen = 3),
                memory(2, "Riya, younger sister, living in Pune", timesSeen = 2)
            ),
            now
        )
        assertEquals(1, plan.merges.size)
        val merge = plan.merges.first()
        // Evidence adds up rather than being thrown away.
        assertEquals(5, merge.survivor.timesSeen)
        assertEquals(listOf(2L), merge.absorbedIds)
    }

    @Test
    fun `the user's own wording always survives a merge`() {
        val plan = MemoryConsolidator.plan(
            listOf(
                memory(1, "Riya is the user's younger sister in Pune", timesSeen = 9),
                memory(2, "Riya is my sister and she lives in Pune", userEdited = true, timesSeen = 1)
            ),
            now
        )
        val survivor = plan.merges.single().survivor
        assertEquals(2L, survivor.id)
        assertTrue(survivor.userEdited)
        assertEquals(10, survivor.timesSeen)
    }

    @Test
    fun `different kinds are never merged even with identical text`() {
        val plan = MemoryConsolidator.plan(
            listOf(
                memory(1, "Start running again", kind = MemoryKind.GOAL),
                memory(2, "Start running again", kind = MemoryKind.THEME)
            ),
            now
        )
        assertTrue(plan.merges.isEmpty())
    }

    @Test
    fun `distinct facts about the same person are deliberately not merged`() {
        // Merging is the one destructive-ish operation here, so it errs toward
        // leaving duplicates: a wrong merge silently loses a real memory, while
        // a missed one is merely visible and deletable by the user.
        val plan = MemoryConsolidator.plan(
            listOf(
                memory(1, "Riya lives in Pune"),
                memory(2, "Riya works as an architect")
            ),
            now
        )
        assertTrue(plan.merges.isEmpty())
    }

    @Test
    fun `unrelated memories of the same kind are left alone`() {
        val plan = MemoryConsolidator.plan(
            listOf(
                memory(1, "Riya is the younger sister living in Pune"),
                memory(2, "Manager at work is called Devesh and is difficult")
            ),
            now
        )
        assertTrue(plan.merges.isEmpty())
    }

    @Test
    fun `an open loop survives a merge`() {
        val due = now.plusDays(2)
        val plan = MemoryConsolidator.plan(
            listOf(
                memory(1, "Visa interview coming up on Tuesday", kind = MemoryKind.EVENT, timesSeen = 4),
                memory(2, "Interview on Tuesday coming up", kind = MemoryKind.EVENT, dueAt = due)
            ),
            now
        )
        assertEquals(due, plan.merges.single().survivor.dueAt)
    }

    @Test
    fun `three phrasings collapse to one in a single pass`() {
        val plan = MemoryConsolidator.plan(
            listOf(
                memory(1, "Keeps meaning to start running again"),
                memory(2, "Keeps meaning to start running"),
                memory(3, "Meaning to start running again soon")
            ),
            now
        )
        assertEquals(1, plan.merges.size)
        assertEquals(2, plan.merges.single().absorbedIds.size)
    }

    // ---- pruning, which must be timid --------------------------------------

    @Test
    fun `a long-dead never-reinforced memory is pruned`() {
        val plan = MemoryConsolidator.plan(
            listOf(memory(1, "Mentioned a podcast once", weight = 0.3f, ageDays = 400)),
            now
        )
        assertEquals(listOf(1L), plan.pruneIds)
    }

    @Test
    fun `anything the user edited is never pruned`() {
        val plan = MemoryConsolidator.plan(
            listOf(memory(1, "Do not call me buddy", weight = 0.1f, ageDays = 900, userEdited = true)),
            now
        )
        assertTrue(plan.pruneIds.isEmpty())
    }

    @Test
    fun `a reinforced memory is never pruned however old`() {
        val plan = MemoryConsolidator.plan(
            listOf(memory(1, "Sister lives in Pune", timesSeen = 2, weight = 0.2f, ageDays = 900)),
            now
        )
        assertTrue(plan.pruneIds.isEmpty())
    }

    @Test
    fun `an open loop is never pruned`() {
        val plan = MemoryConsolidator.plan(
            listOf(memory(1, "Scan next week", weight = 0.1f, ageDays = 400, dueAt = now.plusDays(1))),
            now
        )
        assertTrue(plan.pruneIds.isEmpty())
    }

    @Test
    fun `recent memories are never pruned`() {
        val plan = MemoryConsolidator.plan(
            listOf(memory(1, "Something small from yesterday", weight = 0.3f, ageDays = 2)),
            now
        )
        assertTrue(plan.pruneIds.isEmpty())
    }

    @Test
    fun `an empty journal produces an empty plan`() {
        assertTrue(MemoryConsolidator.plan(emptyList(), now).isEmpty)
    }

    @Test
    fun `a merge survivor is never also pruned`() {
        val plan = MemoryConsolidator.plan(
            listOf(
                memory(1, "Old note about the balcony garden", weight = 0.2f, ageDays = 400),
                memory(2, "Old note about balcony garden", weight = 0.2f, ageDays = 400)
            ),
            now
        )
        val survivorId = plan.merges.single().survivor.id
        assertTrue(survivorId !in plan.pruneIds)
        assertTrue(plan.merges.single().absorbedIds.none { it in plan.pruneIds })
    }
}
