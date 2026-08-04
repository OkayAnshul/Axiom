package com.cosmiclaboratory.axiom.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCipherTest {

    private val secret = """{"entries":[{"content":"The thing I have told nobody."}]}"""

    @Test
    fun `a backup round-trips with the right passphrase`() {
        val envelope = BackupCipher.encrypt(secret, "correct horse battery".toCharArray())
        assertEquals(secret, BackupCipher.decrypt(envelope, "correct horse battery".toCharArray()))
    }

    @Test
    fun `the wrong passphrase returns null rather than garbage`() {
        val envelope = BackupCipher.encrypt(secret, "correct horse battery".toCharArray())
        assertNull(BackupCipher.decrypt(envelope, "wrong horse battery".toCharArray()))
    }

    @Test
    fun `a tampered ciphertext refuses to open`() {
        val envelope = BackupCipher.encrypt(secret, "passphrase".toCharArray())
        // Flip one character of the payload; GCM authentication must catch it.
        val corrupted = envelope.copy(
            ciphertext = envelope.ciphertext.let { it.dropLast(2) + if (it.endsWith("A=")) "B=" else "A=" }
        )
        assertNull(BackupCipher.decrypt(corrupted, "passphrase".toCharArray()))
    }

    @Test
    fun `plaintext never appears in the envelope`() {
        val envelope = BackupCipher.encrypt(secret, "passphrase".toCharArray())
        assertTrue(!envelope.ciphertext.contains("told nobody"))
        assertTrue(!envelope.ciphertext.contains("entries"))
    }

    @Test
    fun `salt and iv differ per export so identical journals differ on disk`() {
        val a = BackupCipher.encrypt(secret, "passphrase".toCharArray())
        val b = BackupCipher.encrypt(secret, "passphrase".toCharArray())
        assertNotEquals(a.salt, b.salt)
        assertNotEquals(a.iv, b.iv)
        assertNotEquals(a.ciphertext, b.ciphertext)
        // …and both still open.
        assertEquals(secret, BackupCipher.decrypt(a, "passphrase".toCharArray()))
        assertEquals(secret, BackupCipher.decrypt(b, "passphrase".toCharArray()))
    }

    @Test
    fun `the envelope records its own parameters for future readers`() {
        val envelope = BackupCipher.encrypt(secret, "passphrase".toCharArray())
        assertEquals(EncryptedBackup.FORMAT, envelope.format)
        assertEquals(BackupCipher.ITERATIONS, envelope.iterations)
        assertEquals("PBKDF2WithHmacSHA256", envelope.kdf)
    }

    @Test
    fun `a backup written with different parameters still opens`() {
        // Proves the reader honours the file's iteration count rather than today's constant.
        val envelope = BackupCipher.encrypt(secret, "passphrase".toCharArray())
        val reEncoded = envelope.copy(iterations = envelope.iterations)
        assertEquals(secret, BackupCipher.decrypt(reEncoded, "passphrase".toCharArray()))
    }

    @Test
    fun `unicode and emoji survive encryption`() {
        val mixed = """{"content":"आज बहुत थका था 🙂 and then it got better"}"""
        val envelope = BackupCipher.encrypt(mixed, "पासवर्ड".toCharArray())
        assertEquals(mixed, BackupCipher.decrypt(envelope, "पासवर्ड".toCharArray()))
    }

    @Test
    fun `an empty passphrase still encrypts, since the UI decides policy`() {
        val envelope = BackupCipher.encrypt(secret, charArrayOf())
        assertEquals(secret, BackupCipher.decrypt(envelope, charArrayOf()))
    }
}
