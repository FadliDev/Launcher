package com.sally.ember.game

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.PI
import kotlin.math.abs
import org.lwjgl.glfw.CallbackBridge as CB

/** Optional motion-aim based on Android's rotation-vector sensor. */
internal class GyroAimController(context: Context, private val onDelta: (Float, Float) -> Unit) : SensorEventListener {
    private val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor = manager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
        ?: manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private var enabled = false
    private var resumed = false
    private var warmup = 0
    private val matrix = FloatArray(9)
    private val orientation = FloatArray(3)
    private var lastYaw = Float.NaN
    private var lastPitch = Float.NaN
    var sensitivity = 1f
    var invertX = false
    var invertY = false

    fun setEnabled(value: Boolean) { enabled = value; refresh() }
    fun onResume() { resumed = true; refresh() }
    fun onPause() { resumed = false; refresh() }

    private fun refresh() {
        manager.unregisterListener(this)
        lastYaw = Float.NaN; lastPitch = Float.NaN; warmup = 2
        if (enabled && resumed && sensor != null) manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!enabled || !resumed || !CB.isGrabbing() || event.values.size < 3) return
        SensorManager.getRotationMatrixFromVector(matrix, event.values)
        SensorManager.getOrientation(matrix, orientation)
        val yaw = orientation[0]; val pitch = orientation[1]
        if (warmup > 0 || lastYaw.isNaN() || lastPitch.isNaN()) {
            lastYaw = yaw; lastPitch = pitch; warmup--; return
        }
        var dx = angleDelta(yaw, lastYaw); var dy = angleDelta(pitch, lastPitch)
        lastYaw = yaw; lastPitch = pitch
        if (abs(dx) + abs(dy) < 0.0015f) return
        if (invertX) dx = -dx
        if (invertY) dy = -dy
        onDelta(dx * sensitivity * 230f, dy * sensitivity * 230f)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun angleDelta(now: Float, before: Float): Float {
        var d = now - before
        val pi = PI.toFloat()
        while (d > pi) d -= (2f * pi)
        while (d < -pi) d += (2f * pi)
        return d
    }
}
