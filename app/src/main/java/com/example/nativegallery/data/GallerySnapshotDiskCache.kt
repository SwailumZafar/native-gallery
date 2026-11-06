package com.example.nativegallery.data

import android.net.Uri
import com.example.nativegallery.model.Album
import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.model.MediaType
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/** Complete catalogues only. Read/write on IO; selected-photo access never restores a full catalogue. */
internal data class SavedGalleryCatalogue(val snapshot: GallerySnapshot, val providerRevision: String?)

internal class GallerySnapshotDiskCache(private val directory: File, private val nowMillis: () -> Long = System::currentTimeMillis) {
    fun load(access: MediaAccessState): GallerySnapshot? = loadCatalogue(access)?.snapshot

    fun loadCatalogue(access: MediaAccessState): SavedGalleryCatalogue? {
        if (!access.hasAccess || access.isLimited) return null
        val file = file(access)
        return runCatching {
            DataInputStream(GZIPInputStream(file.inputStream().buffered()).buffered(64 * 1024)).use { input ->
                require(input.readInt() == 0x504C4731)
                val age = nowMillis() - input.readLong()
                // Present a complete saved page immediately after process eviction, including
                // overnight resumes. The provider revision is checked before skipping refresh.
                require(age in 0..2_592_000_000L)
                val media = List(input.count(500_000)) { input.media(0) }
                val byId = media.associateBy { it.id }
                val albums = List(input.count(50_000)) {
                    val id = input.readUTF(); val name = input.readUTF(); val count = input.readInt()
                    val covers = List(input.count(20)) { input.readUTF() }
                    val res = input.intOrNull(); val uri = input.text()?.let(Uri::parse)
                    val hidden = input.readBoolean(); val all = input.readBoolean(); val video = input.readBoolean()
                    val cover = input.text()?.let(byId::get); val smart = input.readBoolean()
                    Album(id, name, count, covers, res, uri, hidden, all, video, cover, smart)
                }
                // Older catalogue files end here. They are still usable while a fresh
                // background scan supplies the first provider revision.
                val revision = try { input.text() } catch (_: java.io.EOFException) { null }
                SavedGalleryCatalogue(GallerySnapshot(media, albums), revision)
            }
        }.getOrNull()
    }

    fun save(access: MediaAccessState, snapshot: GallerySnapshot, providerRevision: String? = null) {
        if (!access.hasAccess || access.isLimited) return
        runCatching {
            directory.mkdirs()
            val target = file(access)
            val temporary = File(directory, target.name + ".tmp")
            DataOutputStream(GZIPOutputStream(temporary.outputStream().buffered()).buffered(64 * 1024)).use { output ->
                output.writeInt(0x504C4731); output.writeLong(nowMillis())
                output.writeInt(snapshot.mediaItems.size)
                snapshot.mediaItems.forEach { output.media(it) }
                output.writeInt(snapshot.albums.size)
                snapshot.albums.forEach { album ->
                    output.writeUTF(album.id); output.writeUTF(album.name); output.writeInt(album.itemCount)
                    output.writeInt(album.coverMediaIds.size); album.coverMediaIds.forEach(output::writeUTF)
                    output.number(album.coverRes); output.text(album.coverUri?.toString())
                    output.writeBoolean(album.isHidden); output.writeBoolean(album.isAllPhotos)
                    output.writeBoolean(album.hasVideoBadge); output.text(album.coverMediaItem?.id)
                    output.writeBoolean(album.isSmart)
                }
                output.text(providerRevision)
            }
            check(temporary.renameTo(target))
        }
    }

    private fun file(access: MediaAccessState) = File(directory,
        "catalogue-${access.mediaKinds.map { it.name }.sorted().joinToString("-")}.gz")
    private fun DataOutputStream.text(value: String?) { writeBoolean(value != null); if (value != null) writeUTF(value) }
    private fun DataInputStream.text(): String? = if (readBoolean()) readUTF() else null
    private fun DataOutputStream.number(value: Int?) { writeBoolean(value != null); if (value != null) writeInt(value) }
    private fun DataInputStream.intOrNull(): Int? = if (readBoolean()) readInt() else null
    private fun DataOutputStream.number(value: Long?) { writeBoolean(value != null); if (value != null) writeLong(value) }
    private fun DataInputStream.longOrNull(): Long? = if (readBoolean()) readLong() else null
    private fun DataInputStream.count(max: Int): Int = readInt().also { require(it in 0..max) }

    private fun DataOutputStream.media(item: MediaItem) {
        writeUTF(item.id); writeUTF(item.albumId); writeInt(item.type.ordinal); writeUTF(item.title); writeUTF(item.dateLabel)
        number(item.imageRes); text(item.contentUri?.toString()); text(item.previewUri?.toString())
        writeBoolean(item.isVideo); text(item.durationLabel); number(item.durationMillis); text(item.mimeType)
        number(item.fileSizeBytes); writeLong(item.dateModifiedMillis); number(item.width); number(item.height)
        text(item.relativePath); writeLong(item.sortTimestampMillis); writeBoolean(item.isBurst); writeInt(item.burstCount)
        text(item.burstGroupId); writeInt(item.burstItems.size); item.burstItems.forEach { media(it) }
        writeBoolean(item.isMotionPhoto); text(item.motionVideoUri?.toString()); number(item.motionVideoOffset)
    }

    private fun DataInputStream.media(depth: Int): MediaItem {
        require(depth <= 4)
        val id = readUTF(); val album = readUTF(); val type = MediaType.entries[readInt()]
        val title = readUTF(); val date = readUTF(); val res = intOrNull()
        val uri = text()?.let(Uri::parse); val preview = text()?.let(Uri::parse); val video = readBoolean()
        val label = text(); val duration = longOrNull(); val mime = text(); val bytes = longOrNull()
        val modified = readLong(); val width = intOrNull(); val height = intOrNull(); val path = text()
        val timestamp = readLong(); val burst = readBoolean(); val burstCount = readInt(); val group = text()
        val children = List(count(1000)) { media(depth + 1) }
        val motion = readBoolean(); val motionUri = text()?.let(Uri::parse); val offset = longOrNull()
        return MediaItem(id, album, type, title, date, res, uri, preview, video, label, duration, mime,
            bytes, modified, width, height, path, timestamp, burst, burstCount, group, children, motion, motionUri, offset)
    }
}
