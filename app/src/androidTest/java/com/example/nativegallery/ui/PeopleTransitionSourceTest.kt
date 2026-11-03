package com.example.nativegallery.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.nativegallery.data.PeopleRepository
import com.example.nativegallery.data.PersonGroup
import com.example.nativegallery.model.Album
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.model.MediaType
import com.example.nativegallery.ui.theme.GalleryTheme
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PeopleTransitionSourceTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun personReturnUsesTheFaceCropAndSquareFaceBoundsWithoutTheLabels() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val crop = File.createTempFile("person-transition-", ".jpg", context.cacheDir)
        val bitmap = Bitmap.createBitmap(112, 112, Bitmap.Config.ARGB_8888)
        crop.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        bitmap.recycle()
        val photo = MediaItem("fixture-photo", "fixture", MediaType.Photo, "Fixture", "Test",
            contentUri = Uri.parse("content://fixture/photo"))
        val person = PersonGroup("person:fixture", Uri.fromFile(crop), listOf(photo))
        val state = PeopleState(PeopleRepository(context))
        val sources = AlbumTransitionSources<GraphicsLayer>()
        val active = mutableStateOf<String?>(null)
        var bounds = Rect.Zero
        var opened: Album? = null
        try {
            composeRule.setContent {
                GalleryTheme(darkTheme = false) {
                    CompositionLocalProvider(LocalAlbumTransitionSources provides sources) {
                        PeopleSection(state, listOf(person), { album, rect -> opened = album; bounds = rect }, active)
                    }
                }
            }
            composeRule.waitUntil(5_000) {
                composeRule.onAllNodes(androidx.compose.ui.test.hasContentDescription("Person 1"))
                    .fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.onNodeWithContentDescription("Person 1").performClick()
            composeRule.runOnIdle {
                assertTrue(bounds.width > 0f)
                assertEquals(bounds.width, bounds.height, 0.1f)
                assertEquals(person.thumbnail, opened?.coverUri)
                assertNotEquals(photo.contentUri, opened?.coverUri)
                assertNotNull(sources.get(person.id))
                active.value = person.id
            }
            composeRule.waitForIdle()
            composeRule.runOnIdle { assertNotNull(sources.get(person.id)) }
        } finally {
            crop.delete()
        }
    }
}
