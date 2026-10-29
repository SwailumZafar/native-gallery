package com.example.nativegallery.data

import android.graphics.Bitmap
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.model.MediaType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PhotoGeometryHeaderTest {
    @Test fun originalOrientationReplacesIncorrectProviderDimensionsWithoutEditingThePhoto() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "geometry-fixture-${System.nanoTime()}.jpg")
        try {
            val bitmap = Bitmap.createBitmap(300, 100, Bitmap.Config.ARGB_8888)
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            bitmap.recycle()
            ExifInterface(file).apply { setAttribute(ExifInterface.TAG_ORIENTATION, "6"); saveAttributes() }
            val original = file.readBytes()
            val item = MediaItem("geometry-test", "fixture", MediaType.Photo, "fixture", "Today",
                contentUri = Uri.fromFile(file), width = 300, height = 100)
            val resolved = PhotoPresentationGeometry.resolve(context, item)
            assertEquals(100, resolved.width)
            assertEquals(300, resolved.height)
            assertArrayEquals(original, file.readBytes())
        } finally { file.delete() }
    }
}
