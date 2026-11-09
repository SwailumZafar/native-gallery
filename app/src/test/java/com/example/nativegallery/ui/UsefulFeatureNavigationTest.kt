package com.example.nativegallery.ui

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.*
import org.junit.Test

class UsefulFeatureNavigationTest {
    @Test fun newFeaturesReturnToTheirLaunchingPageAfterRecreation() {
        for (destination in listOf(GalleryDestination.Moments, GalleryDestination.UsefulScreenshots)) {
            val saved = SavedStateHandle()
            val original = GalleryNavigationViewModel(saved)
            original.openTab(GalleryTab.Menu)
            original.openOverlay(destination)
            val restored = SavedStateHandle().also { copy -> saved.keys().forEach { key -> copy[key] = saved.get<Any?>(key) } }
            val recreated = GalleryNavigationViewModel(restored)
            assertEquals(destination, recreated.uiState.value.destination)
            assertEquals(GalleryBackAction.ReturnToMenu, resolveGalleryBackAction(destination, GalleryTab.Menu,
                false, false, false, false, false, false, false))
            recreated.returnFromOverlay(GalleryTab.Menu)
            assertEquals(GalleryDestination.Main, recreated.uiState.value.destination)
            assertEquals(GalleryTab.Menu, recreated.uiState.value.selectedTab)
        }
    }
    @Test fun privatePreviewIgnoresLetterboxingAndClampsDraggedEndpoints() {
        assertNull(privatePreviewPoint(50f, 10f, 100f, 200f, 200f, 100f))
        val center = privatePreviewPoint(50f, 100f, 100f, 200f, 200f, 100f)!!
        assertEquals(0.5f, center.first, 0.0001f); assertEquals(0.5f, center.second, 0.0001f)
        val outside = privatePreviewPoint(150f, 250f, 100f, 200f, 200f, 100f, clamp = true)!!
        assertEquals(1f, outside.first, 0f); assertEquals(1f, outside.second, 0f)
        assertNull(privatePreviewPoint(0f, 0f, 0f, 100f, 100f, 100f))
    }
}
