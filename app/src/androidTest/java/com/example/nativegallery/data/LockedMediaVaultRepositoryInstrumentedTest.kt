package com.example.nativegallery.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.model.MediaType
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
class LockedMediaVaultRepositoryInstrumentedTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun importSurvivesRepositoryRecreationAndRestoresOriginalBytes() {
        val uniqueId = UUID.randomUUID().toString()
        val mediaId = "android-test-$uniqueId"
        val marker = "native-gallery-vault-marker-$uniqueId".toByteArray()
        val sourceBytes = createPngBytes() + marker
        var sourceUri: Uri? = null
        var restoredUri: Uri? = null
        val repository = LockedMediaVaultRepository(context)

        try {
            sourceUri = insertImageFixture("vault-source-$uniqueId.png", sourceBytes)
            val mediaItem = MediaItem(
                id = mediaId,
                albumId = "android-test-fixtures",
                type = MediaType.Photo,
                title = "vault-source-$uniqueId.png",
                dateLabel = "Instrumentation test",
                contentUri = sourceUri,
                mimeType = "image/png",
                fileSizeBytes = sourceBytes.size.toLong(),
                width = FixtureWidth,
                height = FixtureHeight,
                relativePath = "$FixtureRelativePath/",
                sortTimestampMillis = System.currentTimeMillis()
            )

            assertTrue("The fixture should be encrypted into the private vault", repository.importMedia(mediaItem))
            assertTrue(repository.hasEncryptedCopy(mediaId))
            assertTrue(repository.originalMediaExists(mediaId))

            val rawEncryptedFile = encryptedFile(mediaId)
            assertTrue("The encrypted vault file should exist", rawEncryptedFile.isFile)
            assertFalse(
                "The private vault file must not be a plaintext copy",
                rawEncryptedFile.readBytes().contentEquals(sourceBytes)
            )
            assertFalse(
                "A recognizable plaintext marker must not leak into the encrypted file",
                rawEncryptedFile.readBytes().containsSubsequence(marker)
            )

            val recreatedRepository = LockedMediaVaultRepository(context)
            val recreatedItem = recreatedRepository.loadSnapshot().mediaById[mediaId]
            assertNotNull("Metadata and encrypted media should survive repository recreation", recreatedItem)
            assertArrayEquals(
                "The vault provider should decrypt the original bytes on demand",
                sourceBytes,
                readBytes(requireNotNull(recreatedItem).contentUri)
            )

            assertEquals(1, context.contentResolver.delete(requireNotNull(sourceUri), null, null))
            sourceUri = null
            assertFalse("The simulated public original should now be absent", recreatedRepository.originalMediaExists(mediaId))

            restoredUri = recreatedRepository.restoreMedia(recreatedItem)
            assertNotNull("A vault item should restore to an app-owned MediaStore row", restoredUri)
            assertArrayEquals(
                "Restoring from the encrypted vault must preserve the full source payload",
                sourceBytes,
                readBytes(restoredUri)
            )

            recreatedRepository.delete(mediaId)
            assertFalse(recreatedRepository.hasEncryptedCopy(mediaId))
            assertNull(LockedMediaVaultRepository(context).loadSnapshot().mediaById[mediaId])
        } finally {
            sourceUri?.let { runCatching { context.contentResolver.delete(it, null, null) } }
            restoredUri?.let { runCatching { context.contentResolver.delete(it, null, null) } }
            repository.delete(mediaId)
            LockedMediaVaultProvider.clearSessionCache(context)
        }
    }

    private fun insertImageFixture(displayName: String, bytes: ByteArray): Uri {
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "$FixtureRelativePath/")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = requireNotNull(context.contentResolver.insert(collection, values)) {
            "Unable to create the MediaStore test fixture"
        }
        try {
            checkNotNull(context.contentResolver.openOutputStream(uri, "w")).use { output ->
                output.write(bytes)
            }
            val published = context.contentResolver.update(
                uri,
                ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
                null,
                null
            )
            check(published == 1) { "Unable to publish the MediaStore test fixture" }
            return uri
        } catch (failure: Throwable) {
            runCatching { context.contentResolver.delete(uri, null, null) }
            throw failure
        }
    }

    private fun readBytes(uri: Uri?): ByteArray {
        return checkNotNull(context.contentResolver.openInputStream(requireNotNull(uri))).use { input ->
            input.readBytes()
        }
    }

    private fun encryptedFile(mediaId: String): File {
        val token = Base64.encodeToString(
            mediaId.toByteArray(Charsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
        )
        return File(context.filesDir, "locked_media_vault/$token.ngv")
    }

    private fun createPngBytes(): ByteArray {
        val bitmap = Bitmap.createBitmap(FixtureWidth, FixtureHeight, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.rgb(35, 108, 196))
        return try {
            ByteArrayOutputStream().use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
                output.toByteArray()
            }
        } finally {
            bitmap.recycle()
        }
    }

    private fun ByteArray.containsSubsequence(candidate: ByteArray): Boolean {
        if (candidate.isEmpty()) return true
        if (candidate.size > size) return false
        return (0..size - candidate.size).any { offset ->
            candidate.indices.all { index -> this[offset + index] == candidate[index] }
        }
    }

    private companion object {
        const val FixtureWidth = 48
        const val FixtureHeight = 32
        val FixtureRelativePath = "${Environment.DIRECTORY_PICTURES}/NativeGalleryAndroidTests"
    }
}
