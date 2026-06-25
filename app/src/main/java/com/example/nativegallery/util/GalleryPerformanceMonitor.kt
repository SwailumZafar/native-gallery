package com.example.nativegallery.util

import android.os.SystemClock
import android.util.Log

object GalleryPerformanceMonitor {
    fun <T> trace(label: String, block: () -> T): T {
        val startMillis = SystemClock.elapsedRealtime()
        return try {
            block()
        } finally {
            Log.d(Tag, "$label took ${SystemClock.elapsedRealtime() - startMillis}ms")
        }
    }

    suspend fun <T> traceSuspend(label: String, block: suspend () -> T): T {
        val startMillis = SystemClock.elapsedRealtime()
        return try {
            block()
        } finally {
            Log.d(Tag, "$label took ${SystemClock.elapsedRealtime() - startMillis}ms")
        }
    }

    private const val Tag = "NativeGalleryPerf"
}
