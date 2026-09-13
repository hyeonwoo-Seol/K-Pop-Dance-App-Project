package com.example.kpopdancepracticeai.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class SkeletonOverlayTransformTest {
    private val width = 1920f
    private val height = 1080f

    @Test
    fun `portrait clockwise rotation maps raw video point upright`() {
        val point = orientVideoPoint(
            x = 100f,
            y = 200f,
            videoWidth = width,
            videoHeight = height,
            rotationDegrees = 90
        )

        assertEquals(880f, point.x, 0f)
        assertEquals(100f, point.y, 0f)
    }

    @Test
    fun `counterclockwise portrait rotation maps raw video point upright`() {
        val point = orientVideoPoint(
            x = 100f,
            y = 200f,
            videoWidth = width,
            videoHeight = height,
            rotationDegrees = 270
        )

        assertEquals(200f, point.x, 0f)
        assertEquals(1820f, point.y, 0f)
    }

    @Test
    fun `landscape video keeps original point`() {
        val point = orientVideoPoint(
            x = 100f,
            y = 200f,
            videoWidth = width,
            videoHeight = height,
            rotationDegrees = 0
        )

        assertEquals(100f, point.x, 0f)
        assertEquals(200f, point.y, 0f)
    }
}
