package com.example.nativegallery.data

/** Nested vault/public-store writes form one publication boundary. Main-thread confined. */
internal class GalleryMutationGate {
    var revision: Long = 0L
        private set
    private var writes = 0
    val isWriting: Boolean get() = writes > 0

    fun begin() { writes++; revision++ }

    /** True only when the outermost real write finishes. Unmatched callbacks are harmless. */
    fun finish(): Boolean {
        if (writes == 0) return false
        writes--
        revision++
        return writes == 0
    }

    fun canPublish(queryRevision: Long): Boolean = !isWriting && queryRevision == revision
}
