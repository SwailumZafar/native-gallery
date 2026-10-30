package com.example.nativegallery.data

import org.junit.Assert.*
import org.junit.Test

class PeopleMatchingTest {
    @Test fun normalizedFastMatchingPreservesCosineScores() {
        val a = floatArrayOf(0.5f, -2f, 4f, 0.3f)
        val b = floatArrayOf(-1f, 3f, 0.2f, 5f)
        assertEquals(faceSimilarity(a, b), normalizedFaceSimilarity(normalizeFaceVector(a), normalizeFaceVector(b)), 0.00001f)
        assertEquals(0f, normalizedFaceSimilarity(normalizeFaceVector(floatArrayOf(0f, 0f)), normalizeFaceVector(floatArrayOf(2f, 3f))), 0.00001f)
    }
    @Test fun similarityIsIndependentOfEmbeddingMagnitude() {
        assertEquals(1f, faceSimilarity(floatArrayOf(1f, 2f), floatArrayOf(3f, 6f)), 0.0001f)
    }

    @Test fun unrelatedAndOppositeEmbeddingsDoNotMatch() {
        assertEquals(0f, faceSimilarity(floatArrayOf(1f, 0f), floatArrayOf(0f, 1f)), 0.0001f)
        assertTrue(faceSimilarity(floatArrayOf(1f, 0f), floatArrayOf(-1f, 0f)) < 0.5f)
    }

    @Test fun invalidEmbeddingsCannotMatch() {
        assertTrue(faceSimilarity(floatArrayOf(0f, 0f), floatArrayOf(1f, 2f)) < 0.5f)
        assertTrue(faceSimilarity(floatArrayOf(1f), floatArrayOf(1f, 2f)) < 0.5f)
        assertTrue(faceSimilarity(floatArrayOf(), floatArrayOf()) < 0.5f)
    }
}
