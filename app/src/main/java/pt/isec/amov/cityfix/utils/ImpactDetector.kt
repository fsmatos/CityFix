package pt.isec.amov.cityfix.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlin.math.sqrt

/**
 * Impact detector (brief I.2.F), based on the "PotholeDetector" of class slides 10/11 and its
 * limits: the total acceleration sqrt(x² + y² + z²) outside [MIN]..[MAX] m/s² counts as an impact.
 * At rest it is gravity (about 9.8) in any phone position, so, unlike the slide's Z axis alone,
 * an upright phone does not trigger it; a vertical jolt does. [start] calls onImpact at most once
 * per [COOLDOWN_MS], so one bump gives one suggestion. Without an accelerometer, [start] does nothing.
 */
class ImpactDetector(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var onImpact: () -> Unit = {}
    private var lastImpact = 0L

    fun start(onImpact: () -> Unit) {
        this.onImpact = onImpact
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            Log.d("CityFix", "ImpactDetector start")
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        Log.d("CityFix", "ImpactDetector stop")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val total = sqrt(x * x + y * y + z * z)
            val now = System.currentTimeMillis()
            if ((total > MAX || total < MIN) && now - lastImpact > COOLDOWN_MS) {
                lastImpact = now
                onImpact()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    companion object {
        /** Slide values, in m/s² (gravity alone is about 9.8). */
        const val MAX = 14.0f
        const val MIN = 6.0f
        /** At most one suggestion every 30 s. */
        const val COOLDOWN_MS = 30_000L
    }
}
