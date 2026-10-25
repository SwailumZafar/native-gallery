package com.example.nativegallery.ui

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.example.nativegallery.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement

/** Uses only its own temporary MediaStore images; no user media is edited or deleted. */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 29)
class MediaViewerReturnRegressionTest {
    private val composeRule = createAndroidComposeRule<MainActivity>()
    private val fixtures = Fixtures()
    @get:Rule val rules: TestRule = RuleChain.outerRule(fixtures).around(composeRule)

    @Test
    fun tappingTheOpeningMediaAgainCannotRestartItsSpring() {
        waitForTile(0)
        val source = tile(0).fetchSemanticsNode().boundsInRoot.center
        composeRule.mainClock.autoAdvance = false
        composeRule.onRoot().performTouchInput { click(source) }
        composeRule.mainClock.advanceTimeBy(96)
        composeRule.waitForIdle()
        val opening = SemanticsMatcher("media opening") {
            it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("media-open-transition-") == true
        }
        val firstKey = composeRule.onNode(opening).fetchSemanticsNode().config[SemanticsProperties.TestTag]
        repeat(3) {
            composeRule.onRoot().performTouchInput { click(source) }
            composeRule.mainClock.advanceTimeBy(16)
            assertEquals(firstKey, composeRule.onNode(opening).fetchSemanticsNode().config[SemanticsProperties.TestTag])
        }
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(ViewerMediaStageTestTag).assertIsDisplayed()
    }

    @Test
    fun photosReturnToTheCurrentTileAcrossDateSectionsForDragAndBack() {
        openFirstPhoto()
        browseTo(42)
        dismissByDrag()
        assertReturnedTile(42)

        // Browse back above the new opening row as well as below the original one.
        tile(42).performTouchInput { click() }
        waitForViewerPhoto(42)
        selectFilmstripPhoto(40)
        composeRule.onNodeWithContentDescription("Close media").performClick()
        assertReturnedTile(40)
    }

    @Test
    fun albumsAlsoRevealTheCurrentTileBeforeDragClosing() {
        waitForTile(0)
        composeRule.onNodeWithContentDescription("Albums").performClick()
        composeRule.waitUntil(15_000) {
            composeRule.onAllNodes(androidx.compose.ui.test.hasText(fixtures.folder)).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(fixtures.folder).performTouchInput { click() }
        openFirstPhoto()
        browseTo(42)
        dismissByDrag()
        assertReturnedTile(42)
    }

    private fun tile(index: Int) = composeRule.onAllNodesWithContentDescription(fixtures.names[index])[0]

    private fun waitForTile(index: Int) {
        composeRule.waitUntil(15_000) {
            composeRule.onAllNodesWithContentDescription(fixtures.names[index]).fetchSemanticsNodes().isNotEmpty()
        }
        tile(index).assertIsDisplayed()
    }

    private fun openFirstPhoto() {
        waitForTile(0)
        tile(0).performTouchInput { click() }
        waitForViewerPhoto(0)
    }

    private fun waitForViewerPhoto(index: Int) {
        val matcher = hasContentDescription(fixtures.names[index]) and hasAnyAncestor(hasTestTag(ViewerZoomableMediaTestTag))
        composeRule.waitUntil(10_000) { composeRule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }
        composeRule.waitForIdle()
    }

    private fun browseTo(index: Int) {
        for (next in 3..index step 3) selectFilmstripPhoto(next)
    }

    private fun selectFilmstripPhoto(index: Int) {
        composeRule.onNode(hasContentDescription(fixtures.names[index]) and
            hasAnyAncestor(hasTestTag(ViewerFilmstripSectionTestTag))).performClick()
        waitForViewerPhoto(index)
    }

    private fun dismissByDrag() {
        composeRule.onNodeWithTag(ViewerMediaStageTestTag).performTouchInput {
            swipe(center, Offset(centerX, height * 0.88f), durationMillis = 350)
        }
    }

    private fun assertReturnedTile(index: Int) {
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodes(hasTestTag(ViewerMediaStageTestTag)).fetchSemanticsNodes().isEmpty()
        }
        composeRule.waitForIdle()
        waitForTile(index)
        val bounds = tile(index).fetchSemanticsNode().boundsInRoot
        val root = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
        assertTrue("Returned tile must be fully visible: $bounds in $root", bounds.isVisibleGridReturnTarget(root))
    }

    private class Fixtures : TestRule {
        val folder = "TransitionRegression-${System.nanoTime()}"
        val names = List(48) { "${folder}-${it.toString().padStart(2, '0')}.png" }
        override fun apply(base: Statement, description: Description) = object : Statement() {
            override fun evaluate() {
                val context = InstrumentationRegistry.getInstrumentation().targetContext
                val preferences = context.getSharedPreferences("native_gallery_permission_prompt", Context.MODE_PRIVATE)
                val handled = preferences.getBoolean("initial_prompt_handled", false)
                val uris = mutableListOf<Uri>()
                val bitmap = Bitmap.createBitmap(128, 96, Bitmap.Config.ARGB_8888)
                try {
                    preferences.edit().putBoolean("initial_prompt_handled", true).commit()
                    // Keep the two fixture date sections ahead of any pre-existing emulator media.
                    val now = System.currentTimeMillis() + 3 * 86_400_000L
                    names.forEachIndexed { index, name ->
                        bitmap.eraseColor(if (index % 2 == 0) android.graphics.Color.BLUE else android.graphics.Color.RED)
                        val values = ContentValues().apply {
                            put(MediaStore.Images.Media.DISPLAY_NAME, name)
                            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/$folder/")
                            put(MediaStore.Images.Media.DATE_TAKEN, now - (if (index < 24) 0 else 86_400_000L) - index * 1_000L)
                            put(MediaStore.Images.Media.WIDTH, 128)
                            put(MediaStore.Images.Media.HEIGHT, 96)
                            put(MediaStore.Images.Media.IS_PENDING, 1)
                        }
                        val uri = checkNotNull(context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
                        uris += uri
                        context.contentResolver.openOutputStream(uri)!!.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                        context.contentResolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
                    }
                    base.evaluate()
                } finally {
                    bitmap.recycle()
                    uris.forEach { context.contentResolver.delete(it, null, null) }
                    preferences.edit().putBoolean("initial_prompt_handled", handled).commit()
                }
            }
        }
    }
}
