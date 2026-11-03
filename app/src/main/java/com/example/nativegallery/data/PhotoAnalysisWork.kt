package com.example.nativegallery.data

import android.os.Process
import android.os.SystemClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import java.util.concurrent.Executors

/** Analysis never shares the UI/thumbnail worker pool, and yields at photo boundaries. */
internal object PhotoAnalysisWork {
    val dispatcher = Executors.newFixedThreadPool(2) { task ->
        Thread({ Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND); task.run() }, "photo-analysis")
    }.asCoroutineDispatcher()
    private val peopleAllowed = MutableStateFlow(false)
    private val idle = MutableStateFlow(true)
    private val touching = MutableStateFlow(false)
    private val documentsScrolling = MutableStateFlow(false)
    fun setDocumentsScrolling(active: Boolean) {
        quietSince = SystemClock.elapsedRealtime()
        documentsScrolling.value = active
    }
    @Volatile private var quietSince = SystemClock.elapsedRealtime()
    fun setTouching(active: Boolean) {
        quietSince = SystemClock.elapsedRealtime()
        touching.value = active
    }
    fun setPeopleAllowed(allowed: Boolean) { peopleAllowed.value = allowed }
    fun setInteractionBusy(busy: Boolean) {
        if (idle.value == busy) quietSince = SystemClock.elapsedRealtime()
        idle.value = !busy
    }
    private suspend fun awaitQuiet(needsPeople: Boolean) {
        while (true) {
            currentCoroutineContext().ensureActive()
            // Steady-state scans should not allocate four flow collectors for every face.
            if (idle.value && !touching.value && !documentsScrolling.value && (!needsPeople || peopleAllowed.value) &&
                SystemClock.elapsedRealtime() - quietSince >= 350L) return
            combine(peopleAllowed, idle, touching, documentsScrolling) { allowed, settled, pressed, scrolling ->
                (!needsPeople || allowed) && settled && !pressed && !scrolling
            }.first { it }
            delay((350L - (SystemClock.elapsedRealtime() - quietSince)).coerceAtLeast(0))
            if (idle.value && !touching.value && !documentsScrolling.value && (!needsPeople || peopleAllowed.value) &&
                SystemClock.elapsedRealtime() - quietSince >= 350L) return
        }
    }
    suspend fun awaitPeopleAllowed() = awaitQuiet(true)
    suspend fun awaitIdle() = awaitQuiet(false)
}
