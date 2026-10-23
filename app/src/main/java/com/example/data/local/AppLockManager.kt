package com.example.data.local

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Optional app-level PIN lock, independent from the vault master PIN.
 * PINs are stored as salted PBKDF2 hashes and are never persisted in plaintext.
 */
class AppLockManager(context: Context) {

    private val prefs = context.getSharedPreferences("omnichat_app_lock", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ENABLED = "enabled"
        private const val KEY_HASH = "pin_hash"
        private const val KEY_SALT = "pin_salt"
        private const val KEY_TIMEOUT = "timeout_ms"
        private const val KEY_LAST_BACKGROUNDED = "last_backgrounded_at"
        private const val ITERATIONS = 120_000
        private const val KEY_LENGTH_BITS = 256

        const val IMMEDIATE = 0L
        const val THIRTY_SECONDS = 30_000L
        const val ONE_MINUTE = 60_000L
        const val FIVE_MINUTES = 300_000L
    }

    fun isEnabled(): Boolean =
        prefs.getBoolean(KEY_ENABLED, false) && prefs.getString(KEY_HASH, null) != null

    fun setPin(pin: String): Boolean {
        if (!isValidPin(pin)) return false
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = hashPin(pin, salt)
        prefs.edit()
            .putString(KEY_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putBoolean(KEY_ENABLED, true)
            .apply()
        return true
    }

    fun verify(pin: String): Boolean {
        if (!isValidPin(pin)) return false
        val storedHash = prefs.getString(KEY_HASH, null) ?: return false
        val storedSalt = prefs.getString(KEY_SALT, null) ?: return false
        return try {
            val expected = Base64.decode(storedHash, Base64.NO_WRAP)
            val salt = Base64.decode(storedSalt, Base64.NO_WRAP)
            MessageDigest.isEqual(expected, hashPin(pin, salt))
        } catch (_: Exception) {
            false
        }
    }

    fun disable() {
        prefs.edit()
            .remove(KEY_HASH)
            .remove(KEY_SALT)
            .putBoolean(KEY_ENABLED, false)
            .apply()
    }

    fun isValidPin(pin: String): Boolean = pin.matches(Regex("\\d{4,8}"))

    var timeoutMillis: Long
        get() = prefs.getLong(KEY_TIMEOUT, ONE_MINUTE)
        set(value) = prefs.edit().putLong(KEY_TIMEOUT, value.coerceAtLeast(0L)).apply()

    fun markBackgrounded(now: Long = System.currentTimeMillis()) {
        prefs.edit().putLong(KEY_LAST_BACKGROUNDED, now).apply()
    }

    fun shouldRelock(now: Long = System.currentTimeMillis()): Boolean {
        if (!isEnabled()) return false
        val lastBackgrounded = prefs.getLong(KEY_LAST_BACKGROUNDED, -1L)
        if (lastBackgrounded < 0L) return false
        return now - lastBackgrounded >= timeoutMillis
    }

    fun clearBackgroundTimestamp() {
        prefs.edit().remove(KEY_LAST_BACKGROUNDED).apply()
    }

    private fun hashPin(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
                .generateSecret(spec)
                .encoded
        } finally {
            spec.clearPassword()
        }
    }
}
