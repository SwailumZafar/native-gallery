package com.example.nativegallery.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.SystemClock
import com.example.nativegallery.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.coroutines.coroutineContext

data class ScreenshotEntry(val media: MediaItem, val text: String = "", val category: ScreenshotCategory = ScreenshotCategory.Other,
    val indexed: Boolean = false, val pinned: Boolean = false, val done: Boolean = false, val followUp: LocalDate? = null,
    val manualCategory: ScreenshotCategory? = null) {
    val displayCategory: ScreenshotCategory get() = manualCategory ?: category
}

class ScreenshotRepository(context: Context, databaseName: String = "useful_screenshots.db") {
    private val context = context.applicationContext
    private val database = ScreenshotDatabase(this.context, databaseName)

    suspend fun cached(media: List<MediaItem>): List<ScreenshotEntry> = withContext(Dispatchers.IO) {
        val records = database.read()
        media.filter(::isScreenshotMedia).distinctBy { it.id }.sortedByDescending { it.sortTimestampMillis }.map { item ->
            val record = records[item.id]
            val valid = record?.fingerprint == fingerprint(item)
            ScreenshotEntry(item, if (valid) record?.text.orEmpty() else "", if (valid) record!!.category else ScreenshotCategory.Other,
                valid, record?.pinned ?: false, record?.done ?: false, record?.followUp, record?.manualCategory)
        }
    }

    suspend fun scan(media: List<MediaItem>, publish: (List<ScreenshotEntry>, Int, Int, Int) -> Unit) = withContext(PhotoAnalysisWork.dispatcher) {
        val entries = cached(media).toMutableList()
        var checked = entries.count { it.indexed }
        var failed = 0
        var lastPublished = SystemClock.elapsedRealtime()
        publish(entries.toList(), checked, entries.size, failed)
        if (checked == entries.size) return@withContext
        val writes = mutableListOf<Pair<ScreenshotEntry, String>>()
        fun flush() { database.write(writes); writes.clear() }
        try {
            var published = entries.toList()
            scanPhotoText(entries.indices.filterNot { entries[it].indexed }, { index ->
                decodeUsefulPhoto(context, requireNotNull(entries[index].media.contentUri), 1280)
            }) { index, result ->
                coroutineContext.ensureActive()
                val next = result.getOrNull()?.let { text -> entries[index].copy(text = text.text, category = screenshotCategory(text.text), indexed = true) }
                if (next != null) {
                    writes += next to fingerprint(next.media); entries[index] = next
                    if (writes.size >= 16) flush()
                } else failed++
                checked++
                if (SystemClock.elapsedRealtime() - lastPublished >= 150 || checked == entries.size) {
                    published = entries.toList(); lastPublished = SystemClock.elapsedRealtime()
                }
                publish(published, checked, entries.size, failed)
            }
        } finally { withContext(NonCancellable) { flush() } }
        publish(entries.toList(), checked, entries.size, failed)
    }

    suspend fun annotate(entry: ScreenshotEntry) = withContext(Dispatchers.IO) {
        database.annotate(entry)
    }

    suspend fun clearIndex() = withContext(Dispatchers.IO) {
        database.writableDatabase.execSQL("UPDATE screenshots SET fingerprint='', text='', category='Other'")
    }
    fun close() = database.close()

    private fun fingerprint(item: MediaItem) = "${item.id}:${item.contentUri}:${item.dateModifiedMillis}:${item.fileSizeBytes}:${item.sortTimestampMillis}"
}

private data class ScreenshotRecord(val fingerprint: String, val text: String, val category: ScreenshotCategory,
    val pinned: Boolean, val done: Boolean, val followUp: LocalDate?, val manualCategory: ScreenshotCategory?)

private class ScreenshotDatabase(context: Context, name: String) : SQLiteOpenHelper(context, name, null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE screenshots (id TEXT PRIMARY KEY, fingerprint TEXT NOT NULL DEFAULT '', text TEXT NOT NULL DEFAULT '', category TEXT NOT NULL DEFAULT 'Other', pinned INTEGER NOT NULL DEFAULT 0, done INTEGER NOT NULL DEFAULT 0, followup TEXT, user_category TEXT)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    fun read(): Map<String, ScreenshotRecord> = buildMap {
        readableDatabase.query("screenshots", null, null, null, null, null, null).use { cursor ->
            while (cursor.moveToNext()) {
                fun value(name: String) = cursor.getString(cursor.getColumnIndexOrThrow(name))
                put(value("id"), ScreenshotRecord(value("fingerprint"), value("text"),
                    ScreenshotCategory.entries.firstOrNull { it.name == value("category") } ?: ScreenshotCategory.Other,
                    value("pinned") == "1", value("done") == "1", runCatching { LocalDate.parse(value("followup")) }.getOrNull(),
                    ScreenshotCategory.entries.firstOrNull { it.name == value("user_category") }))
            }
        }
    }
    fun write(records: List<Pair<ScreenshotEntry, String>>) {
        if (records.isEmpty()) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            records.forEach { (entry, fingerprint) ->
            // Update OCR columns only; an annotation edited while scanning must survive.
            db.insertWithOnConflict("screenshots", null, ContentValues().apply { put("id", entry.media.id) }, SQLiteDatabase.CONFLICT_IGNORE)
            db.update("screenshots", ContentValues().apply {
                put("fingerprint", fingerprint); put("text", entry.text); put("category", entry.category.name)
            }, "id=?", arrayOf(entry.media.id))
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }
    fun annotate(entry: ScreenshotEntry) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.insertWithOnConflict("screenshots", null, ContentValues().apply { put("id", entry.media.id) }, SQLiteDatabase.CONFLICT_IGNORE)
            db.update("screenshots", ContentValues().apply {
                put("pinned", if (entry.pinned) 1 else 0); put("done", if (entry.done) 1 else 0)
                put("followup", entry.followUp?.toString())
                put("user_category", entry.manualCategory?.name)
            }, "id=?", arrayOf(entry.media.id))
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }
}
