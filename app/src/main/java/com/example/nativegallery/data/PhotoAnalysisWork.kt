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
import java.util.concurrent.Executor

/** Analysis never shares the UI/thumbnail worker pool, and yields at photo boundaries. */
internal object PhotoAnalysisWork {
    private val workers = Executors.newFixedThreadPool(3) { task ->
        Thread({ Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND); task.run() }, "photo-analysis")
    }
    val dispatcher = workers.asCoroutineDispatcher()
    private val inferenceWorkers = Executors.newFixedThreadPool(3) { task ->
        Thread({
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            task.run()
        }, "photo-inference")
    }
    val inferenceDispatcher = inferenceWorkers.asCoroutineDispatcher()
    // Image Labeler keys its native model queues by options, including executor identity. Distinct,
    // stable lanes allow actual parallel inference instead of three clients sharing one queue.
    // Inference runs at background priority to keep UI and RenderThread scrolling perfectly smooth.
    private val inferenceLanes = List(3) {
        object : Executor { override fun execute(command: Runnable) = inferenceWorkers.execute(command) }
    }
    fun inferenceExecutor(slot: Int): Executor = inferenceLanes[slot]
    private val peopleAllowed = MutableStateFlow(false)
    private val idle = MutableStateFlow(true)
    private val touching = MutableStateFlow(false)
    private val documentsScrolling = MutableStateFlow(false)
    fun setDocumentsScrolling(active: Boolean) {
        if (documentsScrolling.value == active) return
        quietSince = SystemClock.elapsedRealtime()
        documentsScrolling.value = active
    }
    @Volatile private var quietSince = SystemClock.elapsedRealtime()
    fun setTouching(active: Boolean) {
        if (touching.value == active) return
        quietSince = SystemClock.elapsedRealtime()
        touching.value = active
    }
    fun setPeopleAllowed(allowed: Boolean) { peopleAllowed.value = allowed }
    fun setInteractionBusy(busy: Boolean) {
        quietSince = SystemClock.elapsedRealtime()
        idle.value = !busy
    }
    private suspend fun awaitQuiet(needsPeople: Boolean) {
        while (true) {
            currentCoroutineContext().ensureActive()
            // Steady-state scans should not allocate four flow collectors for every face.
            if (idle.value && !touching.value && !documentsScrolling.value && (!needsPeople || peopleAllowed.value) &&
                SystemClock.elapsedRealtime() - quietSince >= 120L) return
            combine(peopleAllowed, idle, touching, documentsScrolling) { allowed, settled, pressed, scrolling ->
                (!needsPeople || allowed) && settled && !pressed && !scrolling
            }.first { it }
            delay((120L - (SystemClock.elapsedRealtime() - quietSince)).coerceAtLeast(0))
            if (idle.value && !touching.value && !documentsScrolling.value && (!needsPeople || peopleAllowed.value) &&
                SystemClock.elapsedRealtime() - quietSince >= 120L) return
        }
    }
    suspend fun awaitPeopleAllowed() = awaitQuiet(true)
    suspend fun awaitIdle() = awaitQuiet(false)
}
