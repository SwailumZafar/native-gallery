package com.example.nativegallery.ui

import android.graphics.Color
import android.net.Uri
import android.os.Looper
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.nativegallery.ui.components.VideoThumbnailDecoder
import com.example.nativegallery.ui.components.VideoThumbnailDecoderHost
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

class VideoThumbnailWorkerTest {
    @get:Rule val composeRule = createComposeRule()
    @Before fun keepAwake() = TestScreenAwake.enable()

    @Test fun fallbackDecodingUsesItsOwnLooperAndCopiesTheActualVideoFrame(): Unit = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val video = File.createTempFile("thumbnail-worker-", ".mp4", context.cacheDir)
        instrumentation.context.assets.open("thumbnail-red.mp4").use { input ->
            video.outputStream().use(input::copyTo)
        }
        try {
            composeRule.setContent { VideoThumbnailDecoderHost() }
            composeRule.waitForIdle()
            assertNotSame(Looper.getMainLooper(), VideoThumbnailDecoder.applicationLooper)
            VideoThumbnailDecoder.setInteractionBlocked(false)
            val frame = withTimeout(15_000) { VideoThumbnailDecoder.decode(context, Uri.fromFile(video), 64) }
            assertNotNull("Worker decoder must capture a frame", frame)
            requireNotNull(frame)
            val pixel = frame.getPixel(frame.width / 2, frame.height / 2)
            assertTrue("Captured frame must match the red fixture", Color.red(pixel) > 160 && Color.green(pixel) < 80)
            frame.recycle()
        } finally {
            VideoThumbnailDecoder.setInteractionBlocked(true)
            video.delete()
        }
    }
}
