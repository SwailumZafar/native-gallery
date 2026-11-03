package com.example.nativegallery.data

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class FaceClusterIndexTest {
    private fun pose(degrees: Double): FloatArray {
        val angle = Math.toRadians(degrees)
        return floatArrayOf(cos(angle).toFloat(), sin(angle).toFloat())
    }

    @Test fun laterPosesMatchMoreThanTheOriginalFace() {
        val index = FaceClusterIndex()
        index.add("one", "first", pose(0.0))
        index.add("two", "second", pose(45.0))
        index.add("three", "third", pose(80.0))
        assertEquals(1, index.clusters.size)
        assertEquals(setOf("one", "two", "three"), index.clusters.single().photoIds)
        assertEquals("first", index.clusters.single().key)
    }

    @Test fun bridgingPoseCombinesPreviouslySplitGroups() {
        val index = FaceClusterIndex()
        index.add("one", "first", pose(0.0))
        index.add("two", "second", pose(80.0))
        assertEquals(2, index.clusters.size)
        index.add("three", "third", pose(40.0))
        assertEquals(1, index.clusters.size)
        assertEquals(3, index.clusters.single().photoIds.size)
    }

    @Test fun peopleAppearingTogetherRemainSeparateWhenAnotherFaceMatchesBoth() {
        val index = FaceClusterIndex()
        index.add("together", "first", pose(0.0))
        index.add("together", "second", pose(80.0))
        index.add("other", "third", pose(40.0))
        assertEquals(2, index.clusters.size)
        assertEquals(listOf(1, 2), index.clusters.map { it.photoIds.size }.sorted())
    }

    @Test fun unrelatedAndInvalidFacesDoNotJoinAGroup() {
        val index = FaceClusterIndex()
        index.add("one", "first", pose(0.0))
        index.add("two", "second", pose(180.0))
        index.add("zero", "zero", floatArrayOf(0f, 0f))
        index.add("invalid", "invalid", floatArrayOf(Float.NaN, 1f))
        assertEquals(2, index.clusters.size)
    }
}
