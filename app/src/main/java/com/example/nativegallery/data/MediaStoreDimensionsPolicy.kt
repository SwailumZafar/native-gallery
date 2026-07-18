package com.example.nativegallery.data

internal object MediaStoreDimensionsPolicy {
    fun orientedDimensions(width: Int?, height: Int?, orientationDegrees: Int): Pair<Int?, Int?> {
        val safeWidth = width?.takeIf { it > 0 }
        val safeHeight = height?.takeIf { it > 0 }
        val normalizedOrientation = ((orientationDegrees % 360) + 360) % 360
        return if (
            safeWidth != null &&
            safeHeight != null &&
            (normalizedOrientation == 90 || normalizedOrientation == 270)
        ) {
            safeHeight to safeWidth
        } else {
            safeWidth to safeHeight
        }
    }
}
