package com.cosmiclaboratory.axiom.data.backup

import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom

/**
 * Passphrase encryption for backup files.
 *
 * A journal export is the most sensitive file this app will ever produce: it is
 * everything the user has written, in one place, sitting wherever they saved
 * it. Encryption is optional because a plaintext JSON backup is also the most
 * *portable* thing the app can produce — readable in ten years by anything,
 * with or without Axiom — and that is a real form of ownership too. The choice
 * is the user's, and the UI states plainly what each one means.
 *
 * Standard construction, no invention: PBKDF2-HMAC-SHA256 to stretch the
 * passphrase, AES-256-GCM for the payload so tampering and a wrong passphrase
 * both fail loudly rather than yielding garbage. Salt and IV are random per
 * export and stored beside the ciphertext, which is what they are for.
 */
object BackupCipher {

    /** Chosen to be uncomfortable on a GPU but tolerable on a phone. */
    const val ITERATIONS = 210_000
    private const val KEY_BITS = 256
    private const val GCM_TAG_BITS = 128
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12

    fun encrypt(plaintext: String, passphrase: CharArray): EncryptedBackup {
        val random = SecureRandom()
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val key = deriveKey(passphrase, salt, ITERATIONS)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        return EncryptedBackup(
            iterations = ITERATIONS,
            salt = salt.toBase64(),
            iv = iv.toBase64(),
            ciphertext = ciphertext.toBase64()
        )
    }

    /**
     * Returns null when the passphrase is wrong or the file has been altered.
     * GCM cannot tell those two apart, and neither can we — both mean "this
     * will not open", which is the only thing the user can act on.
     */
    fun decrypt(envelope: EncryptedBackup, passphrase: CharArray): String? = runCatching {
        val key = deriveKey(passphrase, envelope.salt.fromBase64(), envelope.iterations)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, envelope.iv.fromBase64()))
        String(cipher.doFinal(envelope.ciphertext.fromBase64()), Charsets.UTF_8)
    }.getOrNull()

    private fun deriveKey(passphrase: CharArray, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(passphrase, salt, iterations, KEY_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }

    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    // java.util rather than android.util: it exists from API 26 (this app's
    // minimum) and, unlike the Android one, is real in JVM unit tests rather
    // than a stub that throws — so the crypto here is actually covered.
    private fun ByteArray.toBase64(): String = java.util.Base64.getEncoder().encodeToString(this)

    private fun String.fromBase64(): ByteArray = java.util.Base64.getDecoder().decode(this)
}
