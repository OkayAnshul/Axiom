package com.cosmiclaboratory.axiom.ui.screens.talks

import com.cosmiclaboratory.axiom.data.database.entity.CompanionMessageEntity
import com.cosmiclaboratory.axiom.domain.model.MemoryItem
import com.cosmiclaboratory.axiom.domain.model.MemoryKind
import com.cosmiclaboratory.axiom.domain.model.MemorySource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * The grouping and attribution rules behind "Our talks".
 *
 * The interesting cases are all about *not* claiming more than the data
 * supports: a memory the AI digest filed under a whole session must not appear
 * as a mark on one arbitrary turn.
 */
class TalksViewModelTest {

    private val vm = TalksViewModel.Companion

    private fun message(
        id: Long,
        text: String,
        at: LocalDateTime,
        role: CompanionMessageEntity.Role = CompanionMessageEntity.Role.USER
    ) = CompanionMessageEntity(
        id = id,
        threadId = "companion",
        role = role.name,
        content = text,
        createdAt = at
    )

    private fun memory(
        id: Long,
        text: String,
        sourceId: Long?,
        dueAt: LocalDateTime? = null
    ) = MemoryItem(
        id = id,
        kind = if (dueAt != null) MemoryKind.EVENT else MemoryKind.THEME,
        text = text,
        weight = 0.5f,
        timesSeen = 1,
        createdAt = LocalDateTime.of(2026, 8, 1, 9, 0),
        lastSeenAt = LocalDateTime.of(2026, 8, 1, 9, 0),
        source = MemorySource.CONVERSATION,
        sourceId = sourceId,
        userEdited = false,
        dueAt = dueAt
    )

    private val monday = LocalDate.of(2026, 8, 3)
    private val tuesday = LocalDate.of(2026, 8, 4)

    // ---- grouping ----------------------------------------------------------

    @Test
    fun `messages are grouped by day, newest day first`() {
        val days = vm.buildDays(
            messages = listOf(
                message(1, "monday morning", monday.atTime(9, 0)),
                message(2, "monday night", monday.atTime(22, 0)),
                message(3, "tuesday", tuesday.atTime(10, 0))
            ),
            fromConversation = emptyList(),
            conversationEntryDates = emptySet()
        )

        assertEquals(listOf(tuesday, monday), days.map { it.date })
        assertEquals(2, days.first { it.date == monday }.messageCount)
    }

    @Test
    fun `the opening is the first thing the user said, not the companion`() {
        val days = vm.buildDays(
            messages = listOf(
                message(1, "Morning — how did it go?", monday.atTime(9, 0),
                    CompanionMessageEntity.Role.ASSISTANT),
                message(2, "Badly, honestly", monday.atTime(9, 5))
            ),
            fromConversation = emptyList(),
            conversationEntryDates = emptySet()
        )

        assertEquals("Badly, honestly", days.single().opening)
    }

    @Test
    fun `memories and open loops are counted against the day they came from`() {
        val days = vm.buildDays(
            messages = listOf(
                message(1, "monday", monday.atTime(9, 0)),
                message(2, "tuesday", tuesday.atTime(9, 0))
            ),
            fromConversation = listOf(
                memory(10, "Keeps coming back to the move", sourceId = 1),
                memory(11, "My scan is on Friday", sourceId = 1,
                    dueAt = LocalDateTime.of(2026, 8, 7, 9, 0)),
                memory(12, "Keeps coming back to work", sourceId = 2)
            ),
            conversationEntryDates = setOf(monday)
        )

        val mon = days.first { it.date == monday }
        assertEquals(1, mon.memoriesFormed)
        assertEquals(1, mon.loopsOpened)
        assertTrue(mon.entryWritten)

        val tue = days.first { it.date == tuesday }
        assertEquals(1, tue.memoriesFormed)
        assertEquals(0, tue.loopsOpened)
        assertFalse(tue.entryWritten)
    }

    @Test
    fun `a memory whose source message is gone is not counted anywhere`() {
        val days = vm.buildDays(
            messages = listOf(message(1, "monday", monday.atTime(9, 0))),
            fromConversation = listOf(memory(10, "an orphan", sourceId = 999)),
            conversationEntryDates = emptySet()
        )

        assertEquals(0, days.single().memoriesFormed)
    }

    // ---- landmarks ---------------------------------------------------------

    @Test
    fun `a turn containing a memory verbatim is a landmark`() {
        val messages = listOf(
            message(1, "Nothing much happened", monday.atTime(9, 0)),
            message(2, "Anyway, my scan is on Friday and I'm dreading it", monday.atTime(9, 5))
        )
        val landmarks = vm.landmarksIn(
            messages,
            listOf(memory(10, "my scan is on Friday", sourceId = 2))
        )

        assertEquals(setOf(2L), landmarks)
    }

    @Test
    fun `a paraphrased memory marks nothing`() {
        // What the AI digest produces: a summary in its own words. There is no
        // turn we can honestly point at, so we point at none.
        val messages = listOf(
            message(1, "work has been relentless since the reorg", monday.atTime(9, 0))
        )
        val landmarks = vm.landmarksIn(
            messages,
            listOf(memory(10, "Feeling worn down by their job", sourceId = 1))
        )

        assertTrue(landmarks.toString(), landmarks.isEmpty())
    }

    @Test
    fun `sourceId alone never creates a landmark`() {
        // The AI path files every memory in a batch under the batch's LAST
        // message. Trusting that pointer would mark turn 3 as the origin of
        // something that was said in turn 1.
        val messages = listOf(
            message(1, "I told Priya about the flat", monday.atTime(9, 0)),
            message(2, "she was pleased", monday.atTime(9, 1)),
            message(3, "ok goodnight", monday.atTime(9, 2))
        )
        val landmarks = vm.landmarksIn(
            messages,
            listOf(memory(10, "Priya is their closest friend", sourceId = 3))
        )

        assertFalse(landmarks.toString(), landmarks.contains(3L))
    }

    @Test
    fun `a memory too short to be distinctive marks nothing`() {
        val messages = listOf(message(1, "the move is soon", monday.atTime(9, 0)))
        val landmarks = vm.landmarksIn(messages, listOf(memory(10, "the move", sourceId = 1)))

        assertTrue(landmarks.toString(), landmarks.isEmpty())
    }

    @Test
    fun `companion turns are never landmarks`() {
        // The companion quoting the user back at them is not the user saying it.
        val messages = listOf(
            message(1, "my scan is on Friday", monday.atTime(9, 0)),
            message(2, "You said my scan is on Friday — how are you feeling about it?",
                monday.atTime(9, 1), CompanionMessageEntity.Role.ASSISTANT)
        )
        val landmarks = vm.landmarksIn(
            messages,
            listOf(memory(10, "my scan is on Friday", sourceId = 1))
        )

        assertEquals(setOf(1L), landmarks)
    }
}
