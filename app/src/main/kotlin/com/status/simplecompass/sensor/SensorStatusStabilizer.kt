package com.status.simplecompass.sensor

import android.hardware.SensorManager

enum class CompassStatus {
    UNAVAILABLE,
    LOADING,
    READY,
    CALIBRATION,
    INTERFERENCE,
}

class SensorStatusStabilizer(
    private val warningDelayNanos: Long = 1_500_000_000L,
    private val recoveryDelayNanos: Long = 3_000_000_000L,
) {
    private var current = CompassStatus.LOADING
    private var pending: CompassStatus? = null
    private var pendingSinceNanos = 0L

    fun update(
        sensorAvailable: Boolean,
        hasReading: Boolean,
        magneticFieldMicroTesla: Float?,
        accuracy: Int?,
        nowNanos: Long,
    ): CompassStatus {
        if (!sensorAvailable) return setImmediately(CompassStatus.UNAVAILABLE)
        if (!hasReading) return setImmediately(CompassStatus.LOADING)

        val rawStatus = rawStatus(magneticFieldMicroTesla, accuracy)
        if (rawStatus == current) {
            pending = null
            return current
        }

        if (pending != rawStatus) {
            pending = rawStatus
            pendingSinceNanos = nowNanos
            return current
        }

        val delay = when {
            current == CompassStatus.LOADING -> warningDelayNanos
            rawStatus == CompassStatus.READY -> recoveryDelayNanos
            else -> warningDelayNanos
        }
        if (nowNanos - pendingSinceNanos >= delay) {
            current = rawStatus
            pending = null
        }
        return current
    }

    fun reset(sensorAvailable: Boolean): CompassStatus =
        setImmediately(if (sensorAvailable) CompassStatus.LOADING else CompassStatus.UNAVAILABLE)

    private fun rawStatus(field: Float?, accuracy: Int?): CompassStatus {
        val interferencePending = current == CompassStatus.INTERFERENCE ||
            pending == CompassStatus.INTERFERENCE
        val normalRange = if (interferencePending) 25f..65f else 20f..70f
        return when {
            field != null && field !in normalRange -> CompassStatus.INTERFERENCE
            accuracy != null && accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW ->
                CompassStatus.CALIBRATION
            else -> CompassStatus.READY
        }
    }

    private fun setImmediately(status: CompassStatus): CompassStatus {
        current = status
        pending = null
        return current
    }
}
