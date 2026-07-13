package com.example.nativegallery.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.example.nativegallery.model.MediaItem
import java.io.File
import com.example.nativegallery.model.MediaType
import org.json.JSONObject
import java.io.OutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class LockedMediaVaultRepository(context: Context) {
    private val appContext = context.applicationContext
    private val vaultDir: File = File(appContext.filesDir, VaultDirName).apply { mkdirs() }
    private val metadataPreferences = appContext.getSharedPreferences(
        "native_gallery_locked_vault_metadata",
        Context.MODE_PRIVATE
    )

    fun importMedia(mediaItem: MediaItem): Boolean {
        val sourceUri = mediaItem.contentUri ?: return false
        val targetFile = encryptedFileForId(mediaItem.id)
        if (targetFile.exists() && targetFile.length() > IvByteCount) {
            saveMetadata(mediaItem)
            return true
        }

        val tempFile = File(vaultDir, "${fileToken(mediaItem.id)}.tmp")
        return runCatching {
            appContext.contentResolver.openInputStream(sourceUri)?.use { input ->
                val cipher = Cipher.getInstance(CipherTransformation).apply {
                    init(Cipher.ENCRYPT_MODE, secretKey())
                }
                tempFile.outputStream().use { fileOutput ->
                    fileOutput.write(cipher.iv)
                    CipherOutputStream(fileOutput, cipher).use { cipherOutput ->
                        input.copyTo(cipherOutput, DefaultBufferSize)
                    }
                }
            } ?: return false

            if (targetFile.exists() && !targetFile.delete()) return@runCatching false
            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }
            val imported = targetFile.exists() && targetFile.length() > IvByteCount
            if (imported) {
                saveMetadata(mediaItem)
            }
            imported
        }.onFailure {
            tempFile.delete()
        }.getOrDefault(false)
    }

    fun encryptedUriFor(mediaId: String): Uri? {
        val token = fileToken(mediaId)
        val file = encryptedFileForToken(token)
        if (!file.exists()) return null
        return Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(vaultAuthority(appContext))
            .appendPath(token)
            .build()
    }

    fun hasEncryptedCopy(mediaId: String): Boolean {
        val file = encryptedFileForId(mediaId)
        return file.exists() && file.length() > IvByteCount
    }

    fun storedMediaItems(): List<MediaItem> {
        return metadataTokens().mapNotNull { token ->
            metadataPreferences.getString(metadataKey(token), null)
                ?.let(::decodeMetadata)
                ?.takeIf { hasEncryptedCopy(it.id) }
                ?.let { mediaItem ->
                    encryptedUriFor(mediaItem.id)?.let { vaultUri -> mediaItem.copy(contentUri = vaultUri) } ?: mediaItem
                }
        }
    }

    private fun saveMetadata(mediaItem: MediaItem) {
        val token = fileToken(mediaItem.id)
        val metadata = JSONObject()
            .put("id", mediaItem.id)
            .put("albumId", mediaItem.albumId)
            .put("type", mediaItem.type.name)
            .put("title", mediaItem.title)
            .put("dateLabel", mediaItem.dateLabel)
            .put("contentUri", mediaItem.contentUri?.toString().orEmpty())
            .put("isVideo", mediaItem.isVideo)
            .put("durationLabel", mediaItem.durationLabel.orEmpty())
            .put("durationMillis", mediaItem.durationMillis ?: -1L)
            .put("mimeType", mediaItem.mimeType.orEmpty())
            .put("fileSizeBytes", mediaItem.fileSizeBytes ?: -1L)
            .put("width", mediaItem.width ?: -1)
            .put("height", mediaItem.height ?: -1)

        val tokens = metadataTokens() + token
        metadataPreferences.edit()
            .putString(metadataKey(token), metadata.toString())
            .putStringSet(MetadataTokensKey, tokens)
            .apply()
    }

    private fun decodeMetadata(encoded: String): MediaItem? {
        return runCatching {
            val json = JSONObject(encoded)
            val type = runCatching { MediaType.valueOf(json.optString("type", MediaType.Photo.name)) }
                .getOrDefault(MediaType.Photo)
            MediaItem(
                id = json.getString("id"),
                albumId = json.optString("albumId", "locked"),
                type = type,
                title = json.optString("title", "Locked media"),
                dateLabel = json.optString("dateLabel", "Locked"),
                contentUri = json.optString("contentUri").takeIf { it.isNotBlank() }?.let(Uri::parse),
                isVideo = json.optBoolean("isVideo", type == MediaType.Video),
                durationLabel = json.optString("durationLabel").takeIf { it.isNotBlank() },
                durationMillis = json.optLong("durationMillis", -1L).takeIf { it >= 0L },
                mimeType = json.optString("mimeType").takeIf { it.isNotBlank() },
                fileSizeBytes = json.optLong("fileSizeBytes", -1L).takeIf { it >= 0L },
                width = json.optInt("width", -1).takeIf { it > 0 },
                height = json.optInt("height", -1).takeIf { it > 0 }
            )
        }.getOrNull()
    }

    private fun removeMetadata(token: String) {
        val tokens = metadataTokens() - token
        metadataPreferences.edit()
            .remove(metadataKey(token))
            .putStringSet(MetadataTokensKey, tokens)
            .apply()
    }

    private fun metadataTokens(): Set<String> {
        return metadataPreferences.getStringSet(MetadataTokensKey, emptySet()).orEmpty().toSet()
    }

    private fun metadataKey(token: String): String = "media_$token"

    fun delete(mediaId: String) {
        val token = fileToken(mediaId)
        encryptedFileForToken(token).delete()
        removeMetadata(token)
    }

    fun hasEncryptedToken(token: String): Boolean {
        val encryptedFile = encryptedFileForToken(token)
        return encryptedFile.exists() && encryptedFile.length() > IvByteCount
    }

    fun decryptTokenTo(token: String, outputStream: OutputStream): Boolean {
        val encryptedFile = encryptedFileForToken(token)
        if (!encryptedFile.exists() || encryptedFile.length() <= IvByteCount) return false

        return runCatching {
            encryptedFile.inputStream().use { fileInput ->
                val iv = ByteArray(IvByteCount)
                val read = fileInput.read(iv)
                if (read != IvByteCount) return false

                val cipher = Cipher.getInstance(CipherTransformation).apply {
                    init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(GcmTagBits, iv))
                }
                CipherInputStream(fileInput, cipher).use { cipherInput ->
                    cipherInput.copyTo(outputStream, DefaultBufferSize)
                }
            }
            true
        }.getOrDefault(false)
    }

    private fun encryptedFileForId(mediaId: String): File {
        return encryptedFileForToken(fileToken(mediaId))
    }

    private fun encryptedFileForToken(token: String): File {
        val cleanToken = token.filter { it.isLetterOrDigit() || it == '_' || it == '-' }
        return File(vaultDir, "$cleanToken.ngv")
    }

    private fun fileToken(mediaId: String): String {
        return Base64.encodeToString(
            mediaId.toByteArray(Charsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
        )
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(AndroidKeyStore).apply { load(null) }
        val existingEntry = keyStore.getEntry(KeyAlias, null) as? KeyStore.SecretKeyEntry
        if (existingEntry != null) return existingEntry.secretKey

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, AndroidKeyStore)
        val keySpec = KeyGenParameterSpec.Builder(
            KeyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(keySpec)
        return keyGenerator.generateKey()
    }

    companion object {
        private const val VaultDirName = "locked_media_vault"
        private const val KeyAlias = "native_gallery_locked_media_v1"
        private const val AndroidKeyStore = "AndroidKeyStore"
        private const val CipherTransformation = "AES/GCM/NoPadding"
        private const val IvByteCount = 12
        private const val GcmTagBits = 128
        private const val DefaultBufferSize = 64 * 1024

        private const val MetadataTokensKey = "metadata_tokens"
        fun vaultAuthority(context: Context): String = "${context.packageName}.vault"
    }
}