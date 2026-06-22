package com.example.nativegallery.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = GalleryBlue,
    onPrimary = Color.White,
    background = IcyBackground,
    onBackground = Color.Black,
    surface = GallerySurface,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFE6E0D2),
    onSurfaceVariant = GalleryTextMuted,
    outline = GalleryLine
)

private val DarkColorScheme = darkColorScheme(
    primary = GalleryBlueDark,
    onPrimary = Color.Black,
    background = IcyBackgroundDark,
    onBackground = Color(0xFFF4F8FB),
    surface = GallerySurfaceDark,
    onSurface = Color(0xFFF7FAFC),
    surfaceVariant = Color(0xFF1F2B36),
    onSurfaceVariant = Color(0xFFAAB4BE),
    outline = Color(0xFF2D3A46)
)

@Composable
fun GalleryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = GalleryTypography,
        content = content
    )
}
