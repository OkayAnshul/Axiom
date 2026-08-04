package com.cosmiclaboratory.axiom.data.backup

import com.cosmiclaboratory.axiom.data.database.dao.AIInsightDao
import com.cosmiclaboratory.axiom.data.database.dao.CompanionDao
import com.cosmiclaboratory.axiom.data.database.dao.EntryDao
import com.cosmiclaboratory.axiom.data.database.dao.MemoryItemDao
import com.cosmiclaboratory.axiom.data.database.dao.TagDao
import com.cosmiclaboratory.axiom.data.database.entity.AIInsightEntity
import com.cosmiclaboratory.axiom.data.database.entity.CompanionMessageEntity
import com.cosmiclaboratory.axiom.data.database.entity.EntryEntity
import com.cosmiclaboratory.axiom.data.database.entity.EntryTagCrossRef
import com.cosmiclaboratory.axiom.data.database.entity.MemoryItemEntity
import com.cosmiclaboratory.axiom.data.database.entity.TagEntity
import com.cosmiclaboratory.axiom.data.preferences.UserPreferences
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/** Why a restore could not proceed, in terms the user can act on. */
sealed interface RestoreFailure {
    data object NotABackup : RestoreFailure
    data object NeedsPassphrase : RestoreFailure
    data object WrongPassphrase : RestoreFailure
    data class Corrupt(val cause: Throwable) : RestoreFailure
}

sealed interface RestoreResult {
    data class Success(val summary: RestoreSummary) : RestoreResult
    data class Failed(val reason: RestoreFailure) : RestoreResult
}

/**
 * Whole-journal export and restore.
 *
 * Restore MERGES rather than replaces. Replacing is easier to write and one
 * mistaken tap away from destroying the thing the feature exists to protect;
 * merging means restoring the same file twice is harmless, and a backup can be
 * pulled into a journal that has since moved on. Identity is content-based
 * because row ids are meaningless across devices: an entry is "the same entry"
 * when it was created at the same moment with the same words.
 */
@Singleton
class BackupManager @Inject constructor(
    private val entryDao: EntryDao,
    private val tagDao: TagDao,
    private val insightDao: AIInsightDao,
    private val memoryDao: MemoryItemDao,
    private val companionDao: CompanionDao,
    private val prefs: UserPreferences,
    private val json: Json
) {

    private val pretty = Json(from = json) { prettyPrint = true }

    /**
     * Serialises everything. With a passphrase the payload is sealed; without
     * one the file is readable JSON, which is the more portable choice and the
     * user's to make — the UI says which they are getting.
     */
    suspend fun export(passphrase: CharArray?): String {
        val backup = BackupFile(
            exportedAt = LocalDateTime.now().toString(),
            displayName = prefs.displayName.first(),
            entries = entryDao.allForBackup().map { it.toBackup() },
            tags = tagDao.allForBackup().map { BackupTag(it.id, it.name, it.color, it.createdAt.toString()) },
            entryTags = entryDao.allTagCrossRefs().map { BackupEntryTag(it.entryId, it.tagId) },
            insights = insightDao.allForBackup().map { it.toBackup() },
            memories = memoryDao.getAll().map { it.toBackup() },
            companionMessages = companionDao.allForBackup().map { it.toBackup() }
        )
        val plaintext = pretty.encodeToString(BackupFile.serializer(), backup)
        if (passphrase == null || passphrase.isEmpty()) return plaintext
        return pretty.encodeToString(
            EncryptedBackup.serializer(),
            BackupCipher.encrypt(plaintext, passphrase)
        )
    }

    suspend fun restore(fileContents: String, passphrase: CharArray?): RestoreResult {
        val plaintext = when {
            fileContents.contains(EncryptedBackup.FORMAT) -> {
                if (passphrase == null || passphrase.isEmpty()) {
                    return RestoreResult.Failed(RestoreFailure.NeedsPassphrase)
                }
                val envelope = runCatching {
                    json.decodeFromString(EncryptedBackup.serializer(), fileContents)
                }.getOrElse { return RestoreResult.Failed(RestoreFailure.Corrupt(it)) }
                BackupCipher.decrypt(envelope, passphrase)
                    ?: return RestoreResult.Failed(RestoreFailure.WrongPassphrase)
            }
            fileContents.contains(BackupFile.FORMAT) -> fileContents
            else -> return RestoreResult.Failed(RestoreFailure.NotABackup)
        }

        val backup = runCatching { json.decodeFromString(BackupFile.serializer(), plaintext) }
            .getOrElse { return RestoreResult.Failed(RestoreFailure.Corrupt(it)) }

        return runCatching { RestoreResult.Success(merge(backup)) }
            .getOrElse { RestoreResult.Failed(RestoreFailure.Corrupt(it)) }
    }

    private suspend fun merge(backup: BackupFile): RestoreSummary {
        var entriesAdded = 0
        var entriesSkipped = 0
        var tagsAdded = 0
        var memoriesAdded = 0
        var messagesAdded = 0

        // Tags first: entries reference them, and names are the natural identity.
        val tagIdMap = mutableMapOf<Long, Long>()
        val existingTags = tagDao.allForBackup().associateBy { it.name.lowercase() }
        backup.tags.forEach { tag ->
            val existing = existingTags[tag.name.lowercase()]
            tagIdMap[tag.id] = existing?.id ?: run {
                tagsAdded++
                tagDao.insertTag(
                    TagEntity(name = tag.name, color = tag.color, createdAt = parseTime(tag.createdAt))
                )
            }
        }

        val entryIdMap = mutableMapOf<Long, Long>()
        val existingEntries = entryDao.allForBackup().associateBy { entryIdentity(it.createdAt.toString(), it.content) }
        backup.entries.forEach { entry ->
            val identity = entryIdentity(entry.createdAt, entry.content)
            val existing = existingEntries[identity]
            if (existing != null) {
                entriesSkipped++
                entryIdMap[entry.id] = existing.id
            } else {
                entriesAdded++
                entryIdMap[entry.id] = entryDao.insert(entry.toEntity())
            }
        }

        // Relationships are rebuilt against the NEW ids, never the file's.
        backup.entryTags.forEach { link ->
            val entryId = entryIdMap[link.entryId] ?: return@forEach
            val tagId = tagIdMap[link.tagId] ?: return@forEach
            entryDao.insertTagCrossRef(EntryTagCrossRef(entryId = entryId, tagId = tagId))
        }

        val entriesWithInsights = insightDao.allForBackup().map { it.entryId }.toSet()
        backup.insights.forEach { insight ->
            val entryId = entryIdMap[insight.entryId] ?: return@forEach
            if (entryId in entriesWithInsights) return@forEach
            insightDao.insert(insight.toEntity(entryId))
        }

        val existingMemories = memoryDao.getAll().map { memoryIdentity(it.kind, it.text) }.toSet()
        backup.memories.forEach { memory ->
            if (memoryIdentity(memory.kind, memory.text) in existingMemories) return@forEach
            memoriesAdded++
            memoryDao.upsert(memory.toEntity())
        }

        val existingMessages = companionDao.allForBackup()
            .map { messageIdentity(it.threadId, it.createdAt.toString(), it.content) }
            .toSet()
        backup.companionMessages.forEach { message ->
            if (messageIdentity(message.threadId, message.createdAt, message.content) in existingMessages) {
                return@forEach
            }
            messagesAdded++
            companionDao.insert(message.toEntity())
        }

        return RestoreSummary(entriesAdded, entriesSkipped, memoriesAdded, messagesAdded, tagsAdded)
    }
}

// ---- identity ---------------------------------------------------------------

/**
 * Two entries are the same when they were written at the same moment with the
 * same words. Titles and moods are excluded on purpose: an entry edited after
 * the backup was taken is still that entry, and re-importing should not clone
 * it.
 */
internal fun entryIdentity(createdAt: String, content: String): String =
    "$createdAt|${content.trim()}"

internal fun memoryIdentity(kind: String, text: String): String =
    "${kind.uppercase()}|${text.trim().lowercase()}"

internal fun messageIdentity(threadId: String, createdAt: String, content: String): String =
    "$threadId|$createdAt|${content.trim()}"

// ---- mapping ----------------------------------------------------------------

private fun parseTime(raw: String): LocalDateTime =
    runCatching { LocalDateTime.parse(raw) }.getOrDefault(LocalDateTime.now())

private fun parseTimeOrNull(raw: String?): LocalDateTime? =
    raw?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }

private fun EntryEntity.toBackup() = BackupEntry(
    id = id, title = title, content = content, markdown = markdown, kind = kind,
    createdAt = createdAt.toString(), updatedAt = updatedAt.toString(),
    isFavorite = isFavorite, isArchived = isArchived, isComplete = isComplete,
    mood = mood, moodCapturedAt = moodCapturedAt?.toString(), emotion = emotion,
    energy = energy, promptSnapshot = promptSnapshot, durationMs = durationMs, sourceLang = sourceLang
)

private fun BackupEntry.toEntity() = EntryEntity(
    title = title, content = content, markdown = markdown, kind = kind,
    createdAt = parseTime(createdAt), updatedAt = parseTime(updatedAt),
    isFavorite = isFavorite, isArchived = isArchived, isComplete = isComplete,
    mood = mood, moodCapturedAt = parseTimeOrNull(moodCapturedAt), emotion = emotion,
    energy = energy, promptSnapshot = promptSnapshot, durationMs = durationMs,
    wordCount = content.split(Regex("\\s+")).count { it.isNotBlank() },
    charCount = content.length, sourceLang = sourceLang
)

private fun AIInsightEntity.toBackup() = BackupInsight(
    entryId = entryId, summary = summary, followUp = followUpQuestionText, themes = themesCsv,
    mood = mood, modelName = modelName, totalTokens = totalTokens, createdAt = createdAt.toString()
)

private fun BackupInsight.toEntity(newEntryId: Long) = AIInsightEntity(
    entryId = newEntryId, summary = summary, followUpQuestionText = followUp, themesCsv = themes,
    mood = mood, modelName = modelName, totalTokens = totalTokens, createdAt = parseTime(createdAt)
)

private fun MemoryItemEntity.toBackup() = BackupMemory(
    kind = kind, text = text, weight = weight, timesSeen = timesSeen,
    createdAt = createdAt.toString(), lastSeenAt = lastSeenAt.toString(),
    sourceType = sourceType, userEdited = userEdited, dueAt = dueAt?.toString()
)

private fun BackupMemory.toEntity() = MemoryItemEntity(
    kind = kind, text = text, weight = weight, timesSeen = timesSeen,
    createdAt = parseTime(createdAt), lastSeenAt = parseTime(lastSeenAt),
    sourceType = sourceType, sourceId = null, userEdited = userEdited,
    dueAt = parseTimeOrNull(dueAt)
)

private fun CompanionMessageEntity.toBackup() = BackupMessage(
    threadId = threadId, role = role, content = content,
    createdAt = createdAt.toString(), source = source
)

private fun BackupMessage.toEntity() = CompanionMessageEntity(
    threadId = threadId, role = role, content = content,
    createdAt = parseTime(createdAt), source = source
)
