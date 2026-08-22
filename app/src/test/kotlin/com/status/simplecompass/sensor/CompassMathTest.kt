package com.status.simplecompass.sensor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompassMathTest {
    @Test
    fun normalizeDegrees_wrapsValuesIntoCompassRange() {
        assertEquals(359f, CompassMath.normalizeDegrees(-1f), 0.001f)
        assertEquals(0f, CompassMath.normalizeDegrees(360f), 0.001f)
        assertEquals(5f, CompassMath.normalizeDegrees(725f), 0.001f)
    }

    @Test
    fun directionIndex_usesEightEqualSectors() {
        assertEquals(0, CompassMath.directionIndex(0f))
        assertEquals(0, CompassMath.directionIndex(337.5f))
        assertEquals(1, CompassMath.directionIndex(22.5f))
        assertEquals(2, CompassMath.directionIndex(90f))
        assertEquals(7, CompassMath.directionIndex(315f))
    }

    @Test
    fun headingFilter_crossesNorthWithoutJumpingSouth() {
        val filter = HeadingFilter(alpha = 0.5f)
        filter.apply(359f)
        val result = filter.apply(1f)

        assertTrue(result < 5f || result > 355f)
    }

    @Test
    fun trueHeading_appliesDeclinationAndWraps() {
        assertEquals(8f, CompassMath.trueHeading(358f, 10f), 0.001f)
        assertEquals(345f, CompassMath.trueHeading(5f, -20f), 0.001f)
    }

    @Test
    fun northAlignmentGate_triggersOnceUntilHeadingLeavesExitRange() {
        val gate = NorthAlignmentGate()

        assertTrue(gate.shouldTrigger(0.8f, isLevel = true))
        assertTrue(!gate.shouldTrigger(0.2f, isLevel = true))
        assertTrue(!gate.shouldTrigger(5f, isLevel = true))
        assertTrue(gate.shouldTrigger(359.5f, isLevel = true))
    }

    @Test
    fun northAlignmentGate_requiresLevelDevice() {
        val gate = NorthAlignmentGate()

        assertTrue(!gate.shouldTrigger(0f, isLevel = false))
        assertTrue(gate.shouldTrigger(0f, isLevel = true))
    }
}
