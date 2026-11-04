package com.example.nativegallery.ui

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.nativegallery.R
import com.example.nativegallery.data.AlbumArrangementRepository
import com.example.nativegallery.data.AlbumSortMode
import com.example.nativegallery.data.PeopleRepository
import com.example.nativegallery.model.Album
import com.example.nativegallery.model.AlbumLayoutMode
import com.example.nativegallery.ui.theme.GalleryTheme
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GalleryPolishTest {
    @org.junit.Before fun keepTestWindowAwake() = TestScreenAwake.enable()
    @get:Rule val compose = createComposeRule()
    private val base get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun completedScanRemovesProgressAndEnablesScanAgain() {
        val state = PeopleState(PeopleRepository(base)).apply { enabled = true; scanning = true; scanned = 5; total = 10 }
        compose.setContent { GalleryTheme(darkTheme = false) { PeopleSection(state, emptyList(), { _, _ -> }) } }
        val progress = SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)
        compose.onAllNodes(progress).assertCountEquals(1)
        compose.runOnIdle { state.publish(emptyList(), 10, 10) }
        compose.onAllNodes(progress).assertCountEquals(0)
        compose.onNodeWithText("Scan").assertIsEnabled()
    }

    @Test fun smartAlbumCanBeRemovedAndAlbumSortIsSaved() {
        val preferencesName = "album-polish-${System.nanoTime()}"
        val context = object : ContextWrapper(base) {
            override fun getApplicationContext(): Context = this
            override fun getSharedPreferences(name: String, mode: Int) = base.getSharedPreferences(preferencesName, mode)
        }
        var albums by mutableStateOf(listOf(Album("smart:fixture", "Recent videos", 0, emptyList(), isSmart = true)))
        try {
            compose.setContent {
                CompositionLocalProvider(LocalContext provides context) {
                    GalleryTheme(darkTheme = false) {
                        AlbumsScreen(albums, AlbumLayoutMode.Basic, {}, {}, {}, {},
                            onDeleteAlbum = { selected -> albums = albums.filterNot { it.id == selected.id } },
                            onAlbumClick = { _, _ -> }, onAlbumBoundsChanged = { _, _ -> },
                            contentPadding = PaddingValues(0.dp), listState = rememberLazyListState())
                    }
                }
            }
            compose.onNodeWithContentDescription("Album options").performClick()
            compose.onNodeWithText("Sort albums").performClick()
            compose.mainClock.advanceTimeBy(800)
            compose.waitForIdle()
            save(compose.onNodeWithTag("gallery-sort-sheet").captureToImage().asAndroidBitmap(), "photo-lane-sort-sheet.png")
            compose.onNodeWithText("Most items").performClick()
            compose.runOnIdle { assertEquals(AlbumSortMode.MostItems, AlbumArrangementRepository(context).load().sortMode) }
            compose.onNodeWithContentDescription("Album options").performClick()
            compose.onNodeWithText("Manage smart albums").performClick()
            compose.onNodeWithContentDescription("Remove smart album Recent videos").performClick()
            compose.onNodeWithText("Remove").performClick()
            compose.runOnIdle { assertTrue(albums.isEmpty()) }
            compose.onNodeWithText("No smart albums yet.").assertIsDisplayed()
        } finally { base.deleteSharedPreferences(preferencesName) }
    }

    @Test fun recentlyDeletedEmptyStateRendersAndLauncherScalesCleanly() {
        compose.setContent {
            GalleryTheme(darkTheme = false) {
                RecentlyDeletedScreen(emptyList(), {}, { _, _ -> }, {}, {}, PaddingValues(0.dp))
            }
        }
        compose.onNodeWithText("Recently deleted is empty").assertIsDisplayed()
        val screenshot = compose.onRoot().captureToImage().asAndroidBitmap()
        save(screenshot, "photo-lane-recently-deleted.png")
        val preview = Bitmap.createBitmap(768, 660, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(preview).apply { drawColor(Color.rgb(238, 240, 245)) }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.DKGRAY; textSize = 20f }
        listOf(48, 72, 96, 192, 384).forEachIndexed { index, size ->
            val x = if (index < 3) 32 + index * 220 else if (index == 3) 32 else 320
            val y = if (index < 3) 40 else 250
            canvas.drawText("${size}px", x.toFloat(), (y - 12).toFloat(), paint)
            val icon = requireNotNull(base.getDrawable(R.mipmap.ic_launcher))
            icon.setBounds(x, y, x + size, y + size)
            icon.draw(canvas)
            assertEquals(255, Color.alpha(preview.getPixel(x + size / 2, y + size / 2)))
        }
        save(preview, "photo-lane-icon-sizes.png")
        preview.recycle()
    }

    private fun save(bitmap: Bitmap, name: String) {
        File(base.getExternalFilesDir(null), name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
