package com.example.nativegallery.data

import org.junit.Assert.*
import org.junit.Test

class PeopleMatchingTest {
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
