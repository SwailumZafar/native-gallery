package com.example.nativegallery.ui

import androidx.compose.ui.geometry.Offset
import androidx.media3.common.Player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoViewerPolicyTest {
    @Test
    fun runtimeDurationWinsAndUnknownDurationDoesNotInvalidateShortClips() {
        assertEquals(500L, preferredVideoDurationMillis(500L, 1_000L, 2_000L))
        assertEquals(375L, preferredVideoDurationMillis(androidx.media3.common.C.TIME_UNSET, 375L, 0L))
        assertFalse(isVideoSeekAcknowledged(300, 0, 400))
    }
    @Test
    fun filmstripWindowIsStableAndBoundedAroundSelection() {
        assertEquals(0..6, viewerFilmstripWindow(itemCount = 20, selectedIndex = 0))
        assertEquals(0..6, viewerFilmstripWindow(itemCount = 20, selectedIndex = 3))
        assertEquals(7..13, viewerFilmstripWindow(itemCount = 20, selectedIndex = 10))
        assertEquals(13..19, viewerFilmstripWindow(itemCount = 20, selectedIndex = 19))
        assertEquals(0..2, viewerFilmstripWindow(itemCount = 3, selectedIndex = 1))
        assertTrue(viewerFilmstripWindow(itemCount = 0, selectedIndex = 0).isEmpty())
    }
    @Test
    fun videoSideGestureNeedsClearVerticalIntent() {
        assertFalse(shouldActivateVideoSideGesture(Offset(2f, 14f), touchSlop = 8f))
        assertFalse(shouldActivateVideoSideGesture(Offset(28f, 24f), touchSlop = 8f))
        assertTrue(shouldActivateVideoSideGesture(Offset(4f, 22f), touchSlop = 8f))
    }
    @Test
    fun videoSeekIgnoresMatroskaFallbackToStart() {
        assertFalse(
            isVideoSeekAcknowledged(
                requestedPositionMs = 120_000,
                reportedPositionMs = 0,
                durationMs = 600_000
            )
        )
        assertTrue(
            isVideoSeekAcknowledged(
                requestedPositionMs = 120_000,
                reportedPositionMs = 116_000,
                durationMs = 600_000
            )
        )
        assertTrue(
            isVideoSeekAcknowledged(
                requestedPositionMs = 0,
                reportedPositionMs = 0,
                durationMs = 600_000
            )
        )
    }

    @Test
    fun mediaTekCameraAvcPrefersVendorHardwareDecoder() {
        assertTrue(isMediaTekCodecPlatform("mt6768"))
        assertTrue(isMediaTekCodecPlatform("MediaTek-Dimensity"))
        assertFalse(isMediaTekCodecPlatform("qcom"))
        assertTrue(shouldPreferHardwareAvcDecoder("mt6768", "video/avc"))
        assertFalse(shouldPreferHardwareAvcDecoder("mt6768", "video/hevc"))
        assertFalse(shouldPreferHardwareAvcDecoder("qcom", "video/avc"))
    }

    @Test
    fun videoVolumeUsesTheCurrentMediaStreamAndCanRaiseItFromZero() {
        assertEquals(0f, normalizedMediaVolume(currentVolume = 0, maximumVolume = 16), 0.001f)
        assertEquals(0.25f, normalizedMediaVolume(currentVolume = 4, maximumVolume = 16), 0.001f)
        assertEquals(8, mediaStreamVolumeLevel(targetVolume = 0.5f, maximumVolume = 16))
        assertEquals(1f, localPlayerGain(targetVolume = 0.5f, systemVolumeApplied = true), 0.001f)
        assertEquals(0.5f, localPlayerGain(targetVolume = 0.5f, systemVolumeApplied = false), 0.001f)
    }

    @Test
    fun bufferingSeekKeepsPreparedControlsVisible() {
        assertFalse(videoPreparedStateAfterPlaybackState(false, Player.STATE_BUFFERING))
        assertTrue(videoPreparedStateAfterPlaybackState(true, Player.STATE_BUFFERING))
        assertTrue(videoPreparedStateAfterPlaybackState(false, Player.STATE_READY))
        assertTrue(videoPreparedStateAfterPlaybackState(false, Player.STATE_ENDED))
        assertFalse(videoPreparedStateAfterPlaybackState(true, Player.STATE_IDLE))
    }


    @Test
    fun backgroundedVideoCannotKeepPlayingOrAutoplayOnResume() {
        assertFalse(
            shouldVideoPlaybackRun(
                isActive = true,
                isPrepared = true,
                foregroundPlaybackAllowed = true,
                lifecycleResumed = false
            )
        )
        assertFalse(
            shouldVideoPlaybackRun(
                isActive = true,
                isPrepared = true,
                foregroundPlaybackAllowed = false,
                lifecycleResumed = true
            )
        )
        assertTrue(
            shouldVideoPlaybackRun(
                isActive = true,
                isPrepared = true,
                foregroundPlaybackAllowed = true,
                lifecycleResumed = true
            )
        )
    }

    @Test
    fun compactLandscapeKeepsActionsAndFilmstripOutOfTheMediaStage() {
        assertTrue(isViewerCompactLandscape(screenWidthDp = 800, screenHeightDp = 360))
        assertFalse(isViewerCompactLandscape(screenWidthDp = 360, screenHeightDp = 800))
        assertFalse(isViewerCompactLandscape(screenWidthDp = 1000, screenHeightDp = 700))

        assertFalse(
            shouldShowViewerFilmstrip(
                itemCount = 20,
                isVideo = false,
                compactLandscape = true
            )
        )
        assertTrue(
            shouldShowViewerFilmstrip(
                itemCount = 20,
                isVideo = false,
                compactLandscape = false
            )
        )
        assertFalse(
            shouldShowViewerFilmstrip(
                itemCount = 20,
                isVideo = true,
                compactLandscape = false
            )
        )
        assertEquals(4, ViewerBottomChromeSectionGapDp)
        assertEquals(82, ViewerBottomChromeActionInsetDp)
        assertEquals(72, ViewerActionCapsuleHeightDp)
        assertEquals(32, ViewerVideoSeekHeightDp)
        assertEquals(0, viewerBeyondViewportPageCount())
    }

    @Test
    fun viewerControlsStayCenteredAndBoundedOnExpandedWidths() {
        assertEquals(336, viewerVideoControlMaxWidthDp(screenWidthDp = 360))
        assertEquals(560, viewerVideoControlMaxWidthDp(screenWidthDp = 700))
        assertEquals(640, viewerVideoControlMaxWidthDp(screenWidthDp = 1200))
    }

    @Test
    fun pinchOutNeverScalesBelowTheViewerDismissBoundary() {
        assertEquals(0.82f, viewerPinchScale(currentScale = 1f, zoomDelta = 0.1f), 0.001f)
        assertEquals(0.82f, viewerPinchScale(currentScale = 0.82f, zoomDelta = 0.5f), 0.001f)
        assertEquals(5f, viewerPinchScale(currentScale = 4f, zoomDelta = 2f), 0.001f)
    }

    @Test
    fun doubleTapZoomTogglesBetweenFitAndZoomed() {
        assertEquals(2.8f, viewerDoubleTapTargetScale(currentScale = 1f), 0.001f)
        assertEquals(1f, viewerDoubleTapTargetScale(currentScale = 2.8f), 0.001f)
        assertEquals(1f, viewerDoubleTapTargetScale(currentScale = 1.06f), 0.001f)
    }

    @Test
    fun dismissDragProgressAndScaleAreBounded() {
        assertEquals(0f, viewerDismissProgress(Offset.Zero, 100f), 0.001f)
        val halfOffset = Offset(0f, 140f)
        val progress = viewerDismissProgress(halfOffset, 100f)
        assertTrue(progress in 0.45f..0.55f)
        val scale = viewerDismissScale(halfOffset, 100f)
        assertTrue(scale in 0.80f..0.90f)
    }

    @Test
    fun dismissFractionAndAspectDimensionsAreExact() {
        assertEquals(0f, viewerDismissFraction(pullScale = 1f), 0.001f)
        assertEquals(1f, viewerDismissFraction(pullScale = 0.72f), 0.001f)
        assertEquals(0.5f, viewerDismissFraction(pullScale = 0.86f), 0.01f)

        // 16:9 video in portrait 1080x2400
        val (videoW, videoH) = fittedMediaDimensions(
            containerWidth = 1080f,
            containerHeight = 2400f,
            mediaWidth = 1920,
            mediaHeight = 1080
        )
        assertEquals(1080f, videoW, 0.01f)
        assertEquals(607.5f, videoH, 0.01f)

        // 3:4 portrait photo in portrait 1080x2400
        val (photoW, photoH) = fittedMediaDimensions(
            containerWidth = 1080f,
            containerHeight = 2400f,
            mediaWidth = 3000,
            mediaHeight = 4000
        )
        assertEquals(1080f, photoW, 0.01f)
        assertEquals(1440f, photoH, 0.01f)

        // 16:9 video in landscape 2400x1080
        val (landW, landH) = fittedMediaDimensions(
            containerWidth = 2400f,
            containerHeight = 1080f,
            mediaWidth = 1920,
            mediaHeight = 1080
        )
        assertEquals(1920f, landW, 0.01f)
        assertEquals(1080f, landH, 0.01f)

        // 90-degree rotated portrait video (recorded as 1920x1080 with 90deg metadata -> oriented 1080x1920)
        val rawWidth = 1920
        val rawHeight = 1080
        val rotationDegrees = 90
        val orientedW = if (rotationDegrees % 180 != 0) rawHeight else rawWidth
        val orientedH = if (rotationDegrees % 180 != 0) rawWidth else rawHeight
        val (rotatedW, rotatedH) = fittedMediaDimensions(
            containerWidth = 1080f,
            containerHeight = 2400f,
            mediaWidth = orientedW,
            mediaHeight = orientedH
        )
        assertEquals(1080f, rotatedW, 0.01f)
        assertEquals(1920f, rotatedH, 0.01f)
    }
}
