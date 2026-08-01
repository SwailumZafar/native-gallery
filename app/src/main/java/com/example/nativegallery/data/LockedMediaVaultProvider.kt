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
import android.util.Size
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch

class LockedMediaVaultProvider : ContentProvider() {
    private var vaultRepository: LockedMediaVaultRepository? = null

    override fun onCreate(): Boolean {
        val appContext = context?.applicationContext ?: return false
        vaultRepository = LockedMediaVaultRepository(appContext)
        scheduleSessionCacheClear(appContext)
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
        @Suppress("DEPRECATION")
        val requestedSize = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            opts?.get(ContentResolver.EXTRA_SIZE) as? Size
        } else null
        val thumbnailRequested = requestedSize != null &&
            LockedMediaVaultPolicy.shouldServeEncryptedPreview(
                requestedSize.width, requestedSize.height
            )
        val token = uri.lastPathSegment
        val appContext = context?.applicationContext
        if (
            thumbnailRequested &&
            token != null &&
            appContext != null &&
            uri.pathSegments.firstOrNull() != LockedMediaVaultRepository.VaultPreviewPath &&
            vaultRepository?.hasEncryptedToken(token, preview = true) == true
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
        awaitScheduledSessionCacheClear()
        val token = uri.lastPathSegment?.takeIf { it.isNotBlank() }
            ?: throw FileNotFoundException("Missing locked media token")
        val preview = uri.pathSegments.firstOrNull() == LockedMediaVaultRepository.VaultPreviewPath
        val repository = vaultRepository ?: LockedMediaVaultRepository(appContext)
        val readyFile = ensureSessionFile(appContext, repository, token, preview)
        return ParcelFileDescriptor.open(readyFile, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    companion object {
        private const val SessionCacheDir = "locked_media_vault_open"
        private val cacheLock = Any()
        private val sessionLocks = ConcurrentHashMap<String, Any>()

        @Volatile
        private var scheduledCleanupLatch: CountDownLatch? = null

        /**
         * Decrypts the full-quality vault payload into the existing private session cache before a
         * decoder or player asks for it. Call only while the authenticated Locked Media session is
         * active; [clearSessionCache] removes the prepared plaintext when that session closes.
         */
        fun prepareFullQualityRead(context: Context, uri: Uri): Boolean {
            val appContext = context.applicationContext
            if (uri.authority != LockedMediaVaultRepository.vaultAuthority(appContext)) return false
            if (uri.pathSegments.firstOrNull() == LockedMediaVaultRepository.VaultPreviewPath) return false
            val token = uri.lastPathSegment?.takeIf { it.isNotBlank() } ?: return false
            awaitScheduledSessionCacheClear()
            return runCatching {
                ensureSessionFile(
                    context = appContext,
                    repository = LockedMediaVaultRepository(appContext),
                    token = token,
                    preview = false
                )
                true
            }.getOrDefault(false)
        }

        private fun ensureSessionFile(
            context: Context,
            repository: LockedMediaVaultRepository,
            token: String,
            preview: Boolean
        ): File {
            if (!repository.hasEncryptedToken(token, preview)) {
                throw FileNotFoundException("Locked media token not found")
            }
            val sessionKey = if (preview) "preview:$token" else "media:$token"
            val sessionLock = sessionLocks.computeIfAbsent(sessionKey) { Any() }
            return synchronized(sessionLock) {
                val readyFile = sessionFile(context, token, preview)
                if (!readyFile.exists() || readyFile.length() == 0L) {
                    decryptSessionFile(repository, token, preview, readyFile)
                }
                readyFile
            }
        }

        private fun decryptSessionFile(
            repository: LockedMediaVaultRepository,
            token: String,
            preview: Boolean,
            readyFile: File
        ) {
            val partialFile = File(readyFile.parentFile, "${readyFile.name}.partial")
            partialFile.delete()
            val decrypted = partialFile.outputStream().use { output ->
                repository.decryptTokenTo(token, output, preview)
            }
            if (!decrypted || partialFile.length() == 0L) {
                partialFile.delete()
                throw FileNotFoundException("Locked media token could not be decrypted")
            }
            if (readyFile.exists()) readyFile.delete()
            if (!partialFile.renameTo(readyFile)) {
                partialFile.copyTo(readyFile, overwrite = true)
                partialFile.delete()
            }
        }

        fun clearSessionCache(context: Context) {
            awaitScheduledSessionCacheClear()
            clearSessionCacheFiles(context.applicationContext)
        }

        private fun scheduleSessionCacheClear(context: Context) {
            val latch = CountDownLatch(1)
            scheduledCleanupLatch = latch
            Thread(
                {
                    try {
                        clearSessionCacheFiles(context)
                    } finally {
                        latch.countDown()
                        if (scheduledCleanupLatch === latch) scheduledCleanupLatch = null
                    }
                },
                "LockedMediaCacheCleanup"
            ).apply {
                isDaemon = true
                priority = Thread.MIN_PRIORITY
                start()
            }
        }

        private fun awaitScheduledSessionCacheClear() {
            scheduledCleanupLatch?.await()
        }

        private fun clearSessionCacheFiles(context: Context) {
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