package pt.isec.amov.cityfix.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlin.math.abs

class ImpactDetector(
    context: Context,
    private val speed: () -> Float?,
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gravitySensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)

    private val gravity = FloatArray(3)

    private var gravityStarted = false

    private var onImpact: () -> Unit = {}
    private var lastImpact = 0L

    fun start(onImpact: () -> Unit) {
        this.onImpact = onImpact
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            gravitySensor?.let { g ->
                sensorManager.registerListener(this, g, SensorManager.SENSOR_DELAY_GAME)
            }
            Log.d("CityFix", "ImpactDetector start (gravity sensor: ${gravitySensor != null})")
        }
    }

    fun stop() {
        gravityStarted = false
        accelerometer?.let {
            sensorManager.unregisterListener(this)
            Log.d("CityFix", "ImpactDetector stop")
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        when (event?.sensor?.type) {
            Sensor.TYPE_GRAVITY -> event.values.copyInto(gravity, endIndex = 3)
            Sensor.TYPE_ACCELEROMETER -> {
                if (gravitySensor == null) {
                    // Review S4: start the filter at the first reading, not at zero, so the first
                    // events after start do not look like a jolt.
                    if (!gravityStarted) {
                        event.values.copyInto(gravity, endIndex = 3)
                        gravityStarted = true
                    }
                    lowPassGravity(gravity, event.values)
                }
                val vertical = verticalAcceleration(event.values, gravity)
                val now = System.currentTimeMillis()
                if (abs(vertical) > THRESHOLD && now - lastImpact > COOLDOWN_MS) {
                    val moving = (speed() ?: 0f) > MIN_SPEED_MPS
                    if (moving) {
                        lastImpact = now
                        onImpact()
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    companion object {
        const val THRESHOLD = 6f
        const val MIN_SPEED_MPS = 2f
        const val COOLDOWN_MS = 30_000L
    }
}
