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
    primaryContainer = Color(0xFFC2EFE9),
    onPrimaryContainer = Color(0xFF00201D),
    secondary = Color(0xFF3F5F5B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6E8E4),
    onSecondaryContainer = Color(0xFF173E3A),
    tertiary = Color(0xFF4B5F70),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD6E4F1),
    onTertiaryContainer = Color(0xFF1B3445),
    background = IcyBackground,
    onBackground = Color(0xFF181C1B),
    surface = GallerySurface,
    onSurface = Color(0xFF181C1B),
    surfaceDim = Color(0xFFDADDD9),
    surfaceBright = Color(0xFFFAFBF9),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = GalleryContainerLow,
    surfaceContainer = GalleryContainer,
    surfaceContainerHigh = GalleryContainerHigh,
    surfaceContainerHighest = GalleryContainerHighest,
    surfaceVariant = Color(0xFFECEFED),
    onSurfaceVariant = GalleryTextMuted,
    outline = GalleryLine,
    outlineVariant = GalleryLineStrong
)

private val DarkColorScheme = darkColorScheme(
    primary = GalleryBlueDark,
    onPrimary = GalleryOnPrimaryDark,
    primaryContainer = Color(0xFF005048),
    onPrimaryContainer = Color(0xFF9AF2E7),
    secondary = Color(0xFF9ECFC8),
    onSecondary = Color(0xFF0A3733),
    secondaryContainer = Color(0xFF274B47),
    onSecondaryContainer = Color(0xFFB9EAE3),
    tertiary = Color(0xFFB4C9DE),
    onTertiary = Color(0xFF203447),
    tertiaryContainer = Color(0xFF374B60),
    onTertiaryContainer = Color(0xFFD3E6FA),
    background = IcyBackgroundDark,
    onBackground = Color(0xFFF4F8FB),
    surface = GallerySurfaceDark,
    onSurface = Color(0xFFF7FAFC),
    surfaceDim = Color(0xFF0D151D),
    surfaceBright = Color(0xFF2B3742),
    surfaceContainerLowest = Color(0xFF0B131B),
    surfaceContainerLow = GalleryContainerLowDark,
    surfaceContainer = GalleryContainerDark,
    surfaceContainerHigh = GalleryContainerHighDark,
    surfaceContainerHighest = GalleryContainerHighestDark,
    surfaceVariant = Color(0xFF1F2B36),
    onSurfaceVariant = Color(0xFFAAB4BE),
    outline = Color(0xFF60707C),
    outlineVariant = GalleryLineDark
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
