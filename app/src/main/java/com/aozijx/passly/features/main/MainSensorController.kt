package com.aozijx.passly.features.main

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.fragment.app.FragmentActivity

/**
 * 翻转锁屏传感器控制器。
 *
 * 封装加速度计监听逻辑，仅在翻转角度超过阈值且已授权时触发 [onFlipLock] 回调。
 * Activity 持有此控制器并负责在 onResume/onPause/setContent 中调用 [register]/[unregister]。
 */
internal class MainSensorController(
    private val activity: FragmentActivity,
    private val onFlipLock: () -> Unit
) : SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null

    var isFlipLockEnabled = false
    var isFlipExitAndClearStackEnabled = false

    fun initialize() {
        sensorManager = activity.getSystemService(FragmentActivity.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    fun register() {
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun unregister() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!isFlipLockEnabled || event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return
        // Z 轴负值表示屏幕朝下，-8.5f 为触发阈值
        if (event.values[2] < -8.5f) {
            onFlipLock()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
