package com.aozijx.passly.app.shell

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.fragment.app.FragmentActivity

/** Owns the accelerometer lifecycle and emits only the configured face-down gesture. */
internal class FlipLockTriggerController(
    activity: FragmentActivity,
    private val onFlip: () -> Unit,
) : SensorEventListener {

    private val sensorManager =
        activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var started = false
    private var enabled = false

    fun start() {
        if (started) return
        started = true
        updateRegistration()
    }

    fun stop() {
        if (!started) return
        started = false
        sensorManager.unregisterListener(this)
    }

    fun setEnabled(enabled: Boolean) {
        if (this.enabled == enabled) return
        this.enabled = enabled
        updateRegistration()
    }

    private fun updateRegistration() {
        sensorManager.unregisterListener(this)
        if (!started || !enabled || accelerometer == null) return
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!enabled || event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return
        if (event.values[2] < FLIP_THRESHOLD) onFlip()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private companion object {
        const val FLIP_THRESHOLD = -8.5f
    }
}
