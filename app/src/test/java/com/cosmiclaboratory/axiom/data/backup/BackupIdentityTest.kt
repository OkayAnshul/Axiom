package com.cosmiclaboratory.axiom.data.backup

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * These rules decide whether restoring a backup gives the user their journal
 * back or gives them two of everything. Restoring the same file twice must be
 * a no-op.
 */
class BackupIdentityTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

    @Test
    fun `the same entry restored twice is recognised`() {
        val a = entryIdentity("2026-08-01T20:00", "A long and difficult day.")
        val b = entryIdentity("2026-08-01T20:00", "A long and difficult day.")
        assertEquals(a, b)
    }

    @Test
    fun `surrounding whitespace does not create a duplicate`() {
        assertEquals(
            entryIdentity("2026-08-01T20:00", "Same words."),
            entryIdentity("2026-08-01T20:00", "  Same words.  ")
        )
    }

    @Test
    fun `same words at a different moment are different entries`() {
        assertNotEquals(
            entryIdentity("2026-08-01T20:00", "Tired again."),
            entryIdentity("2026-08-02T20:00", "Tired again.")
        )
    }

    @Test
    fun `different words at the same moment are different entries`() {
        assertNotEquals(
            entryIdentity("2026-08-01T20:00", "Tired again."),
            entryIdentity("2026-08-01T20:00", "Actually a good day.")
        )
    }

    @Test
    fun `memory identity ignores case and kind casing`() {
        assertEquals(
            memoryIdentity("PERSON", "Riya is your sister."),
            memoryIdentity("person", "  riya is your sister.  ")
        )
    }

    @Test
    fun `the same memory text under a different kind is not the same memory`() {
        assertNotEquals(
            memoryIdentity("GOAL", "Start running again."),
            memoryIdentity("THEME", "Start running again.")
        )
    }

    @Test
    fun `messages are scoped to their thread`() {
        assertNotEquals(
            messageIdentity("companion", "2026-08-01T20:00", "hello"),
            messageIdentity("other", "2026-08-01T20:00", "hello")
        )
    }

    // ---- the file format ----------------------------------------------------

    @Test
    fun `a backup round-trips through json unchanged`() {
        val original = BackupFile(
            exportedAt = "2026-08-04T12:00",
            displayName = "Anshul",
            entries = listOf(
                BackupEntry(
                    id = 1, content = "आज बहुत थका था 🙂", createdAt = "2026-08-01T20:00",
                    updatedAt = "2026-08-01T20:00", mood = 2, emotion = "STRESS"
                )
            ),
            tags = listOf(BackupTag(1, "work", 0xFF112233L, "2026-08-01T20:00")),
            entryTags = listOf(BackupEntryTag(1, 1)),
            memories = listOf(
                BackupMemory(
                    kind = "PERSON", text = "Riya is your sister.",
                    createdAt = "2026-08-01T20:00", lastSeenAt = "2026-08-01T20:00"
                )
            ),
            companionMessages = listOf(
                BackupMessage("companion", "USER", "hi", "2026-08-01T20:00")
            )
        )
        val encoded = json.encodeToString(BackupFile.serializer(), original)
        val decoded = json.decodeFromString(BackupFile.serializer(), encoded)
        assertEquals(original, decoded)
        // Unicode must survive as text, not escapes the user cannot read.
        assertTrue(encoded.contains("थका"))
    }

    @Test
    fun `the format marker is present so a restore can recognise the file`() {
        val encoded = json.encodeToString(
            BackupFile.serializer(),
            BackupFile(exportedAt = "2026-08-04T12:00")
        )
        assertTrue(encoded.contains(BackupFile.FORMAT))
        assertTrue(!encoded.contains(EncryptedBackup.FORMAT))
    }

    @Test
    fun `an encrypted file is distinguishable from a plain one`() {
        val sealed = BackupCipher.encrypt("""{"format":"axiom.backup"}""", "pw".toCharArray())
        val encoded = json.encodeToString(EncryptedBackup.serializer(), sealed)
        assertTrue(encoded.contains(EncryptedBackup.FORMAT))
    }

    @Test
    fun `older backups missing newer fields still load`() {
        // A file written before `emotion` existed must not fail to restore.
        val old = """
            {"format":"axiom.backup","version":1,"exported_at":"2026-01-01T10:00",
             "entries":[{"id":1,"content":"Hello","created_at":"2026-01-01T10:00",
                         "updated_at":"2026-01-01T10:00"}]}
        """.trimIndent()
        val decoded = json.decodeFromString(BackupFile.serializer(), old)
        assertEquals(1, decoded.entries.size)
        assertEquals(null, decoded.entries[0].emotion)
        assertEquals("FREE_FORM", decoded.entries[0].kind)
    }
}
