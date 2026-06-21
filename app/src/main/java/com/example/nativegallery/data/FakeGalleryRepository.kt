package com.example.nativegallery.data

import com.example.nativegallery.R
import com.example.nativegallery.model.Album
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.model.MediaType

object FakeGalleryRepository {
    val mediaItems = listOf(
        MediaItem("m1", "camera", MediaType.Photo, "Mountain lake", "Today", R.drawable.album_camera),
        MediaItem("m2", "camera", MediaType.Video, "Beach dog", "Today", R.drawable.thumb_dog_beach, isVideo = true, durationLabel = "00:12"),
        MediaItem("m3", "download", MediaType.Photo, "Coffee", "Today", R.drawable.thumb_coffee),
        MediaItem("m4", "download", MediaType.Photo, "Quiet street", "Today", R.drawable.thumb_street),
        MediaItem("m5", "screenshots", MediaType.Photo, "Chair", "Today", R.drawable.album_screenshots),
        MediaItem("m6", "whatsapp", MediaType.Photo, "Leaf", "Today", R.drawable.album_whatsapp),
        MediaItem("m7", "camera", MediaType.Photo, "Coast", "Today", R.drawable.thumb_coast),
        MediaItem("m8", "wallpapers", MediaType.Photo, "Sunrise", "Today", R.drawable.album_videos),
        MediaItem("m9", "videos", MediaType.Video, "Camp fire", "Today", R.drawable.thumb_fire, isVideo = true, durationLabel = "00:15"),
        MediaItem("m10", "download", MediaType.Photo, "Fruit bowl", "Today", R.drawable.thumb_fruit),
        MediaItem("m11", "camera", MediaType.Photo, "Lake dock", "Today", R.drawable.album_download),
        MediaItem("m12", "wallpapers", MediaType.Photo, "City night", "Today", R.drawable.thumb_city),
        MediaItem("m13", "documents", MediaType.Photo, "Fridge label", "14 June 2026", R.drawable.thumb_fridge),
        MediaItem("m14", "documents", MediaType.Photo, "Repair one", "11 June 2026", R.drawable.thumb_repair_1),
        MediaItem("m15", "documents", MediaType.Photo, "Repair two", "11 June 2026", R.drawable.thumb_repair_2),
        MediaItem("m16", "documents", MediaType.Photo, "Repair three", "11 June 2026", R.drawable.thumb_repair_3),
        MediaItem("m17", "documents", MediaType.Photo, "Repair four", "11 June 2026", R.drawable.thumb_repair_4),
        MediaItem("m18", "documents", MediaType.Photo, "Outdoor unit", "11 June 2026", R.drawable.thumb_repair_5)
    )

    val albums = listOf(
        Album("all", "All photos", 4_812, mediaItems.map { it.id }, R.drawable.album_all_photos, isAllPhotos = true),
        Album("camera", "Camera", 1_258, listOf("m1", "m7", "m11"), R.drawable.album_camera),
        Album("videos", "Videos", 342, listOf("m2", "m9"), R.drawable.album_videos),
        Album("screenshots", "Screenshots", 892, listOf("m5"), R.drawable.album_screenshots),
        Album("download", "Download", 156, listOf("m3", "m4", "m10"), R.drawable.album_download),
        Album("whatsapp", "WhatsApp Images", 1_120, listOf("m6"), R.drawable.album_whatsapp),
        Album("documents", "Documents", 64, listOf("m13", "m14"), R.drawable.thumb_document),
        Album("wallpapers", "Wallpapers", 96, listOf("m8", "m12"), R.drawable.thumb_wallpaper),
        Album("favorites", "Favorites", 248, listOf("m10", "m11"), R.drawable.thumb_fruit),
        Album("others", "Others", 1_044, listOf("m4", "m5"), R.drawable.album_others)
    )

    val hideableAlbums = albums.filterNot { it.isAllPhotos || it.id == "favorites" || it.id == "others" }

    fun visibleAlbums(hiddenAlbumIds: Set<String>): List<Album> {
        return albums.filter { it.isAllPhotos || !hiddenAlbumIds.contains(it.id) }
    }

    fun visibleMedia(hiddenAlbumIds: Set<String>): List<MediaItem> {
        return mediaItems.filterNot { hiddenAlbumIds.contains(it.albumId) }
    }
}
