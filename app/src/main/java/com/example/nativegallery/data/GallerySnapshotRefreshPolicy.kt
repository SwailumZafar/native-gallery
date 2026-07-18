package com.example.nativegallery.data

internal object GallerySnapshotRefreshPolicy {
    const val ResumeFullRefreshMaxAgeMillis = 5L * 60L * 1000L
    const val AppWriteObserverGraceMillis = 2_000L

    fun shouldSuppressObserverFullRefresh(
        appWriteInFlight: Boolean,
        suppressionUntilMillis: Long,
        nowMillis: Long
    ): Boolean {
        return appWriteInFlight || suppressionUntilMillis >= nowMillis
    }

    fun latestPageHasChanges(
        baseSnapshot: GallerySnapshot?,
        latestPage: GallerySnapshot,
        pageLimit: Int
    ): Boolean {
        val base = baseSnapshot ?: return latestPage.mediaItems.isNotEmpty()
        val latestItems = latestPage.mediaItems
        if (latestItems.isEmpty()) return base.mediaItems.isNotEmpty()

        val expectedPageSize = minOf(base.mediaItems.size, pageLimit.coerceAtLeast(0))
        if (latestItems.size != expectedPageSize) return true

        return base.mediaItems.take(expectedPageSize) != latestItems
    }

    fun shouldRunResumeFullRefresh(
        latestPageChanged: Boolean,
        lastFullRefreshMillis: Long,
        nowMillis: Long,
        maxAgeMillis: Long = ResumeFullRefreshMaxAgeMillis
    ): Boolean {
        if (latestPageChanged) return true
        if (lastFullRefreshMillis < 0L) return true
        return nowMillis - lastFullRefreshMillis >= maxAgeMillis.coerceAtLeast(0L)
    }
}