package com.example.nativegallery.ui

import com.example.nativegallery.data.VideoDisplayDimensions
import com.example.nativegallery.data.videoDisplayDimensions
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaPresentationGeometryTest {
    private fun item(video: Boolean, width: Int? = null, height: Int? = null) = MediaItem(
        "fixture", "fixture", if (video) MediaType.Video else MediaType.Photo,
        "Fixture", "Today", isVideo = video, width = width, height = height
    )

    @Test
    fun croppedPosterCannotReplaceKnownLandscapeOrPortraitGeometry() {
        for (video in listOf(false, true)) {
            for ((width, height) in listOf(1920 to 1080, 1080 to 1920)) {
                val original = item(video, width, height)
                assertEquals(original, mediaPresentationGeometry(original, 384, 384))
            }
        }
    }

    @Test
    fun unknownVideoAvoidsSquarePosterGeometryWhilePhotosCanUseTheirPreview() {
        val video = mediaPresentationGeometry(item(true), 384, 384)
        assertEquals(16, video.width)
        assertEquals(9, video.height)
        val photo = mediaPresentationGeometry(item(false), 300, 400)
        assertEquals(300, photo.width)
        assertEquals(400, photo.height)
        assertNull(mediaPresentationGeometry(item(false), 0, 0).width)
    }

    @Test
    fun rotationAndPixelAspectProduceDisplayDimensionsBeforeFitting() {
        assertEquals(VideoDisplayDimensions(1080, 1920), videoDisplayDimensions(1920, 1080, 90))
        assertEquals(VideoDisplayDimensions(1080, 1920), videoDisplayDimensions(1920, 1080, -90))
        assertEquals(VideoDisplayDimensions(960, 576), videoDisplayDimensions(720, 576, 0, 4f / 3f))
        val dimensions = videoDisplayDimensions(720, 576, 270, 4f / 3f)!!
        assertEquals(VideoDisplayDimensions(576, 960), dimensions)
        val fitted = fittedMediaDimensions(1080f, 2400f, dimensions.width, dimensions.height)
        assertEquals(1080f, fitted.first, 0.01f)
        assertEquals(1800f, fitted.second, 0.01f)
    }

    @Test
    fun malformedVideoSizeIsRejectedAndInvalidPixelAspectFallsBackSafely() {
        assertNull(videoDisplayDimensions(0, 1080, 0))
        assertNull(videoDisplayDimensions(1920, -1, 0))
        for (ratio in listOf(Float.NaN, Float.POSITIVE_INFINITY, 0f, -1f)) {
            assertEquals(VideoDisplayDimensions(1920, 1080), videoDisplayDimensions(1920, 1080, 360, ratio))
        }
    }
}
