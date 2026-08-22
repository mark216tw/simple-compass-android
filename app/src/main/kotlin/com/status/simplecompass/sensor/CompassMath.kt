package com.status.simplecompass.sensor

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object CompassMath {
    fun normalizeDegrees(degrees: Float): Float = ((degrees % 360f) + 360f) % 360f

    fun directionIndex(degrees: Float): Int =
        ((normalizeDegrees(degrees) + 22.5f) / 45f).toInt() % 8

    fun trueHeading(magneticHeading: Float, declination: Float): Float =
        normalizeDegrees(magneticHeading + declination)

    fun distanceToNorth(degrees: Float): Float {
        val normalized = normalizeDegrees(degrees)
        return minOf(normalized, 360f - normalized)
    }
}

class HeadingFilter(private val alpha: Float = 0.18f) {
    private var filteredSin = 0f
    private var filteredCos = 0f
    private var initialized = false

    fun apply(headingDegrees: Float): Float {
        val radians = Math.toRadians(headingDegrees.toDouble())
        val newSin = sin(radians).toFloat()
        val newCos = cos(radians).toFloat()

        if (!initialized) {
            filteredSin = newSin
            filteredCos = newCos
            initialized = true
        } else {
            filteredSin += alpha * (newSin - filteredSin)
            filteredCos += alpha * (newCos - filteredCos)
        }

        return CompassMath.normalizeDegrees(
            Math.toDegrees(atan2(filteredSin, filteredCos).toDouble()).toFloat(),
        )
    }

    fun reset() {
        initialized = false
    }
}

class ScalarFilter(private val alpha: Float) {
    private var value = 0f
    private var initialized = false

    fun apply(newValue: Float): Float {
        value = if (initialized) value + alpha * (newValue - value) else newValue
        initialized = true
        return value
    }

    fun reset() {
        initialized = false
    }
}

class NorthAlignmentGate(
    private val enterDegrees: Float = 1f,
    private val exitDegrees: Float = 4f,
) {
    private var armed = true

    fun shouldTrigger(headingDegrees: Float, isLevel: Boolean): Boolean {
        val distance = CompassMath.distanceToNorth(headingDegrees)
        if (distance >= exitDegrees) armed = true
        if (!isLevel || !armed || distance > enterDegrees) return false

        armed = false
        return true
    }

    fun reset() {
        armed = true
    }
}
