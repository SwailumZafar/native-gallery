package com.example.nativegallery.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class FastScrollTargetTest {
    @Test fun draggingToEitherEndTargetsTheActualListEdge() {
        assertEquals(FastScrollTarget(0, 0), fastScrollTarget(0f, 200, 9, 110))
        assertEquals(FastScrollTarget(199, 0), fastScrollTarget(1f, 200, 9, 110))
        assertEquals(FastScrollTarget(95, 55), fastScrollTarget(0.5f, 200, 9, 110))
    }

    @Test fun shortListsAndInvalidPointerFractionsStayBounded() {
        assertEquals(FastScrollTarget(0, 0), fastScrollTarget(0.8f, 3, 9, 110))
        assertEquals(FastScrollTarget(0, 0), fastScrollTarget(Float.NaN, 200, 9, 110))
        assertEquals(FastScrollTarget(0, 0), fastScrollTarget(-2f, 200, 9, 110))
        assertEquals(FastScrollTarget(199, 0), fastScrollTarget(2f, 200, 9, 110))
    }
}
