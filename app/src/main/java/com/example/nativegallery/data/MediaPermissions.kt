package com.example.nativegallery.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

enum class MediaKind {
    Images,
    Videos
}

data class MediaAccessState(
    val mediaKinds: Set<MediaKind>,
    val isLimited: Boolean
) {
    val hasAccess: Boolean = mediaKinds.isNotEmpty()
}

object MediaPermissions {
    fun requestPermissions(): Array<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
            else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    fun currentAccess(context: Context): MediaAccessState {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return if (context.hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                MediaAccessState(setOf(MediaKind.Images, MediaKind.Videos), isLimited = false)
            } else {
                MediaAccessState(emptySet(), isLimited = false)
            }
        }

        val hasFullImages = context.hasPermission(Manifest.permission.READ_MEDIA_IMAGES)
        val hasFullVideos = context.hasPermission(Manifest.permission.READ_MEDIA_VIDEO)
        val hasSelectedAccess = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            context.hasPermission(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)

        val kinds = mutableSetOf<MediaKind>()
        if (hasFullImages || hasSelectedAccess) {
            kinds += MediaKind.Images
        }
        if (hasFullVideos || hasSelectedAccess) {
            kinds += MediaKind.Videos
        }

        return MediaAccessState(
            mediaKinds = kinds,
            isLimited = hasSelectedAccess && (!hasFullImages || !hasFullVideos)
        )
    }

    private fun Context.hasPermission(permission: String): Boolean {
        return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }
}