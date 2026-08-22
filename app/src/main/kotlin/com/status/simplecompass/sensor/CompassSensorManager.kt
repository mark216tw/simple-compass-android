package com.status.simplecompass.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import android.view.Surface
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs
import kotlin.math.sqrt

data class CompassReading(
    val headingDegrees: Float = 0f,
    val orientationAccuracy: Int? = null,
    val magneticAccuracy: Int? = null,
    val magneticFieldMicroTesla: Float? = null,
    val pitchDegrees: Float = 0f,
    val rollDegrees: Float = 0f,
    val isLevel: Boolean = true,
    val sensorAvailable: Boolean = false,
    val hasReading: Boolean = false,
    val status: CompassStatus = CompassStatus.LOADING,
)

class CompassSensorManager(
    context: Context,
    private val displayRotation: () -> Int,
) : SensorEventListener, DefaultLifecycleObserver {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val canUseFallback = accelerometer != null && magnetometer != null
    private val headingFilter = HeadingFilter()
    private val pitchFilter = ScalarFilter(alpha = 0.12f)
    private val rollFilter = ScalarFilter(alpha = 0.12f)
    private val magneticFieldFilter = ScalarFilter(alpha = 0.08f)
    private val statusStabilizer = SensorStatusStabilizer()
    private val rotationMatrix = FloatArray(9)
    private val adjustedMatrix = FloatArray(9)
    private val orientation = FloatArray(3)
    private var gravity: FloatArray? = null
    private var geomagnetic: FloatArray? = null

    private val _reading = MutableStateFlow(
        CompassReading(
            sensorAvailable = rotationVector != null || canUseFallback,
            status = if (rotationVector != null || canUseFallback) {
                CompassStatus.LOADING
            } else {
                CompassStatus.UNAVAILABLE
            },
        ),
    )
    val reading: StateFlow<CompassReading> = _reading

    override fun onStart(owner: LifecycleOwner) {
        headingFilter.reset()
        pitchFilter.reset()
        rollFilter.reset()
        magneticFieldFilter.reset()
        val available = rotationVector != null || canUseFallback
        _reading.value = _reading.value.copy(
            hasReading = false,
            sensorAvailable = available,
            orientationAccuracy = null,
            magneticAccuracy = null,
            magneticFieldMicroTesla = null,
            status = statusStabilizer.reset(available),
        )
        rotationVector?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        } ?: run {
            accelerometer?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            }
        }
        magnetometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        sensorManager.unregisterListener(this)
        gravity = null
        geomagnetic = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                publishOrientation(rotationMatrix, event.accuracy)
            }

            Sensor.TYPE_ACCELEROMETER -> {
                gravity = event.values.copyOf()
                updateFallbackOrientation()
            }

            Sensor.TYPE_MAGNETIC_FIELD -> {
                geomagnetic = event.values.copyOf()
                val magnitude = sqrt(
                    event.values[0] * event.values[0] +
                        event.values[1] * event.values[1] +
                        event.values[2] * event.values[2],
                )
                updateReading(
                    _reading.value.copy(
                        magneticAccuracy = event.accuracy,
                        magneticFieldMicroTesla = magneticFieldFilter.apply(magnitude),
                    ),
                )
                if (rotationVector == null) updateFallbackOrientation()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        when (sensor.type) {
            Sensor.TYPE_MAGNETIC_FIELD -> updateReading(
                _reading.value.copy(magneticAccuracy = accuracy),
            )
            Sensor.TYPE_ROTATION_VECTOR -> updateReading(
                _reading.value.copy(orientationAccuracy = accuracy),
            )
        }
    }

    private fun updateFallbackOrientation() {
        val gravityValues = gravity ?: return
        val magneticValues = geomagnetic ?: return
        if (SensorManager.getRotationMatrix(rotationMatrix, null, gravityValues, magneticValues)) {
            publishOrientation(rotationMatrix, _reading.value.magneticAccuracy)
        }
    }

    private fun publishOrientation(matrix: FloatArray, accuracy: Int?) {
        val (axisX, axisY) = when (displayRotation()) {
            Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
            Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
            Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
            else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
        }
        if (!SensorManager.remapCoordinateSystem(matrix, axisX, axisY, adjustedMatrix)) return

        SensorManager.getOrientation(adjustedMatrix, orientation)
        val heading = headingFilter.apply(Math.toDegrees(orientation[0].toDouble()).toFloat())
        val pitch = pitchFilter.apply(Math.toDegrees(orientation[1].toDouble()).toFloat())
        val roll = rollFilter.apply(Math.toDegrees(orientation[2].toDouble()).toFloat())
        updateReading(
            _reading.value.copy(
                headingDegrees = heading,
                orientationAccuracy = accuracy,
                pitchDegrees = pitch,
                rollDegrees = roll,
                isLevel = abs(pitch) <= 5f && abs(roll) <= 5f,
                hasReading = true,
            ),
        )
    }

    private fun updateReading(reading: CompassReading) {
        val statusAccuracy = reading.magneticAccuracy ?: reading.orientationAccuracy
        _reading.value = reading.copy(
            status = statusStabilizer.update(
                sensorAvailable = reading.sensorAvailable,
                hasReading = reading.hasReading,
                magneticFieldMicroTesla = reading.magneticFieldMicroTesla,
                accuracy = statusAccuracy,
                nowNanos = SystemClock.elapsedRealtimeNanos(),
            ),
        )
    }
}
