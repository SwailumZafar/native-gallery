package com.example.nativegallery.data

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.model.MediaType
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate
import java.util.UUID

/** All media, databases, preferences and exported files are generated in isolated test namespaces. */
@RunWith(AndroidJUnit4::class)
class UsefulPhotoRepositoriesTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun root() = File(context.cacheDir, "private_shares/feature-test-${UUID.randomUUID()}").apply { mkdirs() }
    private fun uri(file: File) = FileProvider.getUriForFile(context, "${context.packageName}.private_shares", file)
    private fun fixture(directory: File): File {
        val bitmap = Bitmap.createBitmap(640, 960, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap); canvas.drawColor(Color.WHITE)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 34f }
            listOf("Concert ticket", "Seat A12", "12 NOV 2030", "Email person@example.com", "Reference 456789").forEachIndexed { i, line ->
                canvas.drawText(line, 30f, 120f + i * 70f, paint)
            }
            return File(directory, "source.jpg").also { file -> file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 96, it) } }
        } finally { bitmap.recycle() }
    }
    private fun item(file: File) = MediaItem("fixture-${UUID.randomUUID()}", "feature-test", MediaType.Photo, "Screenshot-fixture.jpg", "",
        contentUri = uri(file), relativePath = "Pictures/Screenshots/", fileSizeBytes = file.length(), dateModifiedMillis = 1,
        sortTimestampMillis = 1_793_000_000_000L, width = 640, height = 960)

    @Test fun privateExportRemovesExifPreservesOriginalAndGrantsOnlyTheCopy(): Unit = runBlocking {
        val directory = root()
        try {
            val file = fixture(directory)
            ExifInterface(file.absolutePath).apply {
                setLatLong(31.5204, 74.3587); setAttribute(ExifInterface.TAG_MAKE, "FixtureCamera")
                setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString()); saveAttributes()
            }
            val before = file.readBytes()
            val source = decodeUsefulPhoto(context, uri(file), 2048)
            try {
                assertEquals(960, source.width); assertEquals(640, source.height)
                val output = PrivateSharingRepository(context, File(directory, "exports")).export(source,
                    listOf(PrivacyRegion("manual", 0.2f, 0.2f, 0.4f, 0.4f)), true)
                assertNotEquals(uri(file), output)
                context.contentResolver.openInputStream(output)!!.use { stream ->
                    val exif = ExifInterface(stream)
                    assertNull(exif.latLong); assertNull(exif.getAttribute(ExifInterface.TAG_MAKE))
                }
                assertArrayEquals(before, file.readBytes())
                val intent = privatePhotoShareIntent(output)
                assertEquals("image/jpeg", intent.type)
                assertEquals(output, intent.clipData!!.getItemAt(0).uri)
                assertEquals(output, intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
                assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
                assertEquals(0, intent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            } finally { source.recycle() }
        } finally { directory.deleteRecursively() }
    }
    @Test fun overlappingFaceMasksCannotRevealASolidCoveredArea() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        val covered = renderPrivateCopy(bitmap, listOf(PrivacyRegion("manual", 0.3f, 0.3f, 0.7f, 0.7f),
            PrivacyRegion("face", 0.1f, 0.1f, 0.9f, 0.9f, PrivacyMaskKind.Face)), false)
        try {
            assertEquals(Color.rgb(24, 24, 28), covered.getPixel(50, 50))
            assertEquals(Color.RED, bitmap.getPixel(50, 50))
        } finally { bitmap.recycle(); covered.recycle() }
    }
    @Test fun bundledAnalysisSuggestsSensitiveAreasOnGeneratedContent(): Unit = runBlocking {
        val directory = root()
        try {
            val prepared = withTimeout(30_000) { PrivateSharingRepository(context).prepare(item(fixture(directory))) }
            try {
                assertNull(prepared.warning)
                assertTrue(prepared.regions.any { it.kind == PrivacyMaskKind.SensitiveText })
                assertTrue(prepared.regions.none { it.kind == PrivacyMaskKind.Face })
            } finally { prepared.bitmap.recycle() }
        } finally { directory.deleteRecursively() }
    }
    @Test fun momentsPersistNamesCoversNotesAndReferencesWithoutMovingFiles(): Unit = runBlocking {
        val name = "moments-test-${UUID.randomUUID()}"
        try {
            val original = MomentLibrary(listOf(SavedMoment("trip", "A trip", listOf("a", "b"), "b", "A day by the lake", 123)), setOf("suggested:a"))
            MomentRepository(context, name).save(original)
            assertEquals(original, MomentRepository(context, name).load())
        } finally { context.deleteSharedPreferences(name) }
    }
    @Test fun screenshotIndexAndAnnotationsSurviveRescanningAndSourceChanges(): Unit = runBlocking {
        val directory = root(); val name = "screenshots-test-${UUID.randomUUID()}.db"
        val repository = ScreenshotRepository(context, name)
        try {
            val photo = item(fixture(directory))
            val date = LocalDate.of(2030, 11, 12)
            repository.annotate(ScreenshotEntry(photo, pinned = true, done = true, followUp = date, manualCategory = ScreenshotCategory.Orders))
            PhotoAnalysisWork.setInteractionBusy(false); PhotoAnalysisWork.setTouching(false); PhotoAnalysisWork.setDocumentsScrolling(false)
            withTimeout(30_000) { repository.scan(listOf(photo)) { _, _, _, _ -> } }
            val cached = repository.cached(listOf(photo)).single()
            assertTrue(cached.indexed); assertTrue(cached.text.contains("Concert", true))
            assertEquals(ScreenshotCategory.Tickets, cached.category)
            assertEquals(ScreenshotCategory.Orders, cached.displayCategory)
            assertTrue(cached.pinned); assertTrue(cached.done); assertEquals(date, cached.followUp)
            val changed = repository.cached(listOf(photo.copy(dateModifiedMillis = 2))).single()
            assertFalse(changed.indexed); assertEquals("", changed.text); assertTrue(changed.pinned)
            repository.clearIndex()
            val cleared = repository.cached(listOf(photo)).single()
            assertFalse(cleared.indexed); assertEquals("", cleared.text)
            assertEquals(ScreenshotCategory.Orders, cleared.displayCategory); assertEquals(date, cleared.followUp)
            assertTrue(repository.cached(emptyList()).isEmpty())
        } finally { repository.close(); context.deleteDatabase(name); directory.deleteRecursively() }
    }
    @Test fun anUnavailableScreenshotDoesNotBlockTheRemainingScan(): Unit = runBlocking {
        val directory = root(); val name = "screenshots-test-${UUID.randomUUID()}.db"
        val repository = ScreenshotRepository(context, name)
        try {
            val valid = item(fixture(directory))
            val missing = valid.copy(id = "missing", contentUri = Uri.parse("content://missing.photo.fixture/absent"), sortTimestampMillis = valid.sortTimestampMillis + 1)
            var failures = 0
            var result = emptyList<ScreenshotEntry>()
            PhotoAnalysisWork.setInteractionBusy(false); PhotoAnalysisWork.setTouching(false); PhotoAnalysisWork.setDocumentsScrolling(false)
            withTimeout(30_000) { repository.scan(listOf(missing, valid)) { entries, _, _, failed -> result = entries; failures = failed } }
            assertEquals(1, failures)
            assertTrue(result.first { it.media.id == valid.id }.indexed)
            assertFalse(result.first { it.media.id == missing.id }.indexed)
        } finally { repository.close(); context.deleteDatabase(name); directory.deleteRecursively() }
    }
    @Test fun privateBatchIntentGrantsEveryCopyAndNoWriteAccess() {
        val copies = listOf(Uri.parse("content://${context.packageName}.private_shares/fixture/a.jpg"), Uri.parse("content://${context.packageName}.private_shares/fixture/b.jpg"))
        val intent = privatePhotosShareIntent(copies)
        assertEquals(Intent.ACTION_SEND_MULTIPLE, intent.action)
        assertEquals(copies, intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM))
        assertEquals(copies, (0 until intent.clipData!!.itemCount).map { intent.clipData!!.getItemAt(it).uri })
        assertEquals(0, intent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
    }
    @Test fun memoryContentScanRejectsGeneratedTextAndReusesItsCache(): Unit = runBlocking {
        val directory = root(); val name = "memories-test-${UUID.randomUUID()}.db"
        try {
            val photo = item(fixture(directory)).copy(title = "IMG_fixture.jpg", relativePath = "DCIM/Camera/")
            val repository = MomentAnalysisRepository(context, name)
            PhotoAnalysisWork.setInteractionBusy(false); PhotoAnalysisWork.setTouching(false); PhotoAnalysisWork.setDocumentsScrolling(false)
            var result: MemoryScanProgress? = null
            withTimeout(45_000) { repository.scan(listOf(photo)) { result = it } }
            assertEquals(1, result?.scanned)
            assertTrue(requireNotNull(result).evidence.containsKey(photo.id))
            assertNull(result!!.evidence.getValue(photo.id).theme)
            var callbacks = 0
            withTimeout(10_000) { repository.scan(listOf(photo)) { callbacks++; result = it } }
            assertEquals(1, callbacks)
            assertEquals(1, result?.scanned)
        } finally { context.deleteDatabase(name); directory.deleteRecursively() }
    }
}
