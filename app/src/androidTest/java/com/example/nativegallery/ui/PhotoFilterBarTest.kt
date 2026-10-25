package com.example.nativegallery.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.nativegallery.ui.theme.GalleryTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhotoFilterBarTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun filtersAnnounceSelectionAndDispatchEachTapOnce() {
        val selections = mutableListOf<PhotoGridFilter>()
        composeRule.setContent {
            GalleryTheme(darkTheme = true) {
                var selected by remember { mutableStateOf(PhotoGridFilter.All) }
                PhotoFilterBar(selectedFilter = selected, onFilterSelected = {
                    selections += it
                    selected = it
                })
            }
        }

        composeRule.onNodeWithText("All").assertIsSelected()
        PhotoGridFilter.entries.forEach { filter ->
            composeRule.onNodeWithText(filter.label).performClick().assertIsSelected()
            PhotoGridFilter.entries.filter { it != filter }.forEach {
                composeRule.onNodeWithText(it.label).assertIsNotSelected()
            }
        }
        composeRule.runOnIdle { assertEquals(PhotoGridFilter.entries.toList(), selections) }
    }

    @Test
    fun narrowLayoutWithLargeTextKeepsEveryLabelAndTouchTargetVisible() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                GalleryTheme(darkTheme = false) {
                    Box(Modifier.width(280.dp)) {
                        PhotoFilterBar(PhotoGridFilter.Favorites, onFilterSelected = {})
                    }
                }
            }
        }

        PhotoGridFilter.entries.forEach { filter ->
            composeRule.onNodeWithText(filter.label)
                .assertIsDisplayed()
                .assertHeightIsAtLeast(48.dp)
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
                    val results = mutableListOf<TextLayoutResult>()
                    action(results)
                    val layout = results.single()
                    assertFalse("${filter.label} loses a line", layout.multiParagraph.didExceedMaxLines)
                    assertEquals(filter.label.length, layout.getLineEnd(0, visibleEnd = true))
                    // The paragraph can retain the parent's width after intrinsic text measurement;
                    // compare visible line bounds with the measured text bounds instead.
                    val lineWidth = layout.getLineRight(0) - layout.getLineLeft(0)
                    assertTrue("${filter.label} is clipped horizontally: $lineWidth / ${layout.size.width}", lineWidth <= layout.size.width + 1f)
                    assertTrue("${filter.label} is clipped vertically", layout.getLineBottom(0) <= layout.size.height + 1f)
                }
        }
    }
}
