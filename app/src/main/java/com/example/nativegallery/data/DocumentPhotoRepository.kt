package com.example.nativegallery.data

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Size
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Immutable
import com.example.nativegallery.model.MediaItem
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Immutable
data class DocumentPhotoMatch(
    val mediaItem: MediaItem,
    val category: DocumentPhotoCategory,
    val recognizedText: String,
    val lineCount: Int
)

@Immutable
data class DocumentPhotoScanProgress(
    val matches: List<DocumentPhotoMatch>,
    val scannedCount: Int,
    val totalCount: Int,
    val scanning: Boolean
)

class DocumentPhotoRepository(context: Context) {
    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver
    private val database = DocumentPhotoIndexDatabase(appContext)
    private val legacyIndexPreferences = appContext.getSharedPreferences(
        LEGACY_INDEX_PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )
    private val migrationPreferences = appContext.getSharedPreferences(
        MIGRATION_PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    suspend fun scan(
        mediaItems: List<MediaItem>,
        onProgress: (DocumentPhotoScanProgress) -> Unit
    ) = withContext(Dispatchers.IO) {
        migrateLegacyStorage()
        val photos = mediaItems
            .asSequence()
            .filterNot { it.isVideo }
            .distinctBy { it.id }
            .sortedByDescending { it.sortTimestampMillis }
            .toList()
        val validIds = photos.mapTo(mutableSetOf()) { it.id }
        val recordsById = database.loadAll()
        database.remove(recordsById.keys - validIds)

        val matches = mutableListOf<DocumentPhotoMatch>()
        val pending = mutableListOf<MediaItem>()
        var scannedCount = 0

        photos.forEach { item ->
            val record = recordsById[item.id]
            if (record != null && record.fingerprint == fingerprint(item)) {
                scannedCount += 1
                record.category?.let { category ->
                    matches += DocumentPhotoMatch(
                        mediaItem = item,
                        category = category,
                        recognizedText = record.recognizedText,
                        lineCount = record.lineCount
                    )
                }
            } else {
                pending += item
            }
        }
        matches.sortByDescending { it.mediaItem.sortTimestampMillis }
        onProgress(
            DocumentPhotoScanProgress(
                matches = matches.toList(),
                scannedCount = scannedCount,
                totalCount = photos.size,
                scanning = pending.isNotEmpty()
            )
        )
        if (pending.isEmpty()) return@withContext

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val bufferedRecords = mutableListOf<StoredDocumentPhotoRecord>()
        try {
            pending.forEachIndexed { index, item ->
                coroutineContext.ensureActive()
                val classification = try {
                    recognizePhoto(item, recognizer)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    null
                }
                bufferedRecords += StoredDocumentPhotoRecord(
                    mediaId = item.id,
                    fingerprint = fingerprint(item),
                    category = classification?.category,
                    recognizedText = classification?.recognizedText.orEmpty(),
                    lineCount = classification?.lineCount ?: 0
                )
                if (
                    bufferedRecords.size >= DATABASE_WRITE_BATCH_SIZE ||
                    index == pending.lastIndex
                ) {
                    database.upsert(bufferedRecords)
                    bufferedRecords.clear()
                }

                if (classification != null) {
                    matches += DocumentPhotoMatch(
                        mediaItem = item,
                        category = classification.category,
                        recognizedText = classification.recognizedText,
                        lineCount = classification.lineCount
                    )
                    matches.sortByDescending { it.mediaItem.sortTimestampMillis }
                }
                scannedCount += 1
                val shouldPublishProgress =
                    scannedCount == photos.size ||
                        scannedCount % PROGRESS_BATCH_SIZE == 0
                if (shouldPublishProgress) {
                    matches.sortByDescending { it.mediaItem.sortTimestampMillis }
                    onProgress(
                        DocumentPhotoScanProgress(
                            matches = matches.toList(),
                            scannedCount = scannedCount,
                            totalCount = photos.size,
                            scanning = scannedCount < photos.size
                        )
                    )
                }
            }
        } finally {
            recognizer.close()
        }
    }

    suspend fun invalidate(mediaIds: Collection<String>) = withContext(Dispatchers.IO) {
        database.remove(mediaIds.toSet())
    }

    private suspend fun recognizePhoto(
        item: MediaItem,
        recognizer: TextRecognizer
    ): DocumentPhotoClassification? {
        val bitmap = loadAnalysisBitmap(item) ?: return null
        return try {
            val result = recognizer.process(InputImage.fromBitmap(bitmap, 0)).awaitResult()
            classifyDocumentPhoto(
                text = result.text,
                lineCount = result.textBlocks.sumOf { it.lines.size },
                blockCount = result.textBlocks.size
            )
        } finally {
            bitmap.recycle()
        }
    }

    private fun loadAnalysisBitmap(item: MediaItem): Bitmap? {
        item.imageRes?.let { resourceId ->
            return BitmapFactory.decodeResource(appContext.resources, resourceId)
        }
        val uri = item.contentUri ?: return null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            loadSystemThumbnail(uri)?.let { return it }
        }
        return decodeSampledBitmap(resolver, uri, MAX_ANALYSIS_EDGE)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun loadSystemThumbnail(uri: android.net.Uri): Bitmap? = runCatching {
        resolver.loadThumbnail(uri, Size(MAX_ANALYSIS_EDGE, MAX_ANALYSIS_EDGE), null)
    }.getOrNull()

    private fun migrateLegacyStorage() {
        if (!migrationPreferences.getBoolean(LEGACY_INDEX_MIGRATION_KEY, false)) {
            val records = legacyIndexPreferences.all.mapNotNull { (key, value) ->
                if (!key.startsWith(LEGACY_RECORD_PREFIX)) return@mapNotNull null
                val encoded = value as? String ?: return@mapNotNull null
                runCatching {
                    val json = JSONObject(encoded)
                    StoredDocumentPhotoRecord(
                        mediaId = key.removePrefix(LEGACY_RECORD_PREFIX),
                        fingerprint = json.getString("fingerprint"),
                        category = json.optString("category")
                            .takeIf { it.isNotBlank() }
                            ?.let(DocumentPhotoCategory::valueOf),
                        recognizedText = json.optString("text"),
                        lineCount = json.optInt("lineCount")
                    )
                }.getOrNull()
            }
            database.upsert(records)
            legacyIndexPreferences.edit().clear().apply()
            migrationPreferences.edit()
                .putBoolean(LEGACY_INDEX_MIGRATION_KEY, true)
                .apply()
        }

        if (!migrationPreferences.getBoolean(LEGACY_FILE_BROWSER_REMOVAL_KEY, false)) {
            resolver.persistedUriPermissions.forEach { permission ->
                if (permission.isReadPermission) {
                    runCatching {
                        resolver.releasePersistableUriPermission(
                            permission.uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }
                }
            }
            appContext.getSharedPreferences(
                LEGACY_FILE_BROWSER_PREFERENCES_NAME,
                Context.MODE_PRIVATE
            ).edit().clear().apply()
            migrationPreferences.edit()
                .putBoolean(LEGACY_FILE_BROWSER_REMOVAL_KEY, true)
                .apply()
        }
    }

    private fun fingerprint(item: MediaItem): String = listOf(
        CLASSIFIER_VERSION,
        item.id,
        item.contentUri?.toString().orEmpty(),
        item.sortTimestampMillis,
        item.fileSizeBytes ?: 0L,
        item.width ?: 0,
        item.height ?: 0,
        item.mimeType.orEmpty()
    ).joinToString(separator = "|")

    private companion object {
        const val LEGACY_INDEX_PREFERENCES_NAME = "native_gallery_document_photos"
        const val LEGACY_FILE_BROWSER_PREFERENCES_NAME = "native_gallery_documents"
        const val MIGRATION_PREFERENCES_NAME = "native_gallery_document_photo_migrations"
        const val LEGACY_RECORD_PREFIX = "record_"
        const val LEGACY_INDEX_MIGRATION_KEY = "sqlite_index_migrated"
        const val LEGACY_FILE_BROWSER_REMOVAL_KEY = "file_browser_removed"
        const val CLASSIFIER_VERSION = 2
        const val MAX_ANALYSIS_EDGE = 1280
        const val PROGRESS_BATCH_SIZE = 20
        const val DATABASE_WRITE_BATCH_SIZE = 20
    }
}

private data class StoredDocumentPhotoRecord(
    val mediaId: String,
    val fingerprint: String,
    val category: DocumentPhotoCategory?,
    val recognizedText: String,
    val lineCount: Int
)

private class DocumentPhotoIndexDatabase(
    context: Context
) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    init {
        setWriteAheadLoggingEnabled(true)
    }

    override fun onCreate(database: SQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE $TABLE_NAME (
                media_id TEXT PRIMARY KEY NOT NULL,
                fingerprint TEXT NOT NULL,
                category TEXT NOT NULL,
                recognized_text TEXT NOT NULL,
                line_count INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(
        database: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        database.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(database)
    }

    fun loadAll(): Map<String, StoredDocumentPhotoRecord> {
        val records = LinkedHashMap<String, StoredDocumentPhotoRecord>()
        readableDatabase.query(
            TABLE_NAME,
            COLUMNS,
            null,
            null,
            null,
            null,
            null
        ).use { cursor ->
            val mediaIdIndex = cursor.getColumnIndexOrThrow("media_id")
            val fingerprintIndex = cursor.getColumnIndexOrThrow("fingerprint")
            val categoryIndex = cursor.getColumnIndexOrThrow("category")
            val textIndex = cursor.getColumnIndexOrThrow("recognized_text")
            val lineCountIndex = cursor.getColumnIndexOrThrow("line_count")
            while (cursor.moveToNext()) {
                val category = cursor.getString(categoryIndex)
                    .takeIf { it.isNotBlank() }
                    ?.let { encoded -> runCatching { DocumentPhotoCategory.valueOf(encoded) }.getOrNull() }
                val record = StoredDocumentPhotoRecord(
                    mediaId = cursor.getString(mediaIdIndex),
                    fingerprint = cursor.getString(fingerprintIndex),
                    category = category,
                    recognizedText = cursor.getString(textIndex),
                    lineCount = cursor.getInt(lineCountIndex)
                )
                records[record.mediaId] = record
            }
        }
        return records
    }

    fun upsert(records: Collection<StoredDocumentPhotoRecord>) {
        if (records.isEmpty()) return
        val database = writableDatabase
        database.beginTransaction()
        try {
            records.forEach { record ->
                val values = ContentValues().apply {
                    put("media_id", record.mediaId)
                    put("fingerprint", record.fingerprint)
                    put("category", record.category?.name.orEmpty())
                    put("recognized_text", record.recognizedText)
                    put("line_count", record.lineCount)
                }
                database.insertWithOnConflict(
                    TABLE_NAME,
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_REPLACE
                )
            }
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
    }

    fun remove(mediaIds: Set<String>) {
        if (mediaIds.isEmpty()) return
        val database = writableDatabase
        database.beginTransaction()
        try {
            mediaIds.forEach { mediaId ->
                database.delete(TABLE_NAME, "media_id = ?", arrayOf(mediaId))
            }
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
    }

    private companion object {
        const val DATABASE_NAME = "document_photos.db"
        const val DATABASE_VERSION = 1
        const val TABLE_NAME = "document_photos"
        val COLUMNS = arrayOf(
            "media_id",
            "fingerprint",
            "category",
            "recognized_text",
            "line_count"
        )
    }
}

private fun decodeSampledBitmap(
    resolver: ContentResolver,
    uri: android.net.Uri,
    maxEdge: Int
): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, bounds)
    }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sampleSize = 1
    while (bounds.outWidth / sampleSize > maxEdge || bounds.outHeight / sampleSize > maxEdge) {
        sampleSize *= 2
    }
    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    return resolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, options)
    }
}

private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { result ->
        if (continuation.isActive) continuation.resume(result)
    }
    addOnFailureListener { error ->
        if (continuation.isActive) continuation.resumeWithException(error)
    }
    addOnCanceledListener {
        continuation.cancel()
    }
}
