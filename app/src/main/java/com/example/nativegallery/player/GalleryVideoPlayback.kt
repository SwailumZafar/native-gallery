package com.example.nativegallery.player

import android.content.Context
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.example.nativegallery.data.AviHeaderFixingDataSource
import java.util.Locale

/** Viewer playback and difficult thumbnail decoding must use the same compatibility path. */
@OptIn(UnstableApi::class)
internal fun galleryVideoMediaSourceFactory(context: Context): DefaultMediaSourceFactory {
    val upstream = DefaultDataSource.Factory(context)
    return DefaultMediaSourceFactory(context, localVideoExtractorsFactory())
        .setDataSourceFactory(DataSource.Factory {
            AviHeaderFixingDataSource(upstream.createDataSource())
        })
}

@OptIn(UnstableApi::class)
internal fun galleryVideoRenderersFactory(context: Context): DefaultRenderersFactory {
    val hardware = Build.HARDWARE.orEmpty()
    return DefaultRenderersFactory(context)
        .setEnableDecoderFallback(true)
        .setMediaCodecSelector { mimeType, secure, tunneling ->
            val decoders = MediaCodecSelector.DEFAULT.getDecoderInfos(mimeType, secure, tunneling)
            if (preferMediaTekHardwareAvc(hardware, mimeType)) {
                decoders.filter { it.hardwareAccelerated }.ifEmpty { decoders }
            } else {
                decoders
            }
        }
        .apply {
            // Verified on Redmi Note 9 / Lineage: Codec2 dies while vendor OMX works reliably
            // through the synchronous adapter, despite the ROM's incorrect AVC capabilities.
            if (isMediaTekHardware(hardware)) forceDisableMediaCodecAsynchronousQueueing()
        }
}

internal fun isMediaTekHardware(hardware: String): Boolean {
    val normalized = hardware.trim().lowercase(Locale.US)
    return normalized.startsWith("mt") || normalized.contains("mediatek")
}

internal fun preferMediaTekHardwareAvc(hardware: String, mimeType: String): Boolean =
    isMediaTekHardware(hardware) && mimeType.equals(MimeTypes.VIDEO_H264, ignoreCase = true)
