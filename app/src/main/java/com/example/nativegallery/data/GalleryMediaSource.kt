package com.example.nativegallery.data

import com.example.nativegallery.model.RecentlyDeletedMedia

/** Read boundary shared by MediaStore and deterministic mutation/refresh regression fixtures. */
interface GalleryMediaSource {
    fun cachedGallery(mediaKinds: Set<MediaKind>): GallerySnapshot?
    fun loadGallery(mediaKinds: Set<MediaKind>): GallerySnapshot
    fun loadGalleryPage(mediaKinds: Set<MediaKind>, limit: Int, offset: Int = 0): GallerySnapshot
    fun mergeLatestPage(mediaKinds: Set<MediaKind>, baseSnapshot: GallerySnapshot?, latestPage: GallerySnapshot): GallerySnapshot
    fun loadTrashedMedia(mediaKinds: Set<MediaKind>): List<RecentlyDeletedMedia>
}
