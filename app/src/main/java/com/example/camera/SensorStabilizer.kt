package com.example.camera

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MotionOffset(
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val rotationDeg: Float = 0f,
    val isStabilizing: Boolean = true
)

class SensorStabilizer(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val gyroSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val accelSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _motionOffset = MutableStateFlow(MotionOffset())
    val motionOffset: StateFlow<MotionOffset> = _motionOffset.asStateFlow()

    private var filterAlpha = 0.85f // Exponential smoothing filter
    private var smoothedPitch = 0f
    private var smoothedRoll = 0f
    private var smoothedYaw = 0f

    var isEISEnabled: Boolean = true

    fun start() {
        gyroSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        accelSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || !isEISEnabled) return

        if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
            val gyroX = event.values[0]
            val gyroY = event.values[1]
            val gyroZ = event.values[2]

            // Apply low-pass smoothing
            smoothedPitch = filterAlpha * smoothedPitch + (1 - filterAlpha) * gyroX
            smoothedRoll = filterAlpha * smoothedRoll + (1 - filterAlpha) * gyroY
            smoothedYaw = filterAlpha * smoothedYaw + (1 - filterAlpha) * gyroZ

            // Counter-act shake by applying inverse pixel transform (normalized)
            val counteractX = -smoothedRoll * 12.0f
            val counteractY = smoothedPitch * 12.0f
            val counteractRot = -smoothedYaw * 2.5f

            _motionOffset.value = MotionOffset(
                offsetX = counteractX.coerceIn(-25f, 25f),
                offsetY = counteractY.coerceIn(-25f, 25f),
                rotationDeg = counteractRot.coerceIn(-5f, 5f),
                isStabilizing = isEISEnabled
            )
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
