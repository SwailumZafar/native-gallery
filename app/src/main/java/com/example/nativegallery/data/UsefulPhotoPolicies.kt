package com.example.nativegallery.data

import com.example.nativegallery.model.MediaItem
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.ResolverStyle
import java.util.Locale

internal fun isScreenshotMedia(item: MediaItem): Boolean {
    if (item.isVideo) return false
    val source = (item.relativePath.orEmpty() + "/" + item.title).lowercase(Locale.ROOT)
    return listOf("screenshot", "screen_shot", "screen-shot", "screen shot", "screen capture").any(source::contains)
}

enum class ScreenshotCategory(val label: String) {
    Tickets("Tickets"), Receipts("Receipts"), Orders("Orders"), Events("Events"), Other("Other")
}

internal fun screenshotCategory(text: String): ScreenshotCategory {
    val words = text.lowercase(Locale.ROOT)
    fun has(vararg values: String) = values.any { Regex("\\b${Regex.escape(it)}\\b").containsMatchIn(words) }
    return when {
        has("boarding", "seat", "gate", "booking reference", "e-ticket", "admit", "ticket") -> ScreenshotCategory.Tickets
        has("receipt", "invoice", "subtotal", "tax invoice", "amount paid") -> ScreenshotCategory.Receipts
        has("order", "delivery", "tracking", "shipment", "dispatched") -> ScreenshotCategory.Orders
        has("concert", "appointment", "reservation", "meeting", "event", "rsvp") -> ScreenshotCategory.Events
        else -> ScreenshotCategory.Other
    }
}

/** Only explicit dates with a year are suggested; the user reviews before adding a follow-up. */
internal fun screenshotDate(text: String): LocalDate? {
    val formats = listOf("uuuu-M-d", "d/M/uuuu", "d-M-uuuu", "d MMM uuuu", "MMM d uuuu", "d MMMM uuuu", "MMMM d uuuu")
    val candidates = Regex("\\b(?:\\d{4}-\\d{1,2}-\\d{1,2}|\\d{1,2}[/\\-]\\d{1,2}[/\\-]\\d{4}|\\d{1,2} [A-Za-z]{3,9} \\d{4}|[A-Za-z]{3,9} \\d{1,2},? \\d{4})\\b")
        .findAll(text).map { it.value.replace(",", "") }
    for (candidate in candidates) for (format in formats) {
        val date = runCatching { LocalDate.parse(candidate, DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(format).toFormatter(Locale.ENGLISH)
            .withResolverStyle(ResolverStyle.STRICT)) }.getOrNull()
        if (date != null && date.year in 2000..2100) return date
    }
    return null
}

data class MomentSuggestion(val id: String, val mediaIds: List<String>, val startMillis: Long, val endMillis: Long, val title: String = "Memories")

/** A date is only a boundary. Suggestions require analyzed photographic content and distinct frames. */
internal fun suggestMoments(media: List<MediaItem>, zone: ZoneId = ZoneId.systemDefault(),
    evidence: Map<String, MemoryEvidence> = emptyMap()): List<MomentSuggestion> {
    val photos = media.asSequence().filter { !it.isVideo && !isScreenshotMedia(it) && it.sortTimestampMillis > 0 }
        .filter { isMemoryCandidate(it, favorite = true) }
        .filter { evidence[it.id]?.let { result -> result.theme != null && result.quality > 0f } == true }
        .distinctBy { it.id }.sortedBy { it.sortTimestampMillis }.toList()
    val groups = mutableListOf<MutableList<MediaItem>>()
    for (photo in photos) {
        val group = groups.lastOrNull()
        val date = Instant.ofEpochMilli(photo.sortTimestampMillis).atZone(zone).toLocalDate()
        val firstDay = group?.firstOrNull()?.let { Instant.ofEpochMilli(it.sortTimestampMillis).atZone(zone).toLocalDate() }
        if (group == null || photo.sortTimestampMillis - group.last().sortTimestampMillis > 20 * 3_600_000L ||
            (firstDay != null && java.time.temporal.ChronoUnit.DAYS.between(firstDay, date) >= 4)) {
            groups += mutableListOf(photo)
        } else group += photo
    }
    return groups.mapNotNull { group ->
        val themes = group.groupBy { evidence.getValue(it.id).theme }
        // People and scenery from one outing belong together; unrelated subjects
        // (for example, meals and pets from an ordinary day) need their own group.
        val outing = group.filter { evidence.getValue(it.id).theme in setOf(MemoryTheme.Together, MemoryTheme.Outdoors, MemoryTheme.Celebration) }
        val coherent = if (outing.size >= 4) outing else themes.values.maxByOrNull { it.size }.orEmpty()
        val distinct = mutableListOf<MediaItem>()
        for (photo in coherent.sortedByDescending { evidence.getValue(it.id).quality }) {
            if (distinct.any { existing -> existing.burstGroupId != null && existing.burstGroupId == photo.burstGroupId ||
                kotlin.math.abs(existing.sortTimestampMillis - photo.sortTimestampMillis) < 15 * 60_000L &&
                    nearDuplicateMemory(evidence.getValue(existing.id).fingerprint, evidence.getValue(photo.id).fingerprint) }) continue
            distinct += photo
        }
        if (distinct.size < 4) return@mapNotNull null
        val members = distinct.sortedBy { it.sortTimestampMillis }
        val kinds = members.map { evidence.getValue(it.id).theme }.toSet()
        val title = when {
            MemoryTheme.Celebration in kinds -> MemoryTheme.Celebration.title
            MemoryTheme.Outdoors in kinds && MemoryTheme.Together in kinds -> "A day out"
            else -> kinds.first()?.title ?: "Memories"
        }
        MomentSuggestion("suggested:content:${members.first().id}", members.map { it.id }, members.first().sortTimestampMillis,
            members.last().sortTimestampMillis, title)
    }.asReversed()
}

/** Prefer favorites and larger originals, with variety rather than many shots of the same second. */
internal fun momentHighlights(media: List<MediaItem>, favorites: Set<String>, limit: Int = 12): List<MediaItem> {
    if (limit <= 0) return emptyList()
    val ranked = media.distinctBy { it.id }.sortedWith(compareByDescending<MediaItem> { it.id in favorites }
        .thenByDescending { (it.width ?: 0).toLong() * (it.height ?: 0) }.thenBy { it.sortTimestampMillis })
    val chosen = mutableListOf<MediaItem>()
    val usedBursts = mutableSetOf<String>()
    for (photo in ranked) {
        if (photo.burstGroupId != null && photo.burstGroupId in usedBursts) continue
        if (chosen.any { kotlin.math.abs(it.sortTimestampMillis - photo.sortTimestampMillis) < 10_000L && it.albumId == photo.albumId }) continue
        chosen += photo
        photo.burstGroupId?.let(usedBursts::add)
        if (chosen.size == limit) break
    }
    return chosen.sortedBy { it.sortTimestampMillis }
}

enum class PrivacyMaskKind { Face, SensitiveText, Text, Manual }

data class PrivacyRegion(val id: String, val left: Float, val top: Float, val right: Float, val bottom: Float,
    val kind: PrivacyMaskKind = PrivacyMaskKind.Manual) {
    fun sanitized(): PrivacyRegion? {
        if (listOf(left, top, right, bottom).any { !it.isFinite() }) return null
        val l = minOf(left, right).coerceIn(0f, 1f)
        val r = maxOf(left, right).coerceIn(0f, 1f)
        val t = minOf(top, bottom).coerceIn(0f, 1f)
        val b = maxOf(top, bottom).coerceIn(0f, 1f)
        return if (r - l < 0.002f || b - t < 0.002f) null else copy(left = l, top = t, right = r, bottom = b)
    }
    fun contains(x: Float, y: Float) = x in left..right && y in top..bottom
}

internal fun isSensitiveShareText(text: String): Boolean =
    Regex("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", RegexOption.IGNORE_CASE).containsMatchIn(text) ||
        Regex("(?:\\+?\\d[\\d ()-]{5,}\\d)").containsMatchIn(text) ||
        Regex("\\b(?:address|passport|password|pin|account|cnic|iban|otp)\\b", RegexOption.IGNORE_CASE).containsMatchIn(text)
