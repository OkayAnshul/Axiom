package com.cosmiclaboratory.axiom.demo

import com.cosmiclaboratory.axiom.data.database.dao.AIInsightDao
import com.cosmiclaboratory.axiom.data.database.dao.CompanionDao
import com.cosmiclaboratory.axiom.data.database.dao.CompanionThreadStateDao
import com.cosmiclaboratory.axiom.data.database.dao.EntryDao
import com.cosmiclaboratory.axiom.data.database.dao.MemoryItemDao
import com.cosmiclaboratory.axiom.data.database.dao.TagDao
import com.cosmiclaboratory.axiom.data.database.dao.UserProfileDao
import com.cosmiclaboratory.axiom.data.database.entity.AIInsightEntity
import com.cosmiclaboratory.axiom.data.database.entity.CompanionMessageEntity
import com.cosmiclaboratory.axiom.data.database.entity.CompanionThreadStateEntity
import com.cosmiclaboratory.axiom.data.database.entity.EntryEntity
import com.cosmiclaboratory.axiom.data.database.entity.EntryTagCrossRef
import com.cosmiclaboratory.axiom.data.database.entity.MemoryItemEntity
import com.cosmiclaboratory.axiom.data.database.entity.TagEntity
import com.cosmiclaboratory.axiom.data.database.entity.UserProfileEntity
import com.cosmiclaboratory.axiom.data.preferences.UserPreferences
import com.cosmiclaboratory.axiom.domain.model.PersonaKey
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fills the database with a coherent two months of journalling, for screenshots.
 *
 * Debug builds only — this file lives in `src/debug`, and the receiver that
 * calls it is declared in `src/debug/AndroidManifest.xml`, so neither the code
 * nor the prose in [DemoContent] is merged into a release build. Nothing in
 * `src/main` knows this class exists.
 *
 * Two things it is careful about.
 *
 * **The data is shaped backwards from [com.cosmiclaboratory.axiom.domain.patterns.PatternFinder].**
 * That engine deliberately stays silent below its thresholds, so plausible-looking
 * random data produces a blank Patterns tab — the one screen most likely to
 * embarrass a store listing. Dates are anchored to weekdays rather than to
 * "N days ago" so the Monday-is-harder finding holds whichever day this is run
 * on; see [DemoEntry.dateFor] and the arithmetic noted on [DemoContent.ENTRIES].
 *
 * **Seed while the app is open.** `CompanionViewModel.startFresh` parks the
 * whole conversation on launch, by design — arriving should not feel like
 * walking back into a room mid-sentence. Messages inserted *after* launch carry
 * ids above the park watermark, so they appear live and the companion screen has
 * something in it. Seed first and then relaunch, and the conversation correctly
 * hides itself again.
 *
 * The persona is invented. None of this is anyone's real journal.
 */
@Singleton
class DemoDataSeeder @Inject constructor(
    private val entryDao: EntryDao,
    private val tagDao: TagDao,
    private val memoryDao: MemoryItemDao,
    private val companionDao: CompanionDao,
    private val threadStateDao: CompanionThreadStateDao,
    private val insightDao: AIInsightDao,
    private val profileDao: UserProfileDao,
    private val prefs: UserPreferences
) {
    private val now: LocalDateTime = LocalDateTime.now()
    private val today: LocalDate = LocalDate.now()

    suspend fun seed() {
        clear()
        seedProfile()
        val tagIds = seedTags()
        val entryIds = seedEntries(tagIds)
        seedInsights(entryIds)
        seedMemories()
        seedConversation(entryIds)
    }

    /** Wipes what [seed] writes, so re-running gives identical screenshots. */
    suspend fun clear() {
        companionDao.clear()
        memoryDao.getAll().forEach { memoryDao.deleteById(it.id) }
        // ai_insights and entry_tag_cross_ref both cascade from entries.
        entryDao.allForBackup().forEach { entryDao.deleteById(it.id) }
        tagDao.allForBackup().forEach { tagDao.deleteTag(it) }
    }

    // ---- who the demo user is ----------------------------------------------

    private suspend fun seedProfile() {
        profileDao.upsert(
            UserProfileEntity(
                id = 0,
                displayName = DemoContent.USER_NAME,
                createdAt = now.minusDays(64),
                onboardingComplete = true,
                activePersonaKey = PersonaKey.CALM.storageValue
            )
        )
        prefs.setDisplayName(DemoContent.USER_NAME)
        prefs.setOnboardingComplete(true)
        // Keep the first-run coach marks out of the screenshots.
        prefs.setTalkHintSeen(true)
        prefs.setPromptSwipeHintSeen(true)
    }

    // ---- tags ---------------------------------------------------------------

    private suspend fun seedTags(): Map<String, Long> =
        DemoContent.TAGS.associate { (name, colour) ->
            name to tagDao.insertTag(
                TagEntity(name = name, color = colour, createdAt = now.minusDays(60))
            )
        }

    // ---- entries ------------------------------------------------------------

    private suspend fun seedEntries(tagIds: Map<String, Long>): List<Long> =
        DemoContent.ENTRIES.map { demo ->
            val createdAt = demo.dateFor(today).atTime(demo.hour, demo.minute)
            val id = entryDao.insert(
                EntryEntity(
                    title = demo.title,
                    content = demo.body,
                    markdown = demo.body,
                    kind = demo.kind.name,
                    createdAt = createdAt,
                    updatedAt = createdAt,
                    isFavorite = demo.favourite,
                    isComplete = true,
                    mood = demo.mood,
                    // Non-null means the user chose this mood rather than the
                    // classifier inferring it, which is what a demo should show.
                    moodCapturedAt = createdAt,
                    emotion = demo.emotion.name,
                    promptSnapshot = demo.prompt,
                    wordCount = demo.body.split(WHITESPACE).count { it.isNotBlank() },
                    charCount = demo.body.length
                )
            )
            demo.tags.forEach { tag ->
                tagIds[tag]?.let { entryDao.insertTagCrossRef(EntryTagCrossRef(id, it)) }
            }
            id
        }

    private suspend fun seedInsights(entryIds: List<Long>) {
        DemoContent.INSIGHTS.forEach { insight ->
            val entryId = entryIds.getOrNull(insight.entryIndex) ?: return@forEach
            insightDao.insert(
                AIInsightEntity(
                    entryId = entryId,
                    summary = insight.summary,
                    followUpQuestionText = insight.followUp,
                    themesCsv = insight.themes,
                    mood = insight.mood,
                    modelName = MODEL_NAME,
                    totalTokens = insight.tokens,
                    createdAt = now.minusDays(insight.entryIndex.toLong())
                )
            )
        }
    }

    // ---- what the companion remembers ---------------------------------------

    private suspend fun seedMemories() {
        DemoContent.MEMORIES.forEach { memory ->
            memoryDao.upsert(
                MemoryItemEntity(
                    kind = memory.kind.name,
                    text = memory.text,
                    weight = memory.weight,
                    timesSeen = memory.timesSeen,
                    createdAt = now.minusDays(memory.firstSeenDaysAgo.toLong()),
                    lastSeenAt = now.minusDays(memory.lastSeenDaysAgo.toLong()),
                    sourceType = memory.source.name,
                    userEdited = memory.userEdited,
                    // A negative value is already overdue, so the loop shows now.
                    dueAt = memory.dueInDays?.let { now.plusDays(it.toLong()) }
                )
            )
        }
    }

    // ---- the conversation ---------------------------------------------------

    private suspend fun seedConversation(entryIds: List<Long>) {
        // The citation chips need real ids, and the entries cited below are the
        // ones the reply is actually talking about.
        val cited = DemoContent.CITED_ENTRY_INDICES
            .mapNotNull { entryIds.getOrNull(it) }
            .joinToString(",")

        DemoContent.CONVERSATION.forEachIndexed { index, turn ->
            companionDao.insert(
                CompanionMessageEntity(
                    threadId = THREAD_ID,
                    role = turn.role.name,
                    content = turn.text,
                    // Spaced a few minutes apart so it reads as one sitting.
                    createdAt = now.minusMinutes((DemoContent.CONVERSATION.size - index) * 3L),
                    citedEntryIdsCsv = if (turn.cites) cited else "",
                    source = turn.source.name
                )
            )
        }

        // Reset the watermarks the previous launch left behind. Without this a
        // re-seed inherits the old park position and the screen carries a "pick
        // up where we left off" chip counting messages that no longer exist —
        // ids keep climbing across a wipe because the primary key is
        // AUTOINCREMENT, so the stale watermark is never re-reached.
        threadStateDao.upsert(
            CompanionThreadStateEntity(threadId = THREAD_ID, updatedAt = now)
        )
    }

    private companion object {
        const val THREAD_ID = "companion"
        const val MODEL_NAME = "openai/gpt-oss-120b"
        val WHITESPACE = Regex("\\s+")
    }
}
