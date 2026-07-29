package com.example.nativegallery.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import androidx.annotation.RequiresApi

/**
 * The special media-management access introduced in Android 12.
 *
 * This is deliberately separate from normal photo/video read permission. When granted, Android
 * permits MediaStore write/delete requests without showing a confirmation dialog for every batch.
 */
enum class MediaManagementAccessState {
    UnsupportedAndroidVersion,
    MissingManifestPermission,
    Granted,
    Requestable,
    RequestActivityUnavailable
}

enum class MediaStoreConfirmationStrategy {
    OneTap,
    AndroidSystemConfirmation
}

data class MediaManagementAccessStatus(
    val state: MediaManagementAccessState
) {
    val isGranted: Boolean
        get() = state == MediaManagementAccessState.Granted

    val canRequest: Boolean
        get() = state == MediaManagementAccessState.Requestable

    val confirmationStrategy: MediaStoreConfirmationStrategy
        get() = if (isGranted) {
            MediaStoreConfirmationStrategy.OneTap
        } else {
            MediaStoreConfirmationStrategy.AndroidSystemConfirmation
        }
}

/**
 * Pure policy surface so decisions can be covered by local JVM tests without loading API 31
 * framework members on older devices.
 */
internal object MediaManagementAccessPolicy {
    const val MinimumSdk = 31

    fun evaluate(
        sdkInt: Int,
        permissionDeclared: Boolean,
        canManageMedia: Boolean,
        requestActivityAvailable: Boolean
    ): MediaManagementAccessStatus {
        val state = when {
            sdkInt < MinimumSdk -> MediaManagementAccessState.UnsupportedAndroidVersion
            !permissionDeclared -> MediaManagementAccessState.MissingManifestPermission
            canManageMedia -> MediaManagementAccessState.Granted
            requestActivityAvailable -> MediaManagementAccessState.Requestable
            else -> MediaManagementAccessState.RequestActivityUnavailable
        }
        return MediaManagementAccessStatus(state)
    }
}

/**
 * Safe Android adapter around [MediaManagementAccessPolicy].
 *
 * Callers should always retain their existing MediaStore confirmation flow as the fallback when
 * [requestAccessIntent] returns null or when [status] is not granted.
 */
class MediaManagementAccessRepository(context: Context) {
    private val appContext = context.applicationContext

    @SuppressLint("NewApi") // Explicit VERSION_CODES.S guard below.
    fun status(): MediaManagementAccessStatus {
        val sdkInt = Build.VERSION.SDK_INT
        if (sdkInt < Build.VERSION_CODES.S) {
            return MediaManagementAccessPolicy.evaluate(
                sdkInt = sdkInt,
                permissionDeclared = false,
                canManageMedia = false,
                requestActivityAvailable = false
            )
        }

        val permissionDeclared = runCatching {
            Api31.isManageMediaPermissionDeclared(appContext)
        }.getOrDefault(false)
        if (!permissionDeclared) {
            return MediaManagementAccessPolicy.evaluate(
                sdkInt = sdkInt,
                permissionDeclared = false,
                canManageMedia = false,
                requestActivityAvailable = false
            )
        }

        val granted = runCatching {
            Api31.canManageMedia(appContext)
        }.getOrDefault(false)
        val requestActivityAvailable = !granted && runCatching {
            Api31.requestIntent(appContext.packageName)
                .resolveActivity(appContext.packageManager) != null
        }.getOrDefault(false)

        return MediaManagementAccessPolicy.evaluate(
            sdkInt = sdkInt,
            permissionDeclared = true,
            canManageMedia = granted,
            requestActivityAvailable = requestActivityAvailable
        )
    }

    /**
     * Returns the system-owned special-access screen only when it is both needed and resolvable.
     * A null result means the caller should continue with normal MediaStore confirmations.
     */
    @SuppressLint("NewApi") // Explicit VERSION_CODES.S guard below.
    fun requestAccessIntent(): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        if (!status().canRequest) return null
        return Api31.requestIntent(appContext.packageName)
    }

    @RequiresApi(31)
    private object Api31 {
        fun canManageMedia(context: Context): Boolean = MediaStore.canManageMedia(context)

        @Suppress("DEPRECATION")
        fun isManageMediaPermissionDeclared(context: Context): Boolean {
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_PERMISSIONS
            )
            return packageInfo.requestedPermissions
                ?.contains(Manifest.permission.MANAGE_MEDIA) == true
        }

        fun requestIntent(packageName: String): Intent {
            return Intent(Settings.ACTION_REQUEST_MANAGE_MEDIA).apply {
                data = Uri.fromParts("package", packageName, null)
            }
        }
    }
}
