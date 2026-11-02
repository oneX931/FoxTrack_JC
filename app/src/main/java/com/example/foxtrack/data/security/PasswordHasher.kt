package com.example.foxtrack.data.security

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordHasher {
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val PREFIX = "pbkdf2-sha256"
    private const val ITERATIONS = 210_000
    private const val KEY_BITS = 256
    private const val SALT_BYTES = 16
    private const val FLAGS = Base64.NO_WRAP

    private val dummyHash: String by lazy { hash("foxtrack-timing-equalizer") }

    fun hash(password: String): String {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val derived = derive(password, salt, ITERATIONS, KEY_BITS)
        return listOf(
            PREFIX,
            ITERATIONS.toString(),
            Base64.encodeToString(salt, FLAGS),
            Base64.encodeToString(derived, FLAGS),
        ).joinToString("$")
    }

    fun verify(password: String, stored: String): Boolean {
        val parts = stored.split("$")
        if (parts.size != 4 || parts[0] != PREFIX) return false
        val iterations = parts[1].toIntOrNull() ?: return false
        val salt = try {
            Base64.decode(parts[2], FLAGS)
        } catch (_: IllegalArgumentException) {
            return false
        }
        val expected = try {
            Base64.decode(parts[3], FLAGS)
        } catch (_: IllegalArgumentException) {
            return false
        }
        if (expected.isEmpty()) return false
        val actual = derive(password, salt, iterations, expected.size * 8)
        return MessageDigest.isEqual(actual, expected)
    }

    fun spendVerificationTime(password: String) {
        verify(password, dummyHash)
    }

    private fun derive(password: String, salt: ByteArray, iterations: Int, keyBits: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, keyBits)
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
