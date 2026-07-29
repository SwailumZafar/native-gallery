package com.swailumzafar.nativegallery.benchmark

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.test.platform.app.InstrumentationRegistry
import java.io.ByteArrayOutputStream

internal object BenchmarkMediaFixtures {
    private val FixtureRelativePath =
        "${Environment.DIRECTORY_PICTURES}/NativeGalleryBenchmark"

    fun grantTargetReadPermission() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        runCatching {
            instrumentation.uiAutomation.grantRuntimePermission(TargetPackage, permission)
        }
    }

    fun seed(count: Int): List<Uri> {
        check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "Benchmark fixtures require MediaStore scoped storage"
        }
        val context = InstrumentationRegistry.getInstrumentation().context
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val fixtureBytes = createPngBytes()
        return buildList {
            repeat(count) { index ->
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "benchmark-${index.toString().padStart(3, '0')}.png")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "$FixtureRelativePath/")
                    put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis() - index * 60_000L)
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = checkNotNull(context.contentResolver.insert(collection, values))
                try {
                    checkNotNull(context.contentResolver.openOutputStream(uri, "w")).use { output ->
                        output.write(fixtureBytes)
                    }
                    check(
                        context.contentResolver.update(
                            uri,
                            ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
                            null,
                            null
                        ) == 1
                    )
                    add(uri)
                } catch (failure: Throwable) {
                    runCatching { context.contentResolver.delete(uri, null, null) }
                    throw failure
                }
            }
        }
    }

    fun delete(uris: List<Uri>) {
        val resolver = InstrumentationRegistry.getInstrumentation().context.contentResolver
        uris.forEach { uri -> runCatching { resolver.delete(uri, null, null) } }
    }

    private fun createPngBytes(): ByteArray {
        val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.rgb(68, 116, 183))
        return try {
            ByteArrayOutputStream().use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
                output.toByteArray()
            }
        } finally {
            bitmap.recycle()
        }
    }
}

internal const val TargetPackage = "com.swailumzafar.nativegallery"
