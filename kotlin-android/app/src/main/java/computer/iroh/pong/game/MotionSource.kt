package computer.iroh.pong.game

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import kotlin.math.max
import kotlin.math.min

/**
 * Paddle input. On real devices, gravity sensor (TYPE_GRAVITY) drives
 * paddleX; on emulators with no gravity sensor, the touch-drag fallback
 * takes over. Once any gravity sample arrives, drag input is ignored.
 */
class MotionSource(context: Context) : SensorEventListener {

    var paddleX: Float by mutableFloatStateOf(0f)
        private set

    private val sensorManager =
        context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gravity: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)

    private var tiltActive = false

    fun start() {
        val sensor = gravity ?: return
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    fun setFromDrag(x: Float) {
        if (tiltActive) return
        paddleX = clamp(x, -1f, 1f)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_GRAVITY) return
        tiltActive = true
        // Gravity x in m/s^2; SensorManager.GRAVITY_EARTH ~= 9.81. Tilting the
        // top of a portrait device to the right pushes gravity toward +X
        // in the device frame, so this maps naturally to "paddle goes right".
        paddleX = clamp(event.values[0] / SensorManager.GRAVITY_EARTH, -1f, 1f)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
}

private fun clamp(v: Float, lo: Float, hi: Float): Float = max(lo, min(hi, v))
