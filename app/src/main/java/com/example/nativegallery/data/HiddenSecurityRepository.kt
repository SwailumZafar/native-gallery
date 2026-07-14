package com.example.nativegallery.data

import android.content.Context
import java.security.MessageDigest
import java.security.SecureRandom

data class PinFailureState(
    val failedAttempts: Int,
    val lockoutUntilMillis: Long
) {
    fun remainingMillis(nowMillis: Long = System.currentTimeMillis()): Long {
        return (lockoutUntilMillis - nowMillis).coerceAtLeast(0L)
    }
}

class HiddenSecurityRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "native_gallery_hidden_security",
        Context.MODE_PRIVATE
    )

    fun hasPin(): Boolean {
        return !preferences.getString(PIN_HASH_KEY, null).isNullOrBlank() &&
            !preferences.getString(PIN_SALT_KEY, null).isNullOrBlank()
    }

    fun setPin(pin: String): Boolean {
        if (!isValidPin(pin)) return false
        val salt = randomSaltHex()
        preferences.edit()
            .putString(PIN_SALT_KEY, salt)
            .putString(PIN_HASH_KEY, hashPin(salt, pin))
            .remove(PIN_FAILED_ATTEMPTS_KEY)
            .remove(PIN_LOCKOUT_UNTIL_KEY)
            .apply()
        return true
    }

    fun verifyPin(pin: String): Boolean {
        if (!isValidPin(pin)) return false
        val salt = preferences.getString(PIN_SALT_KEY, null) ?: return false
        val savedHash = preferences.getString(PIN_HASH_KEY, null) ?: return false
        return hashPin(salt, pin) == savedHash
    }

    fun pinLockoutUntilMillis(): Long {
        return preferences.getLong(PIN_LOCKOUT_UNTIL_KEY, 0L)
    }

    fun recordFailedPinAttempt(nowMillis: Long = System.currentTimeMillis()): PinFailureState {
        val currentLockout = pinLockoutUntilMillis()
        val failureState = nextPinFailureState(
            currentFailedAttempts = preferences.getInt(PIN_FAILED_ATTEMPTS_KEY, 0),
            currentLockoutUntilMillis = currentLockout,
            nowMillis = nowMillis
        )

        preferences.edit()
            .putInt(
                PIN_FAILED_ATTEMPTS_KEY,
                if (failureState.lockoutUntilMillis > nowMillis) 0 else failureState.failedAttempts
            )
            .putLong(PIN_LOCKOUT_UNTIL_KEY, failureState.lockoutUntilMillis)
            .apply()

        return failureState
    }

    fun clearFailedPinAttempts() {
        preferences.edit()
            .remove(PIN_FAILED_ATTEMPTS_KEY)
            .remove(PIN_LOCKOUT_UNTIL_KEY)
            .apply()
    }

    fun clearPin() {
        preferences.edit()
            .remove(PIN_SALT_KEY)
            .remove(PIN_HASH_KEY)
            .remove(PIN_FAILED_ATTEMPTS_KEY)
            .remove(PIN_LOCKOUT_UNTIL_KEY)
            .apply()
    }

    private fun randomSaltHex(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.toHexString()
    }

    private fun hashPin(salt: String, pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest("$salt:$pin".toByteArray(Charsets.UTF_8))
        return bytes.toHexString()
    }

    private fun ByteArray.toHexString(): String {
        return joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }

    companion object {
        private const val PIN_SALT_KEY = "pin_salt"
        private const val PIN_HASH_KEY = "pin_hash"
        private const val PIN_FAILED_ATTEMPTS_KEY = "pin_failed_attempts"
        private const val PIN_LOCKOUT_UNTIL_KEY = "pin_lockout_until"
        private const val MaxFailedPinAttempts = 5
        private const val PinLockoutMillis = 30_000L

        fun isValidPin(pin: String): Boolean {
            return pin.length in 4..12 && pin.all { it.isDigit() }
        }

        fun nextPinFailureState(
            currentFailedAttempts: Int,
            currentLockoutUntilMillis: Long,
            nowMillis: Long
        ): PinFailureState {
            if (currentLockoutUntilMillis > nowMillis) {
                return PinFailureState(currentFailedAttempts, currentLockoutUntilMillis)
            }
            val failedAttempts = currentFailedAttempts.coerceAtLeast(0) + 1
            return PinFailureState(
                failedAttempts = failedAttempts,
                lockoutUntilMillis = if (failedAttempts >= MaxFailedPinAttempts) {
                    nowMillis + PinLockoutMillis
                } else {
                    0L
                }
            )
        }
    }
}
