@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)

package com.example.nativegallery.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.nativegallery.data.PrivacyMaskKind
import com.example.nativegallery.data.ScreenshotCategory
import com.example.nativegallery.data.DocumentPhotoMatch
import com.example.nativegallery.data.DocumentPhotoCategory
import com.example.nativegallery.data.MomentLibrary
import com.example.nativegallery.data.MomentRepository
import com.example.nativegallery.data.SavedMoment
import com.example.nativegallery.model.Album
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.model.MediaType
import com.example.nativegallery.ui.theme.GalleryTheme
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.runBlocking

/** The host renders generated media only; it never opens MainActivity or the user's library. */
@RunWith(AndroidJUnit4::class)
class UsefulFeaturesUiTest {
    @get:Rule val rule = createComposeRule()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val app get() = instrumentation.targetContext.applicationContext as Application
    private val store = ViewModelStore()
    private val key = UUID.randomUUID().toString()
    private val preferenceName = "moments-ui-$key"
    private val databaseName = "screenshots-ui-$key.db"
    private lateinit var directory: File
    private lateinit var media: List<MediaItem>
    private var fontResources: android.content.res.Resources? = null
    private var fontConfiguration: android.content.res.Configuration? = null

    @Before fun prepare() {
        TestScreenAwake.enable()
        directory = File(app.cacheDir, "private_shares/ui-test-$key").apply { mkdirs() }
        media = (0..3).map { index ->
            val bitmap = Bitmap.createBitmap(640, 960, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(if (index == 3) Color.WHITE else Color.rgb(70 + index * 45, 140, 175 - index * 30))
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 34f }
            if (index == 3) listOf("Concert ticket", "Seat A12", "12 NOV 2030", "Reference 456789").forEachIndexed { row, text ->
                canvas.drawText(text, 28f, 120f + row * 70f, paint)
            }
            val name = if (index == 3) "Screenshot-fixture-$key.jpg" else "Trip-fixture-$index.jpg"
            val file = File(directory, name)
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
            bitmap.recycle()
            MediaItem("fixture-$key-$index", "fixture", MediaType.Photo, name, "9 Nov 2026",
                contentUri = FileProvider.getUriForFile(app, "${app.packageName}.private_shares", file),
                relativePath = if (index == 3) "Pictures/Screenshots/" else "DCIM/Camera/",
                sortTimestampMillis = Instant.parse("2026-11-09T10:00:00Z").toEpochMilli() + index * 3_600_000L,
                fileSizeBytes = file.length(), width = 640, height = 960)
        }
    }
    private fun own(model: ViewModel) { instrumentation.runOnMainSync { store.put(key, model) } }
    @After fun clean() {
        instrumentation.runOnMainSync { store.clear() }
        fontConfiguration?.let { configuration -> instrumentation.runOnMainSync {
            @Suppress("DEPRECATION")
            fontResources?.updateConfiguration(configuration, fontResources?.displayMetrics)
        } }
        app.deleteSharedPreferences(preferenceName)
        app.deleteDatabase(databaseName)
        app.deleteDatabase("${preferenceName}_content.db")
        directory.deleteRecursively()
    }
    private fun save(tag: String, name: String) {
        val bitmap = rule.onNodeWithTag(tag).captureToImage().asAndroidBitmap()
        File(app.getExternalFilesDir(null), name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun momentsCanBeCreatedCuratedAndOpenedWithoutMovingPhotos() {
        val model = MomentsViewModel(app, preferenceName); own(model)
        var opened: MediaItem? = null
        rule.setContent { GalleryTheme(darkTheme = false) {
            Box(Modifier.fillMaxSize().testTag("moments-fixture")) { MomentsScreen(media, emptySet(), {}, { item, _, _ -> opened = item }, model) }
        } }
        rule.onNodeWithText("Create").performClick()
        rule.onNodeWithText("Moment name").performTextInput("Fixture trip")
        rule.onNodeWithText("A note to remember").performTextInput("A day by the lake")
        media.take(3).forEach { rule.onNodeWithContentDescription(it.title).performClick() }
        rule.onNodeWithContentDescription(media[1].title).performTouchInput { longClick() }
        rule.onNodeWithText("Save").performClick()
        rule.waitUntil(15_000) { !model.uiState.value.loading && model.uiState.value.cards.any { it.name == "Fixture trip" && it.saved } }
        rule.onNodeWithText("Fixture trip").assertIsDisplayed()
        save("moments-fixture", "feature-ui-moments-fixture.png")
        val moment = model.uiState.value.cards.single { it.saved }
        assertEquals(3, moment.media.size); assertEquals(media[1].id, moment.cover.id)
        // Stories have a live playback clock; drive it explicitly during this handoff.
        rule.mainClock.autoAdvance = false
        rule.onNodeWithText("Open moment").performClick()
        rule.mainClock.advanceTimeBy(300)
        rule.onNodeWithText("View all photos").performClick()
        rule.mainClock.advanceTimeBy(300)
        rule.mainClock.autoAdvance = true
        rule.onNodeWithContentDescription(media[0].title).performClick()
        assertEquals(media[0].id, opened?.id)
        assertEquals(4, directory.listFiles()!!.size)
    }

    @Test fun privateSharingPreviewsManualRedactionAndKeepsTheOriginal() {
        val model = PrivateSharingViewModel(app, SavedStateHandle()); own(model)
        val original = File(directory, media[0].title).readBytes()
        rule.setContent { GalleryTheme(darkTheme = false) { PrivateSharingDialog(key, listOf(media[0].id), media, {}, model) } }
        rule.waitUntil(30_000) { model.uiState.value.preview != null && !model.uiState.value.rendering }
        rule.onNodeWithContentDescription("Private share preview").assertIsDisplayed().performTouchInput { click(center) }
        rule.waitUntil(10_000) { model.uiState.value.regions.any { it.kind == PrivacyMaskKind.Manual } && !model.uiState.value.rendering }
        rule.onNodeWithText("Undo area").assertIsEnabled()
        val beforeToggle = rule.onNodeWithTag("private-preview-area").fetchSemanticsNode().boundsInRoot
        rule.onNodeWithText("Solid covers").performClick()
        rule.waitUntil(10_000) { !model.uiState.value.rendering }
        rule.onNodeWithText("Share copy").assertIsEnabled()
        assertEquals(beforeToggle, rule.onNodeWithTag("private-preview-area").fetchSemanticsNode().boundsInRoot)
        save("useful-feature-dialog", "feature-ui-private-fixture.png")
        rule.onNodeWithText("Undo area").performClick()
        assertArrayEquals(original, File(directory, media[0].title).readBytes())
    }

    @Test fun screenshotsScanSearchAndPersistShelfActions() {
        val model = ScreenshotsViewModel(app, databaseName); own(model)
        var screenFocus: androidx.compose.ui.focus.FocusManager? = null
        rule.setContent { GalleryTheme(darkTheme = false) {
            screenFocus = androidx.compose.ui.platform.LocalFocusManager.current
            Box(Modifier.fillMaxSize().testTag("screenshots-fixture")) { UsefulScreenshotsScreen(media, {}, { _, _, _ -> }, model) }
        } }
        rule.waitUntil(10_000) { model.uiState.value.entries.size == 1 }
        rule.onNodeWithText("Other").performScrollTo().performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Details").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Details").performClick()
        rule.onNode(hasText("Orders") and hasClickAction() and hasAnyAncestor(hasTestTag("useful-feature-dialog"))).performScrollTo().performClick()
        rule.onNode(hasContentDescription("Back") and hasAnyAncestor(hasTestTag("useful-feature-dialog"))).performClick()
        rule.onNodeWithText("Useful").performScrollTo().performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Details").fetchSemanticsNodes().isNotEmpty() }
        assertFalse(model.uiState.value.entries.single().indexed)
        rule.onNodeWithText("Scan screenshots").performClick()
        rule.waitUntil(30_000) { model.uiState.value.entries.single().indexed && !model.uiState.value.scanning }
        rule.onNode(hasSetTextAction()).performTextInput("Concert")
        rule.runOnIdle { screenFocus?.clearFocus(force = true) }
        rule.onNodeWithText("Details").performClick()
        rule.onNodeWithText("Pin").performClick()
        rule.onNodeWithText("Mark done").performClick()
        rule.waitUntil(10_000) { model.uiState.value.entries.single().done }
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Details").fetchSemanticsNodes().isEmpty() }
        rule.onNodeWithText("Done").performScrollTo().performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Details").fetchSemanticsNodes().isNotEmpty() }
        save("screenshots-fixture", "feature-ui-screenshots-fixture.png")
        rule.onNodeWithText("Details").performClick()
        rule.onNodeWithText("Set follow-up").assertIsDisplayed()
        rule.onNode(hasText("Orders") and hasClickAction() and hasAnyAncestor(hasTestTag("useful-feature-dialog"))).performScrollTo().performClick()
        assertEquals(ScreenshotCategory.Orders, model.uiState.value.entries.single().displayCategory)
        assertTrue(model.uiState.value.entries.single().pinned)
        rule.onNodeWithText("Copy text").assertIsEnabled()
    }

    @Test fun documentChipsScrollToSectionsWithoutFilteringOrThumbnailBadges() {
        val bills = (0..35).map { DocumentPhotoMatch(media[0].copy(id = "bill-$it"), DocumentPhotoCategory.BillsReceipts, "Bill $it", 5) }
        val menus = (0..7).map { DocumentPhotoMatch(media[1].copy(id = "menu-$it"), DocumentPhotoCategory.Menus, "Menu $it", 5) }
        rule.setContent { GalleryTheme(darkTheme = false) {
            Box(Modifier.fillMaxSize().testTag("documents-fixture")) {
                DocumentPhotosScreen(bills + menus, false, 44, 44, null, {}, {}, { _, _, _ -> }, 3, 44)
            }
        } }
        rule.onNodeWithTag("document-section-BillsReceipts").assertIsDisplayed()
        rule.onNodeWithText("Menus (8)").performScrollTo().performClick()
        rule.onNodeWithTag("document-section-Menus").assertIsDisplayed()
        save("documents-fixture", "feature-ui-documents-fixture.png")
        rule.onNodeWithText("Bills & receipts (36)").performScrollTo().performClick()
        rule.onNodeWithTag("document-section-BillsReceipts").assertIsDisplayed()
        rule.onNodeWithText("Bill 0").assertDoesNotExist()
    }

    @Test fun scanCounterWalksEveryNumberInsteadOfJumpingABatch() {
        var count by mutableIntStateOf(0)
        rule.mainClock.autoAdvance = false
        rule.setContent { GalleryTheme { LiveScanCounter(count, 40) } }
        val initialBounds = rule.onNodeWithTag("live-scan-counter").fetchSemanticsNode().boundsInRoot
        rule.runOnIdle { count = 40 }
        rule.mainClock.advanceTimeBy(96)
        val intermediate = rule.onNodeWithTag("live-scan-counter").fetchSemanticsNode().config[SemanticsProperties.StateDescription].substringBefore('/').toInt()
        assertTrue("Counter should be between batch endpoints: $intermediate", intermediate in 1..10)
        assertEquals(initialBounds, rule.onNodeWithTag("live-scan-counter").fetchSemanticsNode().boundsInRoot)
        rule.mainClock.advanceTimeBy(900)
        rule.onNodeWithTag("live-scan-counter").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "40/40"))
        assertEquals(initialBounds, rule.onNodeWithTag("live-scan-counter").fetchSemanticsNode().boundsInRoot)
        rule.mainClock.autoAdvance = true
    }

    @Test fun albumsHoldOpensLockedShortcutOnceAndDraggingStillSelectsTabs() {
        var holds = 0; var taps = 0; var haptics = 0
        val feedback = object : HapticFeedback { override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) { haptics++ } }
        rule.setContent { GalleryTheme {
            CompositionLocalProvider(LocalHapticFeedback provides feedback) {
                GalleryBottomBar(GalleryTab.Photos, true, onAlbumsLongPress = { holds++ }, onTabSelected = { taps++ })
            }
        } }
        rule.onNodeWithContentDescription("Albums").performTouchInput { longClick() }
        rule.runOnIdle { assertEquals(1, holds); assertEquals(0, taps); assertEquals(1, haptics) }
        rule.onNodeWithContentDescription("Albums").performClick()
        rule.runOnIdle { assertEquals(1, taps) }
        rule.onNodeWithContentDescription("Albums").performTouchInput { swipeLeft(durationMillis = 150) }
        rule.runOnIdle { assertEquals(1, holds); assertEquals(2, taps) }
    }

    @Test fun hiddenSwitchAndRowEachGiveOneHaptic() {
        var hidden by mutableStateOf(false); var haptics = 0
        val feedback = object : HapticFeedback { override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) { haptics++ } }
        val album = Album("fixture", "Fixture hidden album", 4, emptyList())
        val states = mutableStateMapOf(album.id to false)
        rule.setContent { GalleryTheme { CompositionLocalProvider(LocalHapticFeedback provides feedback) {
            HiddenItemsScreen(listOf(album), states, if (hidden) 1 else 0, if (hidden) 4 else 0,
                {}, { _, value -> hidden = value; states[album.id] = value }, PaddingValues())
        } } }
        rule.onNode(isToggleable()).performScrollTo().performClick()
        rule.runOnIdle { assertTrue(hidden); assertEquals(1, haptics) }
        rule.onNodeWithText(album.name).performClick()
        rule.runOnIdle { assertFalse(hidden); assertEquals(2, haptics) }
    }

    @Test fun privateBatchKeepsMasksWhenMovingBetweenSelectedPhotos() {
        val model = PrivateSharingViewModel(app, SavedStateHandle()); own(model)
        rule.setContent { GalleryTheme { PrivateSharingDialog(key, media.take(2).map { it.id }, media, {}, model) } }
        rule.waitUntil(30_000) { model.uiState.value.preview != null && !model.uiState.value.rendering }
        rule.onNodeWithContentDescription("Private share preview").performTouchInput { click(center) }
        rule.waitUntil(10_000) { model.uiState.value.regions.any { it.kind == PrivacyMaskKind.Manual } && !model.uiState.value.rendering }
        val masks = model.uiState.value.regions
        rule.onNodeWithContentDescription("Selected photo 2 of 2").performClick()
        rule.waitUntil(30_000) { model.uiState.value.index == 1 && model.uiState.value.preview != null && !model.uiState.value.rendering }
        rule.onNodeWithText("Share 2 copies").assertIsEnabled()
        rule.onNodeWithText("Previous").performClick()
        rule.waitUntil(15_000) { model.uiState.value.index == 0 && model.uiState.value.preview != null && !model.uiState.value.rendering }
        assertEquals(masks, model.uiState.value.regions)
        rule.runOnIdle { model.updateAvailable(media.drop(1)); model.setSolid(true); model.share() }
        assertNotNull(model.uiState.value.error)
        assertTrue(model.uiState.value.shareUris.isEmpty())
    }

    @Test fun dismissingPhotoStaysInsideMomentAtTheSameScrollPosition() {
        val photos = (0..35).map { index -> media[0].copy(id = "memory-$index", title = "Memory photo $index", sortTimestampMillis = media[0].sortTimestampMillis + index * 60_000) }
        runBlocking { MomentRepository(app, preferenceName).save(MomentLibrary(listOf(SavedMoment("navigation", "Fixture navigation", photos.map { it.id })))) }
        val model = MomentsViewModel(app, preferenceName); own(model)
        var viewerOpen by mutableStateOf(false)
        var dispatcher: androidx.activity.OnBackPressedDispatcher? = null
        rule.setContent { GalleryTheme {
            dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
            BackHandler(viewerOpen) { viewerOpen = false }
            MomentsScreen(photos, emptySet(), {}, { _, _, _ -> viewerOpen = true }, model, viewerOpen)
        } }
        rule.waitUntil(10_000) { model.uiState.value.cards.any { it.id == "navigation" } }
        rule.mainClock.autoAdvance = false
        rule.onNodeWithText("Open moment").performClick()
        rule.mainClock.advanceTimeBy(300)
        rule.onNodeWithContentDescription("Close story").performClick()
        rule.mainClock.advanceTimeBy(300)
        rule.mainClock.autoAdvance = true
        rule.onNodeWithText("All photos").performClick()
        rule.onNodeWithTag("moment-photos").performScrollToIndex(24)
        rule.onNodeWithContentDescription("Memory photo 24").performClick()
        rule.runOnIdle { assertTrue(viewerOpen); requireNotNull(dispatcher).onBackPressed() }
        rule.runOnIdle { assertFalse(viewerOpen) }
        rule.onNodeWithText("Fixture navigation").assertIsDisplayed()
        rule.onNodeWithContentDescription("Memory photo 24").assertIsDisplayed()
        rule.onNodeWithText("Open moment").assertDoesNotExist()
        rule.runOnIdle { requireNotNull(dispatcher).onBackPressed() }
        rule.onNodeWithText("Open moment").assertIsDisplayed()
    }

    @Test fun storyCanPauseSkipAndDismissIntoAllPhotos() {
        val card = MomentCard("story-fixture", "A day together", media.take(3), media.take(3), media[0], true)
        var visible by mutableStateOf(true)
        rule.mainClock.autoAdvance = false
        rule.setContent { GalleryTheme { if (visible) MomentStory(card) { visible = false } } }
        rule.mainClock.advanceTimeBy(300)
        rule.onNodeWithTag("moment-story").assertIsDisplayed()
        rule.onNodeWithContentDescription("Pause story").performClick()
        rule.mainClock.advanceTimeBy(6_000)
        rule.onNodeWithText("1 of 3 · Hold to pause").assertIsDisplayed()
        rule.onNodeWithContentDescription("Next story photo").performClick()
        rule.mainClock.advanceTimeBy(300)
        rule.onNodeWithText("2 of 3 · Hold to pause").assertIsDisplayed()
        save("moment-story", "feature-ui-story-fixture.png")
        rule.onNodeWithText("View all photos").performClick()
        rule.mainClock.advanceTimeBy(300)
        rule.runOnIdle { assertFalse(visible) }
        rule.mainClock.autoAdvance = true
    }

    @Test fun privateReviewStaysVisibleWithLargeTextAndGivesHaptics() {
        val model = PrivateSharingViewModel(app, SavedStateHandle()); own(model)
        var haptics = 0
        val feedback = object : HapticFeedback { override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) { haptics++ } }
        instrumentation.runOnMainSync {
            val activity = androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED).first()
            fontResources = activity.resources
            fontConfiguration = android.content.res.Configuration(activity.resources.configuration)
            @Suppress("DEPRECATION")
            activity.resources.updateConfiguration(android.content.res.Configuration(activity.resources.configuration).apply { fontScale = 1.4f }, activity.resources.displayMetrics)
        }
        rule.setContent { GalleryTheme {
            CompositionLocalProvider(LocalHapticFeedback provides feedback) {
                PrivateSharingDialog(key, media.take(2).map { it.id }, media, {}, model)
            }
        } }
        rule.waitUntil(30_000) { model.uiState.value.preview != null && !model.uiState.value.rendering }
        val area = rule.onNodeWithTag("private-preview-area").fetchSemanticsNode().boundsInRoot
        assertTrue(area.height > 100 && area.width > 100)
        rule.onNodeWithText("Solid covers").performScrollTo().performClick()
        rule.waitUntil(10_000) { !model.uiState.value.rendering }
        rule.runOnIdle { assertEquals(1, haptics) }
        rule.onNodeWithContentDescription("Selected photo 2 of 2").performClick()
        rule.waitUntil(30_000) { model.uiState.value.index == 1 && model.uiState.value.preview != null && !model.uiState.value.rendering }
        rule.onNodeWithText("Previous").assertIsDisplayed()
        rule.onNodeWithText("Share 2 copies").assertIsDisplayed()
        save("useful-feature-dialog", "feature-ui-private-large-font-fixture.png")
    }
}
