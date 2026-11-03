package com.example.nativegallery.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class AlbumTransitionSourceTest {
    private fun source(ready: Boolean = true) =
        AlbumTransitionSource(Any()).apply { available = ready }

    @Test fun anUndrawnCardCannotReplaceTheVisibleCover() {
        val sources = AlbumTransitionSources<Any>()
        val pending = source(ready = false)
        sources.record("camera", pending)
        assertNull(sources.get("camera"))
        pending.available = true
        assertSame(pending, sources.get("camera"))
    }

    @Test fun disposingThePreviousLayoutKeepsTheReplacementCard() {
        val sources = AlbumTransitionSources<Any>()
        val oldCard = source()
        val newCard = source()
        sources.record("camera", oldCard)
        sources.record("camera", newCard)
        sources.remove("camera", oldCard)
        assertFalse(oldCard.available)
        assertSame(newCard, sources.get("camera"))
    }

    @Test fun aDisposedCaptureIsInvalidatedForAnInFlightTransition() {
        val sources = AlbumTransitionSources<Any>()
        val captured = source()
        sources.record("camera", captured)
        sources.remove("camera", captured)
        assertNull(sources.get("camera"))
        assertFalse(captured.available)
    }
}
