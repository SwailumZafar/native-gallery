package com.example.nativegallery.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.example.nativegallery.data.GalleryPalette
import com.example.nativegallery.ui.theme.galleryColorScheme
import org.junit.Assert.assertTrue
import org.junit.Test

class GalleryThemeContrastTest {
    @Test fun everyPaletteKeepsBodyTextAndSelectedControlsReadableInBothModes() {
        GalleryPalette.entries.forEach { palette ->
            listOf(false, true).forEach { dark ->
                val scheme = galleryColorScheme(palette, dark)
                listOf(
                    scheme.onBackground to scheme.background,
                    scheme.onSurface to scheme.surface,
                    scheme.onSurfaceVariant to scheme.surfaceContainerLow,
                    scheme.onSurfaceVariant to scheme.primaryContainer,
                    scheme.onPrimary to scheme.primary,
                    scheme.onPrimaryContainer to scheme.primaryContainer,
                    scheme.onSecondaryContainer to scheme.secondaryContainer
                ).forEach { (text, background) ->
                    val ratio = contrast(text, background)
                    assertTrue("${palette.label}, dark=$dark: text contrast $ratio", ratio >= 4.5f)
                }
            }
        }
    }

    private fun contrast(a: Color, b: Color): Float {
        val first = a.luminance()
        val second = b.luminance()
        return (maxOf(first, second) + 0.05f) / (minOf(first, second) + 0.05f)
    }
}
