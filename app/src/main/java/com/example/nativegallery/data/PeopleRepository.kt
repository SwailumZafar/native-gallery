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
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Semaphore
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
        Core.setNumThreads(2)
        fun model(name: String): String {
            val file = File(directory, name)
            if (!file.exists()) {
                val temporary = File(directory, "$name.tmp")
                context.assets.open("people/$name").use { source -> temporary.outputStream().use { source.copyTo(it) } }
                check(temporary.renameTo(file)) { "Could not prepare face model" }
            }
            return file.absolutePath
        }
        val detectorModel = model("yunet.onnx")
        // Prepare the embedding file once; individual workers load it only if they find a face.
        // Model preparation is synchronized because several workers may find their first face together.
        val embeddingModel by lazy { model("sface.onnx") }
        var failed = 0
        val changed = linkedMapOf<String, IndexedPhoto>()
        var lastGroups = groupIndex.snapshot()
        var groupsChanged = false
        var lastPublished = SystemClock.elapsedRealtime()
        try {
            coroutineScope {
                val pending = photos.filterNot { it.id in records }
                val parallelism = minOf(3, pending.size)
                val window = Semaphore(parallelism)
                val inputs = Channel<IndexedValue<MediaItem>>(parallelism)
                fun discard(result: Pair<Int, Result<IndexedPhoto>>) {
                    result.second.getOrNull()?.faces?.forEach { File(directory, "${it.key}.jpg").delete() }
                }
                val results = Channel<Pair<Int, Result<IndexedPhoto>>>(parallelism, onUndeliveredElement = ::discard)
                val producer = launch {
                    try { pending.withIndex().forEach { inputs.send(it) } }
                    finally { inputs.close() }
                }
                val workers = List(parallelism) { launch(PhotoAnalysisWork.inferenceDispatcher) {
                    val detector = FaceDetectorYN.create(detectorModel, "", Size(640.0, 640.0), 0.9f)
                    val recognizer by lazy { FaceRecognizerSF.create(embeddingModel, "") }
                    for ((position, photo) in inputs) {
                        window.acquire()
                        PhotoAnalysisWork.awaitPeopleAllowed()
                        val found = mutableListOf<IndexedFace>()
                        val result = try {
                            val bitmap = decode(photo)
                            val rgba = Mat(); val image = Mat(); val faces = Mat()
                            try {
                                PhotoAnalysisWork.awaitPeopleAllowed()
                                Utils.bitmapToMat(bitmap, rgba)
                                Imgproc.cvtColor(rgba, image, Imgproc.COLOR_RGBA2BGR)
                                detector.setInputSize(image.size())
                                detector.detect(image, faces)
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
                                        // Track ownership before writing, including a failed/cancelled crop.
                                        found += IndexedFace(key, vector)
                                        val crop = Bitmap.createBitmap(aligned.cols(), aligned.rows(), Bitmap.Config.ARGB_8888)
                                        val rgb = Mat()
                                        try {
                                            Imgproc.cvtColor(aligned, rgb, Imgproc.COLOR_BGR2RGBA)
                                            Utils.matToBitmap(rgb, crop)
                                            File(directory, "$key.jpg").outputStream().use { crop.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                                        } finally { crop.recycle(); rgb.release() }
                                    } finally { box.release(); aligned.release(); feature.release() }
                                }
                                Result.success(IndexedPhoto(fingerprint(photo), found.toList()))
                            } finally { bitmap.recycle(); rgba.release(); image.release(); faces.release() }
                        } catch (error: Exception) {
                            found.forEach { File(directory, "${it.key}.jpg").delete() }
                            if (error is kotlinx.coroutines.CancellationException) throw error
                            Result.failure(error)
                        }
                        results.send(position to result)
                    }
                } }
                launch { workers.joinAll(); results.close() }
                val ready = mutableMapOf<Int, Result<IndexedPhoto>>()
                var next = 0
                try {
                    for ((position, result) in results) {
                        ready[position] = result
                        // Preserve the existing photo order and therefore deterministic face grouping.
                        while (ready.containsKey(next)) {
                        val photo = pending[next]
                        val indexed = ready.remove(next)!!.getOrNull()
                        if (indexed == null) failed++ else {
                            records[photo.id] = indexed; changed[photo.id] = indexed
                            groupIndex.add(photo, indexed.faces)
                            groupsChanged = groupsChanged || indexed.faces.isNotEmpty()
                        }
                        next++; window.release()
                        if ((records.size + failed) % 20 == 0) {
                            database.upsert(changed)
                            changed.clear()
                        }
                        val now = SystemClock.elapsedRealtime()
                        if (now - lastPublished >= 250L || (lastGroups.isEmpty() && groupsChanged)) {
                            if (records.size + failed < photos.size) PhotoAnalysisWork.awaitPeopleAllowed()
                            if (groupsChanged) { lastGroups = groupIndex.snapshot(); groupsChanged = false }
                            progress(lastGroups, records.size + failed, photos.size)
                            lastPublished = now
                        } else progress(lastGroups, records.size + failed, photos.size)
                        }
                    }
                } finally {
                    producer.cancel(); workers.forEach { it.cancel() }; results.cancel()
                    ready.forEach { (position, result) -> discard(position to result) }
                }
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

    private suspend fun decode(photo: MediaItem): Bitmap {
        val uri = requireNotNull(photo.contentUri)
        val resolver = context.contentResolver
        val width = photo.width ?: 0
        val height = photo.height ?: 0
        if (Build.VERSION.SDK_INT >= 29 && uri.scheme == "content" && width > 0 && height > 0) {
            val scale = minOf(1f, 640f / maxOf(width, height))
            val size = AndroidSize((width * scale).toInt().coerceAtLeast(1), (height * scale).toInt().coerceAtLeast(1))
            runCatching { resolver.loadThumbnail(uri, size, null) }.getOrNull()?.let { return it }
        }
        // ImageDecoder samples and applies EXIF in one pass instead of opening, decoding,
        // reading EXIF and allocating a second full bitmap for imported/file-backed photos.
        if (Build.VERSION.SDK_INT >= 28) return decodeUsefulPhoto(context, uri, 640)
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
