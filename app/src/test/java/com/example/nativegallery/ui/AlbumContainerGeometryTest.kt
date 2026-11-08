package com.example.nativegallery.ui

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import com.example.nativegallery.ui.components.GalleryMotion

class AlbumContainerGeometryTest {
    @Test fun heroFirstAndLastFramesStayGentleAtTheUnchangedAnimationTimings() {
        val source = Rect(54f, 802f, 1210f, 1326f)
        val firstOpeningProgress = 16.6667f / GalleryMotion.AlbumOpenMillis
        val firstClosingProgress = 1f - 16.6667f / GalleryMotion.AlbumCloseMillis
        val first = albumContainerFrame(source, 1264f, 2780f, firstOpeningProgress)
        val close = albumContainerFrame(source, 1264f, 2780f, firstClosingProgress)
        assertTrue("The Hero must not leap hundreds of pixels on its first frame", first.bounds.height - source.height < 100f)
        val previousClosingStep = (2780f - source.height) * (1f - firstClosingProgress)
        assertTrue("Closing must reduce the first-frame jump by more than half: ${2780f - close.bounds.height} vs $previousClosingStep",
            2780f - close.bounds.height < previousClosingStep * 0.5f)
        assertTrue("The captured Hero must cover the initial page preparation", albumCoverAlpha(source, firstOpeningProgress, 1264f) > 0.98f)
        assertEquals(400, GalleryMotion.AlbumOpenMillis)
        assertEquals(280, GalleryMotion.AlbumCloseMillis)
    }

    @Test fun coversOnlyBlendAtTheSourceSizedEndOfTheJourney() {
        val banner = Rect(54f, 802f, 1210f, 1326f)
        val square = Rect(54f, 802f, 578f, 1326f)
        assertEquals(0f, albumCoverAlpha(banner, 0.3f), 0f)
        assertEquals(0f, albumCoverAlpha(square, 0.3f), 0f)
        for (source in listOf(banner, square)) {
            assertEquals(1f, albumCoverAlpha(source, 0f), 0f)
            assertEquals(0f, albumCoverAlpha(source, 1f), 0f)
            var previous = 1f
            for (step in 0..100) {
                val alpha = albumCoverAlpha(source, step / 100f)
                assertTrue(alpha <= previous)
                previous = alpha
            }
        }
    }

    @Test fun miniaturePageKeepsItsRowsUntilTheFinalCollapseIntoTheCover() {
        for (source in listOf(Rect(450f, 497f, 816f, 863f), Rect(54f, 802f, 614f, 1326f))) {
            for (progress in listOf(0.22f, 0.35f, 0.6f, 0.9f)) {
                val frame = albumContainerFrame(source, 1264f, 2780f, progress)
                assertEquals(1264f / 2780f, frame.bounds.width / frame.bounds.height, 0.001f)
                assertEquals(2780f, frame.contentClip.height, 0.01f)
                assertMappedClip(frame)
            }
            val nearingCover = albumContainerFrame(source, 1264f, 2780f, 0.05f)
            assertTrue(nearingCover.contentClip.height < 2780f)
        }
    }

    @Test fun heroCoversExpandInsideTheViewportWithoutAnEarlyHeightJump() {
        for (source in listOf(Rect(54f, 802f, 1210f, 1326f), Rect(54f, 1450f, 1210f, 1940f))) {
            var previous = albumContainerFrame(source, 1264f, 2780f, 0f)
            assertEquals(source, previous.bounds)
            for (step in 1..120) {
                val frame = albumContainerFrame(source, 1264f, 2780f, step / 120f)
                assertTrue(frame.bounds.bottom <= 2780f)
                assertTrue(frame.bounds.height >= previous.bounds.height)
                assertTrue("Hero height must grow gradually", frame.bounds.height - previous.bounds.height < 40f)
                assertMappedClip(frame)
                previous = frame
            }
            assertEquals(Rect(0f, 0f, 1264f, 2780f), previous.bounds)
            assertTrue("Hero cover must blend over several advancing frames", albumCoverAlpha(source, 0.30f, 1264f) > 0.5f)
            assertEquals(0f, albumCoverAlpha(source, 0.80f, 1264f), 0f)
        }
    }

    @Test fun heroPageAndCoverStayCenteredInsideTheSameAperture() {
        val source = Rect(54f, 802f, 1210f, 1326f)
        for (step in 0..20) {
            val frame = albumContainerFrame(source, 1264f, 2780f, step / 20f)
            val cover = albumCoverTransform(source.width, source.height, frame.bounds)
            assertEquals(frame.bounds.center.x, cover.translation.x + source.width * cover.scale / 2f, 0.001f)
            assertEquals(frame.bounds.center.y, cover.translation.y + source.height * cover.scale / 2f, 0.001f)
            assertEquals(frame.bounds.center.y, frame.translation.y + 2780f * frame.contentScale / 2f, 0.001f)
            assertMappedClip(frame)
        }
    }

    @Test fun heroRevealsAndClosesBothEndsOfThePageInsteadOfWipingFromItsHeader() {
        val frame = albumContainerFrame(Rect(54f, 802f, 1210f, 1326f), 1264f, 2780f, 0.5f)
        assertTrue("The header must be masked along with the bottom rows", frame.contentClip.top > 0f)
        assertTrue(frame.contentClip.bottom < 2780f)
        assertEquals(2780f - frame.contentClip.bottom, frame.contentClip.top, 0.01f)
    }

    @Test fun aPartlyVisibleHeroReturnsToItsActualCardWithoutBeingShiftedIntoTheViewport() {
        for (source in listOf(Rect(54f, 2450f, 1210f, 3045f), Rect(54f, -180f, 1210f, 415f))) {
            val closed = albumContainerFrame(source, 1264f, 2780f, 0f)
            assertEquals(source, closed.bounds)
            assertMappedClip(closed)
            val halfway = albumContainerFrame(source, 1264f, 2780f, 0.5f)
            assertEquals((source.top + 0f) / 2f, halfway.bounds.top, 0.001f)
            assertEquals((source.bottom + 2780f) / 2f, halfway.bounds.bottom, 0.001f)
            assertMappedClip(halfway)
        }
    }

    @Test fun aCoverKeepsItsAspectRatioAndCoversTheGrowingAperture() {
        val bounds = Rect(20f, 300f, 1240f, 1700f)
        val crop = albumCoverTransform(1156f, 524f, bounds)
        val drawn = Rect(crop.translation.x, crop.translation.y,
            crop.translation.x + 1156f * crop.scale, crop.translation.y + 524f * crop.scale)
        assertEquals(1156f / 524f, drawn.width / drawn.height, 0.001f)
        assertEquals(bounds.center.x, drawn.center.x, 0.01f)
        assertEquals(bounds.center.y, drawn.center.y, 0.01f)
        assertTrue(drawn.left <= bounds.left && drawn.right >= bounds.right)
        assertTrue(drawn.top <= bounds.top && drawn.bottom >= bounds.bottom)
    }

    @Test fun openingStartsAtTheCardWithTheHeaderAtItsTop() {
        val source = Rect(450f, 497f, 816f, 863f)
        val frame = albumContainerFrame(source, 1264f, 2780f, 0f)
        assertEquals(source, frame.bounds)
        assertEquals(366f / 1264f, frame.contentScale, 0.0001f)
        assertEquals(source.top, frame.translation.y, 0.001f)
        assertEquals(0f, frame.contentClip.top, 0f)
        assertMappedClip(frame)
    }

    @Test fun midwayHeaderAndGridShrinkWithThePageInsteadOfStayingFullSize() {
        val frame = albumContainerFrame(Rect(450f, 497f, 816f, 863f), 1264f, 2780f, 0.5f)
        assertTrue(frame.contentScale in 0.64f..0.65f)
        assertEquals(frame.bounds.top, frame.translation.y, 0.001f)
        assertMappedClip(frame)
    }

    @Test fun bothDirectionsHaveExactBoundsForWideCardsListsAndLandscape() {
        for ((width, height) in listOf(1264f to 2780f, 2780f to 1264f)) {
            for (source in listOf(Rect(54f, 802f, 1210f, 1326f), Rect(64f, 350f, 430f, 716f), Rect(50f, 300f, 1100f, 480f))) {
                for (progress in listOf(0f, 0.1f, 0.5f, 0.9f, 1f)) {
                    assertMappedClip(albumContainerFrame(source, width, height, progress))
                }
            }
        }
    }

    @Test fun settlingAndSpringOvershootCannotLeaveAClipOrScaleAtRest() {
        val source = Rect(54f, 802f, 1210f, 1326f)
        for (progress in listOf(1f, 1.02f)) {
            val frame = albumContainerFrame(source, 1264f, 2780f, progress)
            assertEquals(Rect(0f, 0f, 1264f, 2780f), frame.bounds)
            assertEquals(frame.bounds, frame.contentClip)
            assertEquals(1f, frame.contentScale, 0f)
            assertEquals(0f, frame.translation.x, 0f)
            assertEquals(0f, frame.translation.y, 0f)
        }
    }

    private fun assertMappedClip(frame: AlbumContainerFrame) {
        assertEquals(frame.bounds.left, frame.translation.x + frame.contentClip.left * frame.contentScale, 0.01f)
        assertEquals(frame.bounds.top, frame.translation.y + frame.contentClip.top * frame.contentScale, 0.01f)
        assertEquals(frame.bounds.right, frame.translation.x + frame.contentClip.right * frame.contentScale, 0.01f)
        assertEquals(frame.bounds.bottom, frame.translation.y + frame.contentClip.bottom * frame.contentScale, 0.01f)
    }
}
