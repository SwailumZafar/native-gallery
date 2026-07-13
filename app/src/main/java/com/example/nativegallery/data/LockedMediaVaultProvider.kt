package com.example.nativegallery.data

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileNotFoundException

class LockedMediaVaultProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun getType(uri: Uri): String = "application/octet-stream"

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

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        if (mode.contains('w')) throw FileNotFoundException("Locked media is read-only")
        val appContext = context?.applicationContext ?: throw FileNotFoundException("Missing context")
        val token = uri.lastPathSegment?.takeIf { it.isNotBlank() }
            ?: throw FileNotFoundException("Missing locked media token")
        val repository = LockedMediaVaultRepository(appContext)
        if (!repository.hasEncryptedToken(token)) {
            throw FileNotFoundException("Locked media token not found")
        }

        val tempDir = File(appContext.cacheDir, "locked_media_vault_open").apply { mkdirs() }
        tempDir.listFiles()?.forEach { staleFile -> staleFile.delete() }
        val tempFile = File.createTempFile("locked_media_", ".tmp", tempDir)
        val decrypted = tempFile.outputStream().use { output ->
            repository.decryptTokenTo(token, output)
        }
        if (!decrypted || tempFile.length() == 0L) {
            tempFile.delete()
            throw FileNotFoundException("Locked media token could not be decrypted")
        }

        return ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY).also {
            tempFile.delete()
        }
    }
}