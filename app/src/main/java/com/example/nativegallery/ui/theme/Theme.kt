package com.example.nativegallery.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.nativegallery.data.GalleryPalette

private val IrisLight = lightColorScheme(
    primary = Color(0xFF6846C7), onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF), onPrimaryContainer = Color(0xFF281052),
    secondary = Color(0xFF665580), onSecondary = Color.White,
    secondaryContainer = Color(0xFFECDFF8), onSecondaryContainer = Color(0xFF291A3E),
    tertiary = Color(0xFF9B405B), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD9E3), onTertiaryContainer = Color(0xFF3F0820),
    background = Color(0xFFF8F5FF), onBackground = Color(0xFF211B2C),
    surface = Color(0xFFFFFBFF), onSurface = Color(0xFF211B2C),
    surfaceDim = Color(0xFFDDD6E7), surfaceBright = Color(0xFFFFFBFF),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF4EFFB),
    surfaceContainer = Color(0xFFEEE7F8), surfaceContainerHigh = Color(0xFFE8E0F2),
    surfaceContainerHighest = Color(0xFFE1D8ED), surfaceVariant = Color(0xFFE9E0F2),
    onSurfaceVariant = Color(0xFF635A70), outline = Color(0xFF7D728B), outlineVariant = Color(0xFFD2C5E2)
)
private val IrisDark = darkColorScheme(
    primary = Color(0xFFD1BCFF), onPrimary = Color(0xFF381775),
    primaryContainer = Color(0xFF4D2E93), onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFD3BFE9), onSecondary = Color(0xFF382A4D),
    secondaryContainer = Color(0xFF504063), onSecondaryContainer = Color(0xFFECDFF8),
    tertiary = Color(0xFFFFB1C9), onTertiary = Color(0xFF5E1832),
    tertiaryContainer = Color(0xFF7B2C46), onTertiaryContainer = Color(0xFFFFD9E3),
    background = Color(0xFF131019), onBackground = Color(0xFFF0E8FA),
    surface = Color(0xFF191420), onSurface = Color(0xFFF0E8FA),
    surfaceDim = Color(0xFF131019), surfaceBright = Color(0xFF3E354A),
    surfaceContainerLowest = Color(0xFF0E0B13), surfaceContainerLow = Color(0xFF201A29),
    surfaceContainer = Color(0xFF261F31), surfaceContainerHigh = Color(0xFF30273C),
    surfaceContainerHighest = Color(0xFF3A3048), surfaceVariant = Color(0xFF3C3249),
    onSurfaceVariant = Color(0xFFCEC0DC), outline = Color(0xFFA395B2), outlineVariant = Color(0xFF50445E)
)
private val LagoonLight = IrisLight.copy(
    primary = Color(0xFF006C61), primaryContainer = Color(0xFF9EF3DF), onPrimaryContainer = Color(0xFF002E27),
    secondary = Color(0xFF426461), secondaryContainer = Color(0xFFC6EBE5), onSecondaryContainer = Color(0xFF153633),
    tertiary = Color(0xFF426278), tertiaryContainer = Color(0xFFCDE8FF), onTertiaryContainer = Color(0xFF102F43),
    background = Color(0xFFF1FAF7), onBackground = Color(0xFF152420), surface = Color(0xFFF9FFFC), onSurface = Color(0xFF152420),
    surfaceDim = Color(0xFFD2E0DA), surfaceBright = Color(0xFFF9FFFC),
    surfaceContainerLow = Color(0xFFEAF5EF), surfaceContainer = Color(0xFFE1EFE8),
    surfaceContainerHigh = Color(0xFFD8E9E1), surfaceContainerHighest = Color(0xFFCFE2D9),
    surfaceVariant = Color(0xFFDCEBE3), onSurfaceVariant = Color(0xFF4D645B),
    outline = Color(0xFF6E857A), outlineVariant = Color(0xFFB9D2C6)
)
private val LagoonDark = IrisDark.copy(
    primary = Color(0xFF74E2CB), onPrimary = Color(0xFF00382F), primaryContainer = Color(0xFF005145), onPrimaryContainer = Color(0xFF9EF3DF),
    secondary = Color(0xFFA5D1C8), onSecondary = Color(0xFF163B34), secondaryContainer = Color(0xFF2E5249), onSecondaryContainer = Color(0xFFC6EBE5),
    tertiary = Color(0xFFA5CEE9), onTertiary = Color(0xFF12374F), tertiaryContainer = Color(0xFF2A4E65), onTertiaryContainer = Color(0xFFCDE8FF),
    background = Color(0xFF0C1614), onBackground = Color(0xFFE1F1E9), surface = Color(0xFF111E1A), onSurface = Color(0xFFE1F1E9),
    surfaceDim = Color(0xFF0C1614), surfaceBright = Color(0xFF31463E), surfaceContainerLowest = Color(0xFF07110E),
    surfaceContainerLow = Color(0xFF172720), surfaceContainer = Color(0xFF1C2F27), surfaceContainerHigh = Color(0xFF253A31),
    surfaceContainerHighest = Color(0xFF30473D), surfaceVariant = Color(0xFF334A40),
    onSurfaceVariant = Color(0xFFB7CFC1), outline = Color(0xFF8EA899), outlineVariant = Color(0xFF41594D)
)
private val SunsetLight = IrisLight.copy(
    primary = Color(0xFFAD4435), primaryContainer = Color(0xFFFFDAD2), onPrimaryContainer = Color(0xFF450C05),
    secondary = Color(0xFF8A5541), secondaryContainer = Color(0xFFFFDBC9), onSecondaryContainer = Color(0xFF382011),
    tertiary = Color(0xFF765B00), tertiaryContainer = Color(0xFFFFE394), onTertiaryContainer = Color(0xFF332600),
    background = Color(0xFFFFF5EF), onBackground = Color(0xFF30201C), surface = Color(0xFFFFFBF8), onSurface = Color(0xFF30201C),
    surfaceDim = Color(0xFFE9D5CB), surfaceBright = Color(0xFFFFFBF8),
    surfaceContainerLow = Color(0xFFFFEDE3), surfaceContainer = Color(0xFFF9E4D8),
    surfaceContainerHigh = Color(0xFFF2DACD), surfaceContainerHighest = Color(0xFFECD1C2),
    surfaceVariant = Color(0xFFF3DDD1), onSurfaceVariant = Color(0xFF755A4E), outline = Color(0xFF96796B), outlineVariant = Color(0xFFDFC0AE)
)
private val SunsetDark = IrisDark.copy(
    primary = Color(0xFFFFB4A4), onPrimary = Color(0xFF641E13), primaryContainer = Color(0xFF8B3023), onPrimaryContainer = Color(0xFFFFDAD2),
    secondary = Color(0xFFEDB89A), onSecondary = Color(0xFF4B2A19), secondaryContainer = Color(0xFF68402C), onSecondaryContainer = Color(0xFFFFDBC9),
    tertiary = Color(0xFFECC65E), onTertiary = Color(0xFF3F3000), tertiaryContainer = Color(0xFF594400), onTertiaryContainer = Color(0xFFFFE394),
    background = Color(0xFF1C120F), onBackground = Color(0xFFFBE7DD), surface = Color(0xFF241814), onSurface = Color(0xFFFBE7DD),
    surfaceDim = Color(0xFF1C120F), surfaceBright = Color(0xFF4C3830), surfaceContainerLowest = Color(0xFF150D0A),
    surfaceContainerLow = Color(0xFF2D1F19), surfaceContainer = Color(0xFF35251E), surfaceContainerHigh = Color(0xFF402E25),
    surfaceContainerHighest = Color(0xFF4E392E), surfaceVariant = Color(0xFF513B30),
    onSurfaceVariant = Color(0xFFDFC4B5), outline = Color(0xFFB99B88), outlineVariant = Color(0xFF614A3D)
)

internal fun galleryColorScheme(palette: GalleryPalette, dark: Boolean): ColorScheme = when (palette) {
    GalleryPalette.Iris -> if (dark) IrisDark else IrisLight
    GalleryPalette.Lagoon -> if (dark) LagoonDark else LagoonLight
    GalleryPalette.Sunset -> if (dark) SunsetDark else SunsetLight
}

@Composable
fun GalleryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    palette: GalleryPalette = GalleryPalette.Iris,
    content: @Composable () -> Unit
) {
    MaterialTheme(colorScheme = galleryColorScheme(palette, darkTheme), typography = GalleryTypography, content = content)
}
