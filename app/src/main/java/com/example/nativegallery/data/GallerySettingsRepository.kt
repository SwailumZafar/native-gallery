package com.example.nativegallery.data

import android.content.Context

data class GallerySettings(
    val themeMode: GalleryThemeMode = GalleryThemeMode.System,
    val gridDensity: GalleryGridDensity = GalleryGridDensity.Compact,
    val autoplayVideos: Boolean = true,
    val startVideosMuted: Boolean = false,
    val performanceMode: Boolean = true
)

enum class GalleryThemeMode {
    System,
    Light,
    Dark
}

enum class GalleryGridDensity(val photoColumns: Int) {
    Compact(photoColumns = 4),
    Comfortable(photoColumns = 3),
    Spacious(photoColumns = 2)
}

class GallerySettingsRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "native_gallery_settings",
        Context.MODE_PRIVATE
    )

    fun initialSettings(): GallerySettings {
        return GallerySettings(
            themeMode = enumPreference(THEME_MODE_KEY, GalleryThemeMode.System),
            gridDensity = enumPreference(GRID_DENSITY_KEY, GalleryGridDensity.Compact),
            autoplayVideos = preferences.getBoolean(AUTOPLAY_VIDEOS_KEY, true),
            startVideosMuted = preferences.getBoolean(START_VIDEOS_MUTED_KEY, false),
            performanceMode = preferences.getBoolean(PERFORMANCE_MODE_KEY, true)
        )
    }

    fun save(settings: GallerySettings): GallerySettings {
        preferences.edit()
            .putString(THEME_MODE_KEY, settings.themeMode.name)
            .putString(GRID_DENSITY_KEY, settings.gridDensity.name)
            .putBoolean(AUTOPLAY_VIDEOS_KEY, settings.autoplayVideos)
            .putBoolean(START_VIDEOS_MUTED_KEY, settings.startVideosMuted)
            .putBoolean(PERFORMANCE_MODE_KEY, settings.performanceMode)
            .apply()
        return settings
    }

    private inline fun <reified T : Enum<T>> enumPreference(key: String, defaultValue: T): T {
        val savedName = preferences.getString(key, null) ?: return defaultValue
        return enumValues<T>().firstOrNull { it.name == savedName } ?: defaultValue
    }

    private companion object {
        const val THEME_MODE_KEY = "theme_mode"
        const val GRID_DENSITY_KEY = "grid_density"
        const val AUTOPLAY_VIDEOS_KEY = "autoplay_videos"
        const val START_VIDEOS_MUTED_KEY = "start_videos_muted"
        const val PERFORMANCE_MODE_KEY = "performance_mode"
    }
}
