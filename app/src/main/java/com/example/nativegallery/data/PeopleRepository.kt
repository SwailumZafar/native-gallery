package com.example.nativegallery.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import android.util.AtomicFile
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Build
import android.util.Size as AndroidSize
import com.example.nativegallery.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel
import android.os.SystemClock
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.core.Core
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.FaceDetectorYN
import org.opencv.objdetect.FaceRecognizerSF
import java.io.File
import java.util.UUID
import kotlin.coroutines.coroutineContext
import kotlin.math.sqrt

data class PersonGroup(val id: String, val thumbnail: Uri, val photos: List<MediaItem>)
private data class IndexedFace(val key: String, val vector: FloatArray)
private data class IndexedPhoto(val fingerprint: String, val faces: List<IndexedFace>)

internal fun faceSimilarity(a: FloatArray, b: FloatArray): Float {
    if (a.size != b.size || a.isEmpty()) return -1f
    val aa = a.sumOf { (it * it).toDouble() }
    val bb = b.sumOf { (it * it).toDouble() }
    if (aa == 0.0 || bb == 0.0) return -1f
    return (a.indices.sumOf { (a[it] * b[it]).toDouble() } / sqrt(aa * bb)).toFloat()
}

internal fun normalizeFaceVector(vector: FloatArray): FloatArray {
    val norm = sqrt(vector.sumOf { it.toDouble() * it }).toFloat()
    return if (norm > 0f && norm.isFinite()) FloatArray(vector.size) { vector[it] / norm } else FloatArray(vector.size)
}

internal fun normalizedFaceSimilarity(a: FloatArray, b: FloatArray): Float {
    if (a.size != b.size || a.isEmpty()) return -1f
    var score = 0f
    for (i in a.indices) score += a[i] * b[i]
    return score
}

/** Offline face embeddings and crops stay in app-private, backup-excluded storage. */
class PeopleRepository(context: Context) {
    private val context = context.applicationContext
    private val directory = File(this.context.noBackupFilesDir, "people-v1").apply { mkdirs() }
    private val index = AtomicFile(File(directory, "index.json"))
    private val groupCache = AtomicFile(File(directory, "groups.json"))
    private val mutex = Mutex()
    private val database by lazy { PeopleDatabase(context, File(directory, "index.db").absolutePath) }
    private val preferences = this.context.getSharedPreferences("people", Context.MODE_PRIVATE)
    var enabled: Boolean
        get() = preferences.getBoolean("enabled", false)
        set(value) { preferences.edit().putBoolean("enabled", value).apply() }

    suspend fun clear() = mutex.withLock { withContext(Dispatchers.IO) {
        index.delete()
        groupCache.delete()
        database.writableDatabase.delete("photos", null, null)
        directory.listFiles()?.filter { it.extension == "jpg" }?.forEach { it.delete() }
    } }

    /** Browsing reads a small membership cache, without loading embeddings or running inference. */
    suspend fun cachedGroups(media: List<MediaItem>): List<PersonGroup> = mutex.withLock { withContext(Dispatchers.IO) {
        val byId = media.associateBy { it.id }
        runCatching {
            val groups = JSONArray(groupCache.openRead().bufferedReader().use { it.readText() })
            (0 until groups.length()).mapNotNull { position ->
                val group = groups.getJSONObject(position)
                val cover = group.getString("cover")
                if (File(cover).name != cover || !cover.endsWith(".jpg")) return@mapNotNull null
                val entries = group.getJSONArray("photos")
                val photos = (0 until entries.length()).mapNotNull { i ->
                    val entry = entries.getJSONObject(i)
                    byId[entry.getString("id")]?.takeIf { fingerprint(it) == entry.getString("revision") }
                }
                // A removed/hidden cover must never survive in the face row.
                if (photos.isEmpty() || photos.size != entries.length() || !File(directory, cover).isFile) null
                else PersonGroup(group.getString("id"), Uri.fromFile(File(directory, cover)), photos)
            }
        }.getOrDefault(emptyList())
    } }

    private fun saveGroups(groups: List<PersonGroup>) {
        val json = JSONArray()
        groups.forEach { group ->
            val photos = JSONArray()
            group.photos.forEach { photos.put(JSONObject().put("id", it.id).put("revision", fingerprint(it))) }
            json.put(JSONObject().put("id", group.id).put("cover", File(requireNotNull(group.thumbnail.path)).name).put("photos", photos))
        }
        val stream = groupCache.startWrite()
        try { stream.write(json.toString().toByteArray(Charsets.UTF_8)); groupCache.finishWrite(stream) }
        catch (error: Exception) { groupCache.failWrite(stream); throw error }
    }

    suspend fun scan(media: List<MediaItem>, progress: (List<PersonGroup>, Int, Int) -> Unit) = mutex.withLock { withContext(PhotoAnalysisWork.dispatcher) {
        PhotoAnalysisWork.awaitPeopleAllowed()
        val photos = media.filter { !it.isVideo && it.contentUri != null }.distinctBy { it.id }
            .sortedByDescending { it.sortTimestampMillis }
        val records = load().toMutableMap()
        val byId = photos.associateBy { it.id }
        val obsolete = records.filter { (id, record) -> byId[id]?.let { fingerprint(it) != record.fingerprint } ?: true }.keys
        database.remove(obsolete)
        records.keys.removeAll(obsolete)
        prune(records)
        val groupIndex = Groups(photos)
        records.entries.forEachIndexed { position, (id, record) ->
            if (position % 8 == 0) PhotoAnalysisWork.awaitPeopleAllowed()
            byId[id]?.let { groupIndex.add(it, record.faces) }
        }
        saveGroups(groupIndex.snapshot())
        progress(groupIndex.snapshot(), records.size, photos.size)
        if (records.size == photos.size) return@withContext
        PhotoAnalysisWork.awaitPeopleAllowed()
        check(OpenCVLoader.initLocal()) { "Face recognition could not start." }
        Core.setNumThreads(1)
        fun model(name: String): String {
            val file = File(directory, name)
            if (!file.exists()) {
                val temporary = File(directory, "$name.tmp")
                context.assets.open("people/$name").use { source -> temporary.outputStream().use { source.copyTo(it) } }
                check(temporary.renameTo(file)) { "Could not prepare face model" }
            }
            return file.absolutePath
        }
        val detector = FaceDetectorYN.create(model("yunet.onnx"), "", Size(640.0, 640.0), 0.9f)
        // Most gallery images contain no face. Load the larger embedding model only when
        // detection finds one, rather than paying its setup cost for document-only batches.
        val recognizer by lazy { FaceRecognizerSF.create(model("sface.onnx"), "") }
        var failed = 0
        val changed = linkedMapOf<String, IndexedPhoto>()
        var lastGroups = groupIndex.snapshot()
        var groupsChanged = false
        var lastPublished = SystemClock.elapsedRealtime()
        try {
            coroutineScope {
                // Decode the next photo while native inference handles this one. Capacity one bounds
                // memory and prevents disk work from running far ahead of an interaction pause.
                val decoded = Channel<Pair<MediaItem, Result<Bitmap>>>(1,
                    onUndeliveredElement = { it.second.getOrNull()?.recycle() })
                val pending = photos.filterNot { it.id in records }
                val producer = launch {
                    try {
                        for (photo in pending) {
                            PhotoAnalysisWork.awaitPeopleAllowed()
                            val result = try { Result.success(decode(photo)) }
                            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                            catch (error: Exception) { Result.failure(error) }
                            decoded.send(photo to result)
                        }
                    } finally { decoded.close() }
                }
                try {
                    for ((photo, decodedBitmap) in decoded) {
                        try {
                            val bitmap = decodedBitmap.getOrThrow()
                            val rgba = Mat(); val image = Mat(); val faces = Mat()
                            try {
                                PhotoAnalysisWork.awaitPeopleAllowed()
                                Utils.bitmapToMat(bitmap, rgba)
                                Imgproc.cvtColor(rgba, image, Imgproc.COLOR_RGBA2BGR)
                                detector.setInputSize(image.size())
                                detector.detect(image, faces)
                                val found = mutableListOf<IndexedFace>()
                                for (row in 0 until faces.rows()) {
                                    coroutineContext.ensureActive()
                                    PhotoAnalysisWork.awaitPeopleAllowed()
                                    val aligned = Mat(); val feature = Mat(); val box = faces.row(row)
                                    try {
                                        recognizer.alignCrop(image, box, aligned)
                                        recognizer.feature(aligned, feature)
                                        val vector = FloatArray(feature.total().toInt())
                                        feature.get(0, 0, vector)
                                        val key = UUID.randomUUID().toString()
                                        val crop = Bitmap.createBitmap(aligned.cols(), aligned.rows(), Bitmap.Config.ARGB_8888)
                                        val rgb = Mat()
                                        try {
                                            Imgproc.cvtColor(aligned, rgb, Imgproc.COLOR_BGR2RGBA)
                                            Utils.matToBitmap(rgb, crop)
                                            File(directory, "$key.jpg").outputStream().use { crop.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                                        } finally { crop.recycle(); rgb.release() }
                                        found += IndexedFace(key, vector)
                                    } finally { box.release(); aligned.release(); feature.release() }
                                }
                                records[photo.id] = IndexedPhoto(fingerprint(photo), found)
                                changed[photo.id] = records.getValue(photo.id)
                                groupIndex.add(photo, found)
                                groupsChanged = groupsChanged || found.isNotEmpty()
                            } finally { bitmap.recycle(); rgba.release(); image.release(); faces.release() }
                        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                        catch (_: Exception) { failed++ }
                        if ((records.size + failed) % 20 == 0) {
                            database.upsert(changed)
                            changed.clear()
                        }
                        val now = SystemClock.elapsedRealtime()
                        if (now - lastPublished >= 1500L || (lastGroups.isEmpty() && groupsChanged)) {
                            if (records.size + failed < photos.size) PhotoAnalysisWork.awaitPeopleAllowed()
                            if (groupsChanged) { lastGroups = groupIndex.snapshot(); groupsChanged = false }
                            progress(lastGroups, records.size + failed, photos.size)
                            lastPublished = now
                        }
                    }
                } finally { producer.cancel(); decoded.cancel() }
            }
        } finally {
            // Preserve completed work even when a library refresh cancels the remaining scan.
            withContext(NonCancellable) { database.upsert(changed); saveGroups(groupIndex.snapshot()) }
        }
        prune(records)
        progress(groupIndex.snapshot(), photos.size, photos.size)
        if (failed > 0) error("$failed photos could not be scanned. Tap Scan to retry.")
    } }

    private fun fingerprint(photo: MediaItem) = "${photo.contentUri}:${photo.dateModifiedMillis}:${photo.fileSizeBytes}"

    private fun decode(photo: MediaItem): Bitmap {
        val uri = requireNotNull(photo.contentUri)
        val resolver = context.contentResolver
        val width = photo.width ?: 0
        val height = photo.height ?: 0
        if (Build.VERSION.SDK_INT >= 29 && uri.scheme == "content" && width > 0 && height > 0) {
            val scale = minOf(1f, 640f / maxOf(width, height))
            val size = AndroidSize((width * scale).toInt().coerceAtLeast(1), (height * scale).toInt().coerceAtLeast(1))
            runCatching { resolver.loadThumbnail(uri, size, null) }.getOrNull()?.let { return it }
        }
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, options) }
        options.inSampleSize = 1
        while (maxOf(options.outWidth, options.outHeight) / options.inSampleSize > 1280) options.inSampleSize *= 2
        options.inJustDecodeBounds = false
        val source = resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, options) }
            ?: error("Photo unavailable")
        val exif = resolver.openInputStream(uri).use { stream -> stream?.let { ExifInterface(it) } }
        val matrix = Matrix()
        if (exif?.isFlipped == true) matrix.postScale(-1f, 1f)
        matrix.postRotate((exif?.rotationDegrees ?: 0).toFloat())
        val scale = minOf(1f, 640f / maxOf(source.width, source.height))
        matrix.postScale(scale, scale)
        val result = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        if (result !== source) source.recycle()
        return result
    }

    private inner class Groups(photos: List<MediaItem>) {
        private val index = FaceClusterIndex()
        private val byId = photos.associateBy { it.id }
        private val cachedGroups = mutableMapOf<String, PersonGroup>()

        fun add(photo: MediaItem, faces: List<IndexedFace>) {
            faces.forEach { face ->
                index.add(photo.id, face.key, face.vector)
            }
        }
        fun snapshot(): List<PersonGroup> {
            val clusters = index.clusters.sortedByDescending { it.photoIds.size }
            cachedGroups.keys.retainAll(clusters.map { it.key }.toSet())
            return clusters.map { cluster ->
                cachedGroups[cluster.key]?.takeIf { it.photos.size == cluster.photoIds.size }
                    ?: PersonGroup("person:${cluster.key}", Uri.fromFile(File(directory, "${cluster.key}.jpg")),
                        cluster.photoIds.mapNotNull(byId::get).sortedByDescending { it.sortTimestampMillis })
                        .also { cachedGroups[cluster.key] = it }
            }
        }
    }

    private fun load(): Map<String, IndexedPhoto> {
        if (!index.baseFile.exists()) return database.load()
        val legacy = loadLegacy()
        database.upsert(legacy)
        index.delete()
        return database.load()
    }

    private fun loadLegacy(): Map<String, IndexedPhoto> = try {
        val json = JSONObject(index.openRead().bufferedReader().use { it.readText() })
        json.keys().asSequence().associateWith { id ->
            val item = json.getJSONObject(id)
            val faces = item.getJSONArray("faces")
            IndexedPhoto(item.getString("fingerprint"), (0 until faces.length()).map { i ->
                val face = faces.getJSONObject(i); val vector = face.getJSONArray("vector")
                IndexedFace(face.getString("key"), FloatArray(vector.length()) { vector.getDouble(it).toFloat() })
            })
        }
    } catch (_: Exception) { emptyMap() }

    private fun prune(records: Map<String, IndexedPhoto>) {
        val keep = records.values.flatMap { it.faces }.mapTo(hashSetOf()) { "${it.key}.jpg" }
        directory.listFiles()?.filter { it.extension == "jpg" && it.name !in keep }?.forEach { it.delete() }
    }
}

private class PeopleDatabase(context: Context, path: String) : SQLiteOpenHelper(context, path, null, 1) {
    init { setWriteAheadLoggingEnabled(true) }
    override fun onCreate(db: SQLiteDatabase) { db.execSQL("CREATE TABLE photos (id TEXT PRIMARY KEY, fingerprint TEXT NOT NULL, faces TEXT NOT NULL)") }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    fun load(): Map<String, IndexedPhoto> {
        val result = linkedMapOf<String, IndexedPhoto>()
        readableDatabase.rawQuery("SELECT id, fingerprint, faces FROM photos ORDER BY rowid", null).use { cursor ->
            while (cursor.moveToNext()) {
                val faces = JSONArray(cursor.getString(2))
                result[cursor.getString(0)] = IndexedPhoto(cursor.getString(1), (0 until faces.length()).map { i ->
                    val face = faces.getJSONObject(i); val vector = face.getJSONArray("vector")
                    IndexedFace(face.getString("key"), FloatArray(vector.length()) { vector.getDouble(it).toFloat() })
                })
            }
        }
        return result
    }
    fun upsert(records: Map<String, IndexedPhoto>) {
        if (records.isEmpty()) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            records.forEach { (id, photo) ->
                val faces = JSONArray()
                photo.faces.forEach { faces.put(JSONObject().put("key", it.key).put("vector", JSONArray(it.vector.toList()))) }
                val values = ContentValues().apply { put("id", id); put("fingerprint", photo.fingerprint); put("faces", faces.toString()) }
                db.insertWithOnConflict("photos", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }
    fun remove(ids: Set<String>) {
        if (ids.isEmpty()) return
        val db = writableDatabase
        db.beginTransaction()
        try { ids.forEach { db.delete("photos", "id = ?", arrayOf(it)) }; db.setTransactionSuccessful() }
        finally { db.endTransaction() }
    }
}
