package com.example.nativegallery.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

enum class MediaKind {
    Images,
    Videos
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

    fun hasMediaAccess(context: Context): Boolean {
        return allowedMediaKinds(context).isNotEmpty()
    }

    fun allowedMediaKinds(context: Context): Set<MediaKind> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return if (context.hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                setOf(MediaKind.Images, MediaKind.Videos)
            } else {
                emptySet()
            }
        }

        val kinds = mutableSetOf<MediaKind>()
        if (context.hasPermission(Manifest.permission.READ_MEDIA_IMAGES)) {
            kinds += MediaKind.Images
        }
        if (context.hasPermission(Manifest.permission.READ_MEDIA_VIDEO)) {
            kinds += MediaKind.Videos
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            context.hasPermission(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        ) {
            kinds += MediaKind.Images
            kinds += MediaKind.Videos
        }
        return kinds
    }

    private fun Context.hasPermission(permission: String): Boolean {
        return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }
}
