package com.example.nativegallery.data

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Size
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.model.MediaType
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.math.max
import org.json.JSONObject

data class LockedMediaVaultSnapshot(
    val mediaItems: List<MediaItem> = emptyList(),
    val missingPreviewIds: Set<String> = emptySet()
) {
    val mediaById: Map<String, MediaItem> by lazy(LazyThreadSafetyMode.NONE) { mediaItems.associateBy { it.id } }
}

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
        val fullCopyReady = if (targetFile.exists() && targetFile.length() > MinimumEncryptedFileBytes) {
            true
        } else {
            runCatching {
                appContext.contentResolver.openInputStream(sourceUri)?.use { input ->
                    encryptInputToFile(input, targetFile)
                } ?: false
            }.getOrDefault(false)
        }
        if (!fullCopyReady) return false

        // A failed preview must never invalidate the protected full copy.
        runCatching { ensureEncryptedPreviewFromSource(mediaItem) }
        saveMetadata(mediaItem)
        return true
    }

    fun encryptedUriFor(
        mediaId: String,
        mimeType: String? = null,
        isVideo: Boolean = false
    ): Uri? {
        val token = fileToken(mediaId)
        val file = encryptedFileForToken(token)
        if (!file.exists() || file.length() <= MinimumEncryptedFileBytes) return null
        return vaultUri(
            token = token,
            mimeType = LockedMediaVaultPolicy.mimeType(mimeType, isVideo),
            preview = false
        )
    }

    fun encryptedPreviewUriFor(mediaItem: MediaItem): Uri? {
        mediaItem.previewUri?.let { return it }
        val token = fileToken(mediaItem.id)
        if (!hasEncryptedPreview(mediaItem.id)) return null
        return vaultUri(token = token, mimeType = PreviewMimeType, preview = true)
    }

    fun hasEncryptedPreview(mediaId: String): Boolean {
        val file = encryptedPreviewFileForToken(fileToken(mediaId))
        return file.exists() && file.length() > MinimumEncryptedFileBytes
    }

    fun ensureEncryptedPreviewFromVault(mediaItem: MediaItem): Boolean {
        if (hasEncryptedPreview(mediaItem.id)) return true
        val token = fileToken(mediaItem.id)
        if (!hasEncryptedToken(token)) return false
        val migrationDir = File(appContext.cacheDir, PreviewMigrationDirName).apply { mkdirs() }
        val decryptedFile = File(migrationDir, "$token.media")
        decryptedFile.delete()
        return try {
            runCatching {
                val decrypted = decryptedFile.outputStream().use { output ->
                    decryptTokenTo(token, output)
                }
                if (!decrypted || decryptedFile.length() == 0L) return@runCatching false
                val bitmap = loadPreviewBitmap(decryptedFile, mediaItem.isVideo)
                    ?: return@runCatching false
                try {
                    encryptPreviewBitmap(bitmap, encryptedPreviewFileForToken(token))
                } finally {
                    bitmap.recycle()
                }
            }.getOrDefault(false)
        } finally {
            decryptedFile.delete()
        }
    }

    fun hasEncryptedCopy(mediaId: String): Boolean {
        val file = encryptedFileForId(mediaId)
        return file.exists() && file.length() > MinimumEncryptedFileBytes
    }

    fun loadSnapshot(): LockedMediaVaultSnapshot {
        val missingPreviewIds = mutableSetOf<String>()
        val mediaItems = metadataTokens().mapNotNull { token ->
            metadataPreferences.getString(metadataKey(token), null)
                ?.let(::decodeMetadata)
                ?.takeIf { hasEncryptedCopy(it.id) }
                ?.let { mediaItem ->
                    val previewUri = if (hasEncryptedPreview(mediaItem.id)) {
                        vaultUri(token = token, mimeType = PreviewMimeType, preview = true)
                    } else {
                        missingPreviewIds += mediaItem.id
                        null
                    }
                    mediaItem.copy(
                        contentUri = vaultUri(
                            token = token,
                            mimeType = LockedMediaVaultPolicy.mimeType(mediaItem.mimeType, mediaItem.isVideo),
                            preview = false
                        ),
                        previewUri = previewUri
                    )
                }
        }
        return LockedMediaVaultSnapshot(
            mediaItems = mediaItems.sortedByDescending { it.sortTimestampMillis },
            missingPreviewIds = missingPreviewIds
        )
    }

    fun storedMediaItems(): List<MediaItem> = loadSnapshot().mediaItems

    fun originalMediaExists(mediaId: String): Boolean {
        val token = fileToken(mediaId)
        val originalUri = metadataPreferences.getString(metadataKey(token), null)
            ?.let(::decodeMetadata)
            ?.contentUri
            ?: return false
        return restoredMediaIsReadable(appContext.contentResolver, originalUri)
    }

    fun restoreMedia(mediaItem: MediaItem): Uri? {
        val token = fileToken(mediaItem.id)
        if (!hasEncryptedToken(token)) return null
        val resolver = appContext.contentResolver
        val mimeType = LockedMediaVaultPolicy.restorableMimeType(mediaItem.mimeType, mediaItem.isVideo)
        val displayName = LockedMediaVaultPolicy.restoredDisplayName(
            title = mediaItem.title,
            mimeType = mimeType,
            isVideo = mediaItem.isVideo
        )
        val collection = if (mediaItem.isVideo) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, restoreRelativePath(mediaItem))
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            mediaItem.sortTimestampMillis.takeIf { it > 0L }?.let {
                put(MediaStore.MediaColumns.DATE_TAKEN, it)
            }
        }
        val restoredUri = runCatching { resolver.insert(collection, values) }.getOrNull() ?: return null
        val restored = runCatching {
            resolver.openOutputStream(restoredUri, "w")?.use { output ->
                decryptTokenTo(token, output)
            } ?: false
        }.getOrDefault(false)
        if (!restored || !restoredMediaIsReadable(resolver, restoredUri)) {
            deleteRestoredCopy(resolver, restoredUri)
            return null
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val published = runCatching {
                resolver.update(
                    restoredUri,
                    ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) },
                    null,
                    null
                )
            }.getOrDefault(0) > 0
            if (!published) {
                deleteRestoredCopy(resolver, restoredUri)
                return null
            }
        }
        return restoredUri
    }

    private fun restoreRelativePath(mediaItem: MediaItem): String {
        val originalPath = LockedMediaVaultPolicy.normalizedOriginalRelativePath(mediaItem.relativePath)
        if (originalPath != null) {
            val sharedStorageRoot = Environment.getExternalStorageDirectory()
            val originalDirectory = File(sharedStorageRoot, originalPath.trimEnd('/'))
            if (runCatching { originalDirectory.isDirectory }.getOrDefault(false)) {
                return originalPath
            }
        }
        return LockedMediaVaultPolicy.fallbackRelativePath(mediaItem.isVideo)
    }

    private fun restoredMediaIsReadable(resolver: ContentResolver, uri: Uri): Boolean {
        val queriedSize = runCatching {
            resolver.query(
                uri,
                arrayOf(MediaStore.MediaColumns.SIZE),
                null,
                null,
                null
            )?.use { cursor ->
                val sizeColumn = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                if (sizeColumn >= 0 && cursor.moveToFirst() && !cursor.isNull(sizeColumn)) {
                    cursor.getLong(sizeColumn)
                } else {
                    -1L
                }
            } ?: -1L
        }.getOrDefault(-1L)
        if (LockedMediaVaultPolicy.isRestoredCopyUsable(queriedSize)) return true

        val descriptorSize = runCatching {
            resolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: -1L
        }.getOrDefault(-1L)
        if (LockedMediaVaultPolicy.isRestoredCopyUsable(descriptorSize)) return true

        return runCatching {
            resolver.openInputStream(uri)?.use { input -> input.read() != -1 } ?: false
        }.getOrDefault(false)
    }

    private fun deleteRestoredCopy(resolver: ContentResolver, uri: Uri) {
        runCatching { resolver.delete(uri, null, null) }
    }

    fun delete(mediaId: String) {
        val token = fileToken(mediaId)
        encryptedFileForToken(token).delete()
        encryptedPreviewFileForToken(token).delete()
        LockedMediaVaultProvider.clearCachedToken(appContext, token)
        removeMetadata(token)
    }

    fun hasEncryptedToken(token: String, preview: Boolean = false): Boolean {
        val encryptedFile = if (preview) {
            encryptedPreviewFileForToken(token)
        } else {
            encryptedFileForToken(token)
        }
        return encryptedFile.exists() && encryptedFile.length() > MinimumEncryptedFileBytes
    }

    fun decryptTokenTo(
        token: String,
        outputStream: OutputStream,
        preview: Boolean = false
    ): Boolean {
        val encryptedFile = if (preview) {
            encryptedPreviewFileForToken(token)
        } else {
            encryptedFileForToken(token)
        }
        if (!encryptedFile.exists() || encryptedFile.length() <= MinimumEncryptedFileBytes) return false

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

    private fun ensureEncryptedPreviewFromSource(mediaItem: MediaItem): Boolean {
        val targetFile = encryptedPreviewFileForToken(fileToken(mediaItem.id))
        if (targetFile.exists() && targetFile.length() > MinimumEncryptedFileBytes) return true
        val bitmap = loadPreviewBitmap(mediaItem) ?: return false
        return try {
            encryptPreviewBitmap(bitmap, targetFile)
        } finally {
            bitmap.recycle()
        }
    }

    private fun encryptPreviewBitmap(bitmap: Bitmap, targetFile: File): Boolean {
        val encoded = ByteArrayOutputStream().use { output ->
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, PreviewJpegQuality, output)) return false
            output.toByteArray()
        }
        return encoded.inputStream().use { input -> encryptInputToFile(input, targetFile) }
    }

    private fun loadPreviewBitmap(mediaItem: MediaItem): Bitmap? {
        val uri = mediaItem.contentUri ?: return null
        val platformThumbnail = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                appContext.contentResolver.loadThumbnail(uri, Size(PreviewSize, PreviewSize), null)
            }.getOrNull()
        } else {
            null
        }
        if (platformThumbnail != null) return platformThumbnail
        return if (mediaItem.isVideo) {
            VideoFrameDecoder.decode(appContext, uri, PreviewSize)
        } else {
            appContext.contentResolver.openInputStream(uri)?.use { stream -> BitmapFactory.decodeStream(stream) }
                ?.scaledPreview()
        }
    }

    private fun loadPreviewBitmap(file: File, isVideo: Boolean): Bitmap? {
        return if (isVideo) {
            VideoFrameDecoder.decode(file, PreviewSize)
        } else {
            BitmapFactory.decodeFile(file.absolutePath)?.scaledPreview()
        }
    }

    private fun Bitmap.scaledPreview(): Bitmap {
        val largestSide = max(width, height)
        if (largestSide <= PreviewSize) return this
        val scale = PreviewSize.toFloat() / largestSide.toFloat()
        val scaled = Bitmap.createScaledBitmap(
            this,
            (width * scale).toInt().coerceAtLeast(1),
            (height * scale).toInt().coerceAtLeast(1),
            true
        )
        if (scaled !== this) recycle()
        return scaled
    }

    private fun encryptInputToFile(input: InputStream, targetFile: File): Boolean {
        val tempFile = File(vaultDir, "${targetFile.name}.tmp")
        tempFile.delete()
        return runCatching {
            val cipher = Cipher.getInstance(CipherTransformation).apply {
                init(Cipher.ENCRYPT_MODE, secretKey())
            }
            tempFile.outputStream().use { fileOutput ->
                fileOutput.write(cipher.iv)
                CipherOutputStream(fileOutput, cipher).use { cipherOutput ->
                    input.copyTo(cipherOutput, DefaultBufferSize)
                }
            }
            if (targetFile.exists() && !targetFile.delete()) return@runCatching false
            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }
            targetFile.exists() && targetFile.length() > MinimumEncryptedFileBytes
        }.onFailure {
            tempFile.delete()
        }.getOrDefault(false)
    }

    private fun vaultUri(token: String, mimeType: String, preview: Boolean): Uri {
        return Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(vaultAuthority(appContext))
            .apply { if (preview) appendPath(VaultPreviewPath) }
            .appendPath(token)
            .appendQueryParameter(VaultMimeTypeQuery, mimeType)
            .build()
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
            .put("relativePath", mediaItem.relativePath.orEmpty())
            .put("sortTimestampMillis", mediaItem.sortTimestampMillis)

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
                height = json.optInt("height", -1).takeIf { it > 0 },
                relativePath = json.optString("relativePath").takeIf { it.isNotBlank() },
                sortTimestampMillis = json.optLong("sortTimestampMillis", 0L)
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

    private fun encryptedFileForId(mediaId: String): File = encryptedFileForToken(fileToken(mediaId))

    private fun encryptedFileForToken(token: String): File {
        val cleanToken = cleanToken(token)
        return File(vaultDir, "$cleanToken.ngv")
    }

    private fun encryptedPreviewFileForToken(token: String): File {
        val cleanToken = cleanToken(token)
        return File(vaultDir, "$cleanToken.ngp")
    }

    private fun cleanToken(token: String): String {
        return token.filter { it.isLetterOrDigit() || it == '_' || it == '-' }
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
        check(!vaultContainsEncryptedMedia()) {
            "The Locked Media key is unavailable while encrypted media still exists"
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, AndroidKeyStore)
        val keySpecBuilder = KeyGenParameterSpec.Builder(
            KeyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setKeySize(256)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            keySpecBuilder.setUnlockedDeviceRequired(true)
        }
        keyGenerator.init(keySpecBuilder.build())
        return keyGenerator.generateKey()
    }

    private fun vaultContainsEncryptedMedia(): Boolean {
        return vaultDir.listFiles()?.any { file ->
            (file.extension == "ngv" || file.extension == "ngp") &&
                file.length() > MinimumEncryptedFileBytes
        } == true
    }

    companion object {
        private const val VaultDirName = "locked_media_vault"
        private const val KeyAlias = "native_gallery_locked_media_v1"
        private const val AndroidKeyStore = "AndroidKeyStore"
        private const val CipherTransformation = "AES/GCM/NoPadding"
        private const val IvByteCount = 12
        private const val GcmTagBits = 128
        private const val MinimumEncryptedFileBytes = IvByteCount + (GcmTagBits / 8)
        private const val DefaultBufferSize = 64 * 1024
        private const val PreviewSize = 512
        private const val PreviewJpegQuality = 88
        private const val PreviewMimeType = "image/jpeg"
        private const val MetadataTokensKey = "metadata_tokens"

        const val PreviewMigrationDirName = "locked_media_preview_migration"
        const val VaultMimeTypeQuery = "mime"
        const val VaultPreviewPath = "preview"
        fun vaultAuthority(context: Context): String = "${context.packageName}.vault"
    }
}