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
    private const val ReadMediaImages = "android.permission.READ_MEDIA_IMAGES"
    private const val ReadMediaVideo = "android.permission.READ_MEDIA_VIDEO"
    private const val ReadSelectedVisualMedia = "android.permission.READ_MEDIA_VISUAL_USER_SELECTED"

    fun requestPermissions(): Array<String> {
        return requestPermissionsForSdk(Build.VERSION.SDK_INT)
    }

    fun requestPermissionsForSdk(sdkInt: Int): Array<String> {
        return when {
            sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
                ReadMediaImages,
                ReadMediaVideo,
                ReadSelectedVisualMedia
            )
            sdkInt >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
                ReadMediaImages,
                ReadMediaVideo
            )
            else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    fun currentAccess(context: Context): MediaAccessState {
        val knownPermissions = setOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            ReadMediaImages,
            ReadMediaVideo,
            ReadSelectedVisualMedia
        )
        return accessForPermissions(
            sdkInt = Build.VERSION.SDK_INT,
            grantedPermissions = knownPermissions.filterTo(mutableSetOf()) { permission ->
                context.hasPermission(permission)
            }
        )
    }

    fun accessForPermissions(
        sdkInt: Int,
        grantedPermissions: Set<String>
    ): MediaAccessState {
        if (sdkInt < Build.VERSION_CODES.TIRAMISU) {
            return if (Manifest.permission.READ_EXTERNAL_STORAGE in grantedPermissions) {
                MediaAccessState(setOf(MediaKind.Images, MediaKind.Videos), isLimited = false)
            } else {
                MediaAccessState(emptySet(), isLimited = false)
            }
        }

        val hasFullImages = ReadMediaImages in grantedPermissions
        val hasFullVideos = ReadMediaVideo in grantedPermissions
        val hasSelectedAccess = sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            ReadSelectedVisualMedia in grantedPermissions

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
