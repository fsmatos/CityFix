package pt.isec.amov.cityfix.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

class ImpactDetector(context: Context) : SensorEventListener {

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
                // D-022: gravity is added back, so MIN and MAX are the limits of the class slides,
                // applied to the vertical axis only. There is no minimum speed.
                val withGravity = SensorManager.GRAVITY_EARTH + vertical
                val now = System.currentTimeMillis()
                if ((withGravity > MAX || withGravity < MIN) && now - lastImpact > COOLDOWN_MS) {
                    lastImpact = now
                    onImpact()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    companion object {
        const val MAX = 14f
        const val MIN = 6f
        const val COOLDOWN_MS = 30_000L
    }
}
