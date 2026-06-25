package com.example.nativegallery.data

import android.content.Context
import java.security.MessageDigest
import java.security.SecureRandom

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
            .apply()
        return true
    }

    fun verifyPin(pin: String): Boolean {
        if (!isValidPin(pin)) return false
        val salt = preferences.getString(PIN_SALT_KEY, null) ?: return false
        val savedHash = preferences.getString(PIN_HASH_KEY, null) ?: return false
        return hashPin(salt, pin) == savedHash
    }

    fun clearPin() {
        preferences.edit()
            .remove(PIN_SALT_KEY)
            .remove(PIN_HASH_KEY)
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

        fun isValidPin(pin: String): Boolean {
            return pin.length in 4..12 && pin.all { it.isDigit() }
        }
    }
}