package com.example.nativegallery.data

import android.os.Process
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
    fun setPeopleAllowed(allowed: Boolean) { peopleAllowed.value = allowed }
    fun setInteractionBusy(busy: Boolean) { idle.value = !busy }
    suspend fun awaitPeopleAllowed() { combine(peopleAllowed, idle) { allowed, settled -> allowed && settled }.first { it } }
    suspend fun awaitIdle() { idle.first { it } }
}
