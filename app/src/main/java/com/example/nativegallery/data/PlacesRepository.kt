package com.example.nativegallery.data

import android.content.Context
import android.location.Geocoder
import android.os.Build
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.example.nativegallery.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.*

data class PhotoLocation(
    val latitude: Double,
    val longitude: Double,
    val placeName: String? = null
)

data class PlaceCluster(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val media: List<MediaItem>,
    val coordinateLabel: String
)

data class PlacesScanProgress(
    val clusters: List<PlaceCluster>,
    val scanned: Int,
    val total: Int,
    val geotaggedCount: Int
)

class PlacesRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("places_geo_cache_v2", Context.MODE_PRIVATE)

    suspend fun scanPlaces(
        mediaItems: List<MediaItem>,
        allowCacheNegatives: Boolean = true,
        onProgress: suspend (PlacesScanProgress) -> Unit
    ): List<PlaceCluster> = withContext(Dispatchers.IO) {
        val photos = mediaItems.filter { !it.isVideo && it.contentUri != null }
        val total = photos.size
        if (total == 0) {
            onProgress(PlacesScanProgress(emptyList(), 0, 0, 0))
            return@withContext emptyList()
        }

        val locatedPhotos = mutableListOf<Pair<MediaItem, PhotoLocation>>()
        val editor = prefs.edit()
        var dirtyCache = false

        for ((index, item) in photos.withIndex()) {
            if (!isActive) break
            val cacheKey = "${item.id}_${item.dateModifiedMillis}"
            val cached = prefs.getString(cacheKey, null)

            val loc: PhotoLocation? = when {
                cached == "NONE" && allowCacheNegatives -> null
                cached != null && cached != "NONE" -> {
                    val parts = cached.split("|")
                    if (parts.size >= 2) {
                        val lat = parts[0].toDoubleOrNull()
                        val lon = parts[1].toDoubleOrNull()
                        val place = parts.getOrNull(2)?.takeIf { it.isNotBlank() }
                        if (lat != null && lon != null) PhotoLocation(lat, lon, place) else null
                    } else null
                }
                else -> {
                    val extracted = extractGpsFromUri(item.contentUri)
                    if (extracted != null) {
                        editor.putString(cacheKey, "${extracted.latitude}|${extracted.longitude}|")
                        dirtyCache = true
                    } else if (allowCacheNegatives) {
                        editor.putString(cacheKey, "NONE")
                        dirtyCache = true
                    }
                    extracted
                }
            }

            if (loc != null) {
                locatedPhotos.add(item to loc)
            }

            if ((index + 1) % 35 == 0 || index == total - 1) {
                if (dirtyCache) {
                    editor.apply()
                    dirtyCache = false
                }
                val currentClusters = clusterLocations(locatedPhotos, resolveNames = (index == total - 1))
                onProgress(
                    PlacesScanProgress(
                        clusters = currentClusters,
                        scanned = index + 1,
                        total = total,
                        geotaggedCount = locatedPhotos.size
                    )
                )
            }
        }

        if (dirtyCache) editor.apply()
        val finalClusters = clusterLocations(locatedPhotos, resolveNames = true)
        onProgress(
            PlacesScanProgress(
                clusters = finalClusters,
                scanned = total,
                total = total,
                geotaggedCount = locatedPhotos.size
            )
        )
        finalClusters
    }

    private fun extractGpsFromUri(uri: android.net.Uri?): PhotoLocation? {
        if (uri == null) return null
        val candidateUris = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                runCatching { MediaStore.setRequireOriginal(uri) }.getOrNull()?.let(::add)
            }
            add(uri)
        }
        for (targetUri in candidateUris) {
            val coords = runCatching {
                context.contentResolver.openInputStream(targetUri)?.use { stream ->
                    ExifInterface(stream).latLong
                }
            }.getOrNull()
            if (coords != null && coords.size >= 2 && (coords[0] != 0.0 || coords[1] != 0.0)) {
                return PhotoLocation(coords[0], coords[1])
            }
        }
        return null
    }

    private fun clusterLocations(
        items: List<Pair<MediaItem, PhotoLocation>>,
        resolveNames: Boolean
    ): List<PlaceCluster> {
        if (items.isEmpty()) return emptyList()

        data class MutableCluster(
            var sumLat: Double,
            var sumLon: Double,
            var count: Int,
            val items: MutableList<Pair<MediaItem, PhotoLocation>>
        )

        val clusters = mutableListOf<MutableCluster>()
        val radiusKm = 18.0

        for (pair in items) {
            val loc = pair.second
            val match = clusters.firstOrNull { cluster ->
                val cLat = cluster.sumLat / cluster.count
                val cLon = cluster.sumLon / cluster.count
                haversineKm(cLat, cLon, loc.latitude, loc.longitude) <= radiusKm
            }
            if (match != null) {
                match.sumLat += loc.latitude
                match.sumLon += loc.longitude
                match.count++
                match.items.add(pair)
            } else {
                clusters.add(MutableCluster(loc.latitude, loc.longitude, 1, mutableListOf(pair)))
            }
        }

        return clusters.sortedByDescending { it.count }.mapIndexed { idx, cluster ->
            val centerLat = cluster.sumLat / cluster.count
            val centerLon = cluster.sumLon / cluster.count
            val coordLabel = formatCoordinate(centerLat, centerLon)

            val existingName = cluster.items.firstNotNullOfOrNull { it.second.placeName }
            val resolvedName = existingName ?: if (resolveNames) {
                resolvePlaceName(centerLat, centerLon)
            } else null

            val finalName = resolvedName ?: coordLabel
            if (resolvedName != null && existingName == null) {
                val geoCacheKey = "geo_${"%.2f_%.2f".format(Locale.US, centerLat, centerLon)}"
                prefs.edit().putString(geoCacheKey, resolvedName).apply()
            }

            PlaceCluster(
                id = "place:$idx:${"%.3f_%.3f".format(Locale.US, centerLat, centerLon)}",
                name = finalName,
                latitude = centerLat,
                longitude = centerLon,
                media = cluster.items.map { it.first }.sortedByDescending { it.sortTimestampMillis },
                coordinateLabel = coordLabel
            )
        }
    }

    @Suppress("DEPRECATION")
    private fun resolvePlaceName(lat: Double, lon: Double): String? {
        val geoCacheKey = "geo_${"%.2f_%.2f".format(Locale.US, lat, lon)}"
        prefs.getString(geoCacheKey, null)?.let { return it }

        if (!Geocoder.isPresent()) return null
        return runCatching {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            val addr = addresses?.firstOrNull() ?: return@runCatching null
            val city = addr.locality ?: addr.subAdminArea ?: addr.adminArea
            val country = addr.countryName
            val result = listOfNotNull(city, country).filter { it.isNotBlank() }.distinct().joinToString(", ")
            result.ifBlank { null }?.also {
                prefs.edit().putString(geoCacheKey, it).apply()
            }
        }.getOrNull()
    }
}

internal fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    val c = 2 * asin(sqrt(a))
    return r * c
}

internal fun formatCoordinate(lat: Double, lon: Double): String {
    val latDir = if (lat >= 0) "N" else "S"
    val lonDir = if (lon >= 0) "E" else "W"
    return "%.2f° %s, %.2f° %s".format(Locale.US, abs(lat), latDir, abs(lon), lonDir)
}
