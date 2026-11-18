package com.group14.notes.auth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Password hashing with PBKDF2-HMAC-SHA256 and a random per-user salt.
 * Only the salt and the hash are stored (Base64 text); the password itself never is.
 */
object PasswordHasher {

    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    const val SALT_BYTES = 16

    fun generateSalt(): ByteArray =
        ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }

    fun hash(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    fun encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

    fun decode(text: String): ByteArray = Base64.getDecoder().decode(text)

    /** Constant-time comparison of the attempt against the stored hash. */
    fun verify(password: String, storedHash: String, storedSalt: String): Boolean {
        val attempt = hash(password, decode(storedSalt))
        return MessageDigest.isEqual(decode(storedHash), attempt)
    }
}
