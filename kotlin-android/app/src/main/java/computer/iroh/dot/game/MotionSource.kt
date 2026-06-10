package computer.iroh.dot.game

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
 * Dot input. On real devices the gravity sensor (TYPE_GRAVITY) drives the
 * position in both axes; on emulators with no gravity sensor, the touch-drag
 * fallback takes over. Once any gravity sample arrives, drag input is ignored.
 */
class MotionSource(context: Context) : SensorEventListener {

    var x: Float by mutableFloatStateOf(0f)
        private set
    var y: Float by mutableFloatStateOf(0f)
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

    fun setFromDrag(x: Float, y: Float) {
        if (tiltActive) return
        this.x = clamp(x)
        this.y = clamp(y)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_GRAVITY) return
        tiltActive = true
        // Gravity in m/s^2; GRAVITY_EARTH ~= 9.81. The dot rolls toward the
        // lowered edge, like a ball on a tray: tilting the device's right edge
        // down pushes +X, tilting the top edge down pushes the dot upward
        // (toward -Y on screen).
        x = clamp(event.values[0] / SensorManager.GRAVITY_EARTH)
        y = clamp(-event.values[1] / SensorManager.GRAVITY_EARTH)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
}

private fun clamp(v: Float, lo: Float = -1f, hi: Float = 1f): Float = max(lo, min(hi, v))
