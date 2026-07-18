package com.example.nativegallery.data

import android.content.Context
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

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
        return runCatching {
            val salt = randomSalt()
            val hash = derivePinHash(pin, salt, Pbkdf2Iterations)
            preferences.edit()
                .putInt(PIN_VERSION_KEY, CurrentCredentialVersion)
                .putInt(PIN_ITERATIONS_KEY, Pbkdf2Iterations)
                .putString(PIN_SALT_KEY, salt.toHexString())
                .putString(PIN_HASH_KEY, hash.toHexString())
                .remove(PIN_FAILED_ATTEMPTS_KEY)
                .remove(PIN_LOCKOUT_UNTIL_KEY)
                .commit()
        }.getOrDefault(false)
    }

    fun verifyPin(pin: String): Boolean {
        if (!isValidPin(pin)) return false
        val encodedSalt = preferences.getString(PIN_SALT_KEY, null) ?: return false
        val savedHash = preferences.getString(PIN_HASH_KEY, null) ?: return false
        val credentialVersion = preferences.getInt(PIN_VERSION_KEY, LegacyCredentialVersion)
        val verified = when (credentialVersion) {
            LegacyCredentialVersion -> verifyLegacyPin(encodedSalt, savedHash, pin)
            CurrentCredentialVersion -> {
                val iterations = preferences.getInt(PIN_ITERATIONS_KEY, 0)
                val salt = encodedSalt.hexToByteArrayOrNull()
                val expectedHash = savedHash.hexToByteArrayOrNull()
                if (
                    salt == null || expectedHash == null ||
                    iterations !in MinimumStoredIterations..MaximumStoredIterations
                ) {
                    false
                } else {
                    val candidateHash = runCatching {
                        derivePinHash(pin, salt, iterations)
                    }.getOrNull()
                    candidateHash != null && MessageDigest.isEqual(candidateHash, expectedHash)
                }
            }
            else -> false
        }
        if (
            verified && (
                credentialVersion != CurrentCredentialVersion ||
                    preferences.getInt(PIN_ITERATIONS_KEY, 0) < Pbkdf2Iterations
                )
        ) {
            setPin(pin)
        }
        return verified
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
            .commit()

        return failureState
    }

    fun clearFailedPinAttempts() {
        preferences.edit()
            .remove(PIN_FAILED_ATTEMPTS_KEY)
            .remove(PIN_LOCKOUT_UNTIL_KEY)
            .commit()
    }

    fun clearPin() {
        preferences.edit()
            .remove(PIN_SALT_KEY)
            .remove(PIN_HASH_KEY)
            .remove(PIN_VERSION_KEY)
            .remove(PIN_ITERATIONS_KEY)
            .remove(PIN_FAILED_ATTEMPTS_KEY)
            .remove(PIN_LOCKOUT_UNTIL_KEY)
            .commit()
    }

    private fun randomSalt(): ByteArray {
        return ByteArray(PinSaltBytes).also(SecureRandom()::nextBytes)
    }

    private fun verifyLegacyPin(salt: String, savedHash: String, pin: String): Boolean {
        val expectedHash = savedHash.hexToByteArrayOrNull() ?: return false
        val candidateHash = MessageDigest.getInstance("SHA-256")
            .digest("$salt:$pin".toByteArray(Charsets.UTF_8))
        return MessageDigest.isEqual(candidateHash, expectedHash)
    }

    private fun ByteArray.toHexString(): String {
        return joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }

    private fun String.hexToByteArrayOrNull(): ByteArray? {
        if (length % 2 != 0 || any { it.digitToIntOrNull(16) == null }) return null
        return ByteArray(length / 2) { index ->
            substring(index * 2, index * 2 + 2).toInt(16).toByte()
        }
    }

    companion object {
        private const val PIN_SALT_KEY = "pin_salt"
        private const val PIN_HASH_KEY = "pin_hash"
        private const val PIN_VERSION_KEY = "pin_version"
        private const val PIN_ITERATIONS_KEY = "pin_iterations"
        private const val PIN_FAILED_ATTEMPTS_KEY = "pin_failed_attempts"
        private const val PIN_LOCKOUT_UNTIL_KEY = "pin_lockout_until"
        private const val LegacyCredentialVersion = 1
        private const val CurrentCredentialVersion = 2
        private const val PinSaltBytes = 16
        private const val PinHashBits = 256
        private const val MinimumStoredIterations = 100_000
        private const val MaximumStoredIterations = 1_000_000
        internal const val Pbkdf2Iterations = 210_000
        private const val MaxFailedPinAttempts = 5
        private const val PinLockoutMillis = 30_000L

        internal fun derivePinHash(
            pin: String,
            salt: ByteArray,
            iterations: Int = Pbkdf2Iterations
        ): ByteArray {
            require(pin.isNotEmpty())
            require(salt.isNotEmpty())
            require(iterations > 0)
            val keySpec = PBEKeySpec(pin.toCharArray(), salt, iterations, PinHashBits)
            return try {
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(keySpec)
                    .encoded
            } finally {
                keySpec.clearPassword()
            }
        }

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