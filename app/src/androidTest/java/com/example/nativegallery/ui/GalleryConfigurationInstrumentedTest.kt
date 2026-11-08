package com.example.nativegallery.ui

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isFocused
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.example.nativegallery.MainActivity
import com.example.nativegallery.data.GallerySettingsRepository
import java.io.ByteArrayOutputStream
import java.util.UUID
import org.junit.Assume.assumeTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
class GalleryConfigurationInstrumentedTest {
    @org.junit.Before fun keepTestWindowAwake() = TestScreenAwake.enable()

    private val fixtureRule = AppOwnedPhotoFixtureRule()
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: TestRule = RuleChain
        .outerRule(fixtureRule)
        .around(composeRule)

    @Test
    fun bothAlbumAnimationsSupportRepeatedOpenAndClose() {
        val repository = GallerySettingsRepository(InstrumentationRegistry.getInstrumentation().targetContext)
        val previousSettings = repository.initialSettings()
        try {
            for (classic in listOf(false, true)) {
                repository.save(previousSettings.copy(useClassicAlbumAnimation = classic,
                    folderTransitionStyle = com.example.nativegallery.data.FolderTransitionStyle.Normal))
                composeRule.activityRule.scenario.recreate()
                composeRule.onNodeWithContentDescription("Albums").performClick()
                composeRule.onNode(hasSetTextAction()).performTextClearance()
                composeRule.onNode(hasSetTextAction()).performTextInput(FixtureAlbumName)
                composeRule.waitUntil(ConfigurationTimeoutMillis) {
                    composeRule.onAllNodesWithContentDescription(FixtureAlbumName).fetchSemanticsNodes().isNotEmpty()
                }
                repeat(3) {
                    composeRule.onNodeWithContentDescription(FixtureAlbumName).performClick()
                    composeRule.waitUntil(ConfigurationTimeoutMillis) {
                        composeRule.onAllNodesWithContentDescription("Back").fetchSemanticsNodes().isNotEmpty() &&
                            composeRule.onAllNodesWithTag(AlbumTransitionOverlayTestTag).fetchSemanticsNodes().isEmpty()
                    }
                    composeRule.onNodeWithContentDescription("Back").performClick()
                    composeRule.waitUntil(ConfigurationTimeoutMillis) {
                        composeRule.onAllNodesWithContentDescription("Back").fetchSemanticsNodes().isEmpty() &&
                            composeRule.onAllNodesWithTag(AlbumTransitionOverlayTestTag).fetchSemanticsNodes().isEmpty() &&
                            composeRule.onAllNodesWithContentDescription(FixtureAlbumName).fetchSemanticsNodes().isNotEmpty()
                    }
                }
            }
        } finally { repository.save(previousSettings) }
    }

    @Test
    fun cancelingEdgeBackKeepsTheAlbumAndTheNextCommittedBackStillWorks() {
        composeRule.onNodeWithContentDescription("Albums").performClick()
        composeRule.onNodeWithText("Search albums").performTextInput(FixtureAlbumName)
        composeRule.waitUntil(ConfigurationTimeoutMillis) {
            composeRule.onAllNodesWithContentDescription(FixtureAlbumName).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription(FixtureAlbumName).performClick()
        composeRule.onNodeWithContentDescription("Back").assertIsDisplayed()
        composeRule.runOnIdle {
            val dispatcher = composeRule.activity.onBackPressedDispatcher
            dispatcher.dispatchOnBackStarted(androidx.activity.BackEventCompat(0f, 1000f, 0f, androidx.activity.BackEventCompat.EDGE_LEFT))
            dispatcher.dispatchOnBackProgressed(androidx.activity.BackEventCompat(220f, 1000f, 0.5f, androidx.activity.BackEventCompat.EDGE_LEFT))
            dispatcher.dispatchOnBackCancelled()
        }
        composeRule.onNodeWithContentDescription("Back").assertIsDisplayed()
        composeRule.onNodeWithText(FixtureAlbumName).assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithTag(AlbumTransitionOverlayTestTag).fetchSemanticsNodes().isEmpty())
        composeRule.runOnIdle { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitUntil(ConfigurationTimeoutMillis) {
            composeRule.onAllNodesWithContentDescription("Back").fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun openViewerRemainsOpenAfterLandscapeRecreation() {
        composeRule.waitUntil(ConfigurationTimeoutMillis) {
            composeRule
                .onAllNodesWithContentDescription(fixtureRule.displayName)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithContentDescription(fixtureRule.displayName).performClick()
        composeRule.waitUntil(ConfigurationTimeoutMillis) {
            composeRule.onAllNodesWithContentDescription("Close media").fetchSemanticsNodes().isNotEmpty()
        }
        val mediaCopiesAfterOpen = composeRule
            .onAllNodesWithContentDescription(fixtureRule.displayName)
            .fetchSemanticsNodes()
            .size

        composeRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        val landscapeApplied = runCatching {
            composeRule.waitUntil(ConfigurationTimeoutMillis) {
                composeRule.activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            }
        }.isSuccess
        assumeTrue("The test device does not support runtime orientation changes", landscapeApplied)

        composeRule.onNodeWithContentDescription("Close media").assertIsDisplayed()

        composeRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        val portraitRestored = runCatching {
            composeRule.waitUntil(ConfigurationTimeoutMillis) {
                composeRule.activity.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
            }
        }.isSuccess
        assumeTrue("The test device could not restore portrait orientation", portraitRestored)

        composeRule.onNodeWithContentDescription("Close media").assertIsDisplayed()
        composeRule.onNodeWithTag(ViewerMediaStageTestTag).assertIsDisplayed()
        val mediaCopiesAfterRoundTrip = composeRule
            .onAllNodesWithContentDescription(fixtureRule.displayName)
            .fetchSemanticsNodes()
            .size
        assert(mediaCopiesAfterOpen > 0) { "The open viewer must render the selected photo" }
        assert(mediaCopiesAfterRoundTrip == mediaCopiesAfterOpen) {
            "Landscape-to-portrait must not leave a duplicate photo layer: " +
                "before=$mediaCopiesAfterOpen after=$mediaCopiesAfterRoundTrip"
        }
        assert(
            composeRule.onAllNodesWithTag(ViewerMediaStageTestTag)
                .fetchSemanticsNodes().size == 1
        ) { "Landscape-to-portrait must keep exactly one viewer media stage" }
        assertTrue("The open viewer must render the selected photo", mediaCopiesAfterOpen > 0)
        assertEquals(
            "Landscape-to-portrait must not leave a duplicate photo layer",
            mediaCopiesAfterOpen,
            mediaCopiesAfterRoundTrip
        )
        assertTrue(
            "Landscape-to-portrait must keep exactly one viewer media stage",
            composeRule.onAllNodesWithTag(ViewerMediaStageTestTag).fetchSemanticsNodes().size == 1
        )
    }

    @Test
    fun closingAlbumThenFastScrollingDoesNotLeaveTransitionOverlay() {
        composeRule.waitUntil(ConfigurationTimeoutMillis) {
            composeRule.onAllNodesWithContentDescription(fixtureRule.displayName)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("Albums").performClick()
        composeRule.onNode(hasSetTextAction()).performTextInput(FixtureAlbumName)
        composeRule.waitUntil(ConfigurationTimeoutMillis) {
            composeRule.onAllNodesWithContentDescription(FixtureAlbumName)
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithContentDescription(FixtureAlbumName).performClick()
        composeRule.waitUntil(ConfigurationTimeoutMillis) {
            composeRule.onAllNodesWithContentDescription("Back").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("Back").performClick()

        composeRule.onRoot().performTouchInput { swipeUp(durationMillis = 80L) }
        composeRule.waitUntil(ConfigurationTimeoutMillis) {
            composeRule.onAllNodesWithTag(AlbumTransitionOverlayTestTag)
                .fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNode(hasSetTextAction() and isFocused()).performTextClearance()

        composeRule.onRoot().performTouchInput {
            swipeUp(durationMillis = 80L)
            swipeDown(durationMillis = 80L)
        }
        composeRule.waitForIdle()
        assertTrue(
            "The album transition overlay must not reappear after return scrolling",
            composeRule.onAllNodesWithTag(AlbumTransitionOverlayTestTag).fetchSemanticsNodes().isEmpty()
        )
        composeRule.onNodeWithText("Search albums").assertIsDisplayed()
    }

    @Test
    fun openedAlbumSurvivesBackgroundAndRecreation() {
        composeRule.onNodeWithContentDescription("Albums").performClick()
        composeRule.waitUntil(ConfigurationTimeoutMillis) {
            composeRule.onAllNodesWithText("Search albums").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Search albums").performTextInput(FixtureAlbumName)
        composeRule.waitUntil(ConfigurationTimeoutMillis) {
            composeRule.onAllNodesWithContentDescription(FixtureAlbumName).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription(FixtureAlbumName).performClick()
        composeRule.waitUntil(ConfigurationTimeoutMillis) {
            composeRule.onAllNodesWithContentDescription("Back").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
        composeRule.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        composeRule.activityRule.scenario.recreate()
        composeRule.onNodeWithContentDescription("Back").assertIsDisplayed()
        composeRule.onNodeWithText(FixtureAlbumName).assertIsDisplayed()
    }
    private class AppOwnedPhotoFixtureRule : TestRule {
        lateinit var displayName: String
            private set

        override fun apply(base: Statement, description: Description): Statement {
            return object : Statement() {
                override fun evaluate() {
                    val instrumentation = InstrumentationRegistry.getInstrumentation()
                    val context = instrumentation.targetContext
                    val preferences = context.getSharedPreferences(
                        PermissionPromptPreferences,
                        Context.MODE_PRIVATE
                    )
                    val wasPromptHandled = preferences.getBoolean(InitialPromptHandled, false)
                    val navigation = context.getSharedPreferences("photo_lane_navigation", Context.MODE_PRIVATE)
                    val previousNavigation = navigation.all.toMap()
                    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Manifest.permission.READ_MEDIA_IMAGES
                    } else {
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    }
                    val permissionWasGranted =
                        context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
                    var fixtureUri: Uri? = null
                    displayName = "orientation-${UUID.randomUUID()}.png"

                    preferences.edit().putBoolean(InitialPromptHandled, true).commit()
                    if (!permissionWasGranted) {
                        instrumentation.uiAutomation.grantRuntimePermission(context.packageName, permission)
                    }
                    try {
                        navigation.edit().clear().commit()
                        fixtureUri = insertFixture(context, displayName)
                        base.evaluate()
                    } finally {
                        fixtureUri?.let { runCatching { context.contentResolver.delete(it, null, null) } }
                        preferences.edit().putBoolean(InitialPromptHandled, wasPromptHandled).commit()
                        val restoreNavigation = navigation.edit().clear()
                        previousNavigation.forEach { (key, value) ->
                            when (value) {
                                is String -> restoreNavigation.putString(key, value)
                                is Boolean -> restoreNavigation.putBoolean(key, value)
                            }
                        }
                        restoreNavigation.commit()
                        if (!permissionWasGranted) {
                            runCatching {
                                instrumentation.uiAutomation.revokeRuntimePermission(context.packageName, permission)
                            }
                        }
                    }
                }
            }
        }

        private fun insertFixture(context: Context, name: String): Uri {
            val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/NativeGalleryAndroidTests/"
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = requireNotNull(context.contentResolver.insert(collection, values))
            try {
                checkNotNull(context.contentResolver.openOutputStream(uri, "w")).use { output ->
                    output.write(createPngBytes())
                }
                check(
                    context.contentResolver.update(
                        uri,
                        ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
                        null,
                        null
                    ) == 1
                )
                return uri
            } catch (failure: Throwable) {
                runCatching { context.contentResolver.delete(uri, null, null) }
                throw failure
            }
        }

        private fun createPngBytes(): ByteArray {
            val bitmap = Bitmap.createBitmap(64, 48, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.rgb(23, 122, 173))
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

    private companion object {
        const val ConfigurationTimeoutMillis = 15_000L
        const val PermissionPromptPreferences = "native_gallery_permission_prompt"
        const val InitialPromptHandled = "initial_prompt_handled"
        const val FixtureAlbumName = "NativeGalleryAndroidTests"
        const val AlbumTransitionOverlayTestTag = "AlbumTransitionOverlay"
    }
}
