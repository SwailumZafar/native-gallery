package com.example.nativegallery.data

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.ConcurrentHashMap

class LockedMediaVaultProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        context?.applicationContext?.let(::clearSessionCache)
        return true
    }

    override fun getType(uri: Uri): String {
        return uri.getQueryParameter(LockedMediaVaultRepository.VaultMimeTypeQuery)
            ?.takeIf { it.contains('/') }
            ?: "application/octet-stream"
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0

    override fun openTypedAssetFile(
        uri: Uri,
        mimeTypeFilter: String,
        opts: Bundle?
    ): AssetFileDescriptor? {
        val thumbnailRequested = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            opts?.containsKey(ContentResolver.EXTRA_SIZE) == true
        val token = uri.lastPathSegment
        val appContext = context?.applicationContext
        if (
            thumbnailRequested &&
            token != null &&
            appContext != null &&
            uri.pathSegments.firstOrNull() != LockedMediaVaultRepository.VaultPreviewPath &&
            LockedMediaVaultRepository(appContext).hasEncryptedToken(token, preview = true)
        ) {
            val previewUri = Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(uri.authority)
                .appendPath(LockedMediaVaultRepository.VaultPreviewPath)
                .appendPath(token)
                .appendQueryParameter(LockedMediaVaultRepository.VaultMimeTypeQuery, "image/jpeg")
                .build()
            val descriptor = openFile(previewUri, "r")
            return AssetFileDescriptor(descriptor, 0, AssetFileDescriptor.UNKNOWN_LENGTH)
        }
        return super.openTypedAssetFile(uri, mimeTypeFilter, opts)
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        if (mode.contains('w')) throw FileNotFoundException("Locked media is read-only")
        val appContext = context?.applicationContext ?: throw FileNotFoundException("Missing context")
        val token = uri.lastPathSegment?.takeIf { it.isNotBlank() }
            ?: throw FileNotFoundException("Missing locked media token")
        val preview = uri.pathSegments.firstOrNull() == LockedMediaVaultRepository.VaultPreviewPath
        val repository = LockedMediaVaultRepository(appContext)
        if (!repository.hasEncryptedToken(token, preview)) {
            throw FileNotFoundException("Locked media token not found")
        }

        val sessionKey = if (preview) "preview:$token" else "media:$token"
        val sessionLock = sessionLocks.computeIfAbsent(sessionKey) { Any() }
        return synchronized(sessionLock) {
            val sessionFile = sessionFile(appContext, token, preview)
            if (!sessionFile.exists() || sessionFile.length() == 0L) {
                val partialFile = File(sessionFile.parentFile, "${sessionFile.name}.partial")
                partialFile.delete()
                val decrypted = partialFile.outputStream().use { output ->
                    repository.decryptTokenTo(token, output, preview)
                }
                if (!decrypted || partialFile.length() == 0L) {
                    partialFile.delete()
                    throw FileNotFoundException("Locked media token could not be decrypted")
                }
                if (sessionFile.exists()) sessionFile.delete()
                if (!partialFile.renameTo(sessionFile)) {
                    partialFile.copyTo(sessionFile, overwrite = true)
                    partialFile.delete()
                }
            }
            ParcelFileDescriptor.open(sessionFile, ParcelFileDescriptor.MODE_READ_ONLY)
        }
    }

    companion object {
        private const val SessionCacheDir = "locked_media_vault_open"
        private val cacheLock = Any()
        private val sessionLocks = ConcurrentHashMap<String, Any>()

        fun clearSessionCache(context: Context) {
            synchronized(cacheLock) {
                File(context.applicationContext.cacheDir, SessionCacheDir)
                    .listFiles()
                    ?.forEach { it.delete() }
                File(
                    context.applicationContext.cacheDir,
                    LockedMediaVaultRepository.PreviewMigrationDirName
                ).listFiles()?.forEach { it.delete() }
                sessionLocks.clear()
            }
        }

        fun clearCachedToken(context: Context, token: String) {
            synchronized(cacheLock) {
                sessionFile(context.applicationContext, token, preview = false).delete()
                sessionFile(context.applicationContext, token, preview = true).delete()
            }
        }

        private fun sessionFile(context: Context, token: String, preview: Boolean): File {
            val cleanToken = token.filter { it.isLetterOrDigit() || it == '_' || it == '-' }
            if (cleanToken.isBlank()) throw FileNotFoundException("Invalid locked media token")
            val cacheDir = File(context.applicationContext.cacheDir, SessionCacheDir).apply { mkdirs() }
            val prefix = if (preview) "preview_" else "media_"
            return File(cacheDir, "$prefix$cleanToken.cache")
        }
    }
}