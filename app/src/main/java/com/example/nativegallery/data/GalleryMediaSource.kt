package com.example.nativegallery.data

import com.example.nativegallery.model.RecentlyDeletedMedia

/** Read boundary shared by MediaStore and deterministic mutation/refresh regression fixtures. */
interface GalleryMediaSource {
    fun cachedGallery(mediaKinds: Set<MediaKind>): GallerySnapshot?
    fun hasCompleteCachedGallery(mediaKinds: Set<MediaKind>): Boolean = false
    fun loadSavedGallery(access: MediaAccessState): GallerySnapshot? = null
    fun saveGallery(access: MediaAccessState, snapshot: GallerySnapshot) = Unit
    /** Called on IO. A provider generation match avoids touching the whole library on resume. */
    fun isCachedGalleryCurrent(mediaKinds: Set<MediaKind>): Boolean = false
    fun loadGallery(mediaKinds: Set<MediaKind>): GallerySnapshot
    fun loadGalleryPage(mediaKinds: Set<MediaKind>, limit: Int, offset: Int = 0): GallerySnapshot
    fun mergeLatestPage(mediaKinds: Set<MediaKind>, baseSnapshot: GallerySnapshot?, latestPage: GallerySnapshot): GallerySnapshot
    fun loadTrashedMedia(mediaKinds: Set<MediaKind>): List<RecentlyDeletedMedia>
}
