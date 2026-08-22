package com.status.simplecompass.sensor

import org.junit.Assert.assertEquals
import org.junit.Test

class SensorStatusStabilizerTest {
    private val warningDelay = 1_500_000_000L
    private val recoveryDelay = 3_000_000_000L

    @Test
    fun warningMustRemainStableBeforeItIsShown() {
        val stabilizer = SensorStatusStabilizer(warningDelay, recoveryDelay)

        assertEquals(CompassStatus.LOADING, stabilizer.update(true, true, 50f, 1, 0L))
        assertEquals(
            CompassStatus.CALIBRATION,
            stabilizer.update(true, true, 50f, 1, warningDelay),
        )
    }

    @Test
    fun readyStateUsesLongerRecoveryDelay() {
        val stabilizer = SensorStatusStabilizer(warningDelay, recoveryDelay)
        stabilizer.update(true, true, 50f, 1, 0L)
        stabilizer.update(true, true, 50f, 1, warningDelay)

        assertEquals(
            CompassStatus.CALIBRATION,
            stabilizer.update(true, true, 50f, 3, warningDelay + 1L),
        )
        assertEquals(
            CompassStatus.READY,
            stabilizer.update(true, true, 50f, 3, warningDelay + 1L + recoveryDelay),
        )
    }

    @Test
    fun interferenceUsesHysteresisBeforeRecovery() {
        val stabilizer = SensorStatusStabilizer(warningDelay, recoveryDelay)
        stabilizer.update(true, true, 50f, 3, 0L)
        stabilizer.update(true, true, 50f, 3, warningDelay)
        stabilizer.update(true, true, 75f, 3, warningDelay + 1L)
        assertEquals(
            CompassStatus.INTERFERENCE,
            stabilizer.update(true, true, 75f, 3, warningDelay * 2 + 1L),
        )

        assertEquals(
            CompassStatus.INTERFERENCE,
            stabilizer.update(true, true, 68f, 3, warningDelay * 2 + 2L),
        )
        stabilizer.update(true, true, 60f, 3, warningDelay * 2 + 3L)
        assertEquals(
            CompassStatus.READY,
            stabilizer.update(true, true, 60f, 3, warningDelay * 2 + 3L + recoveryDelay),
        )
    }
}
