package pt.isec.amov.cityfix.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlin.math.abs

/**
 * Impact detector (brief I.2.F), based on the "PotholeDetector" of class slides 10/11. Only the
 * vertical axis counts (F-022, decision 0023): the accelerometer reading is projected onto the
 * gravity direction ([verticalAcceleration]), so a sideways jolt such as putting the phone down
 * on a table does not count. Gravity comes from TYPE_GRAVITY or, without it, from a low-pass
 * filter on the accelerometer ([lowPassGravity]).
 *
 * A drop (free fall) can only reach about -9.8 m/s², so a threshold above that reacts only to
 * upward jolts.
 *
 * An impact is |vertical| > [threshold] while the phone moves: [speed] must give more than
 * [MIN_SPEED_MPS] (null = no recent GPS fix = not moving). [start] calls onImpact at most once
 * per [COOLDOWN_MS], so one bump gives one suggestion. Without an accelerometer, [start] does
 * nothing.
 */
class ImpactDetector(
    context: Context,
    private val threshold: () -> Float,
    private val speed: () -> Float?,
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gravitySensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)

    /** Latest gravity vector, from TYPE_GRAVITY or the low-pass filter. */
    private val gravity = FloatArray(3)

    /** False until the low-pass filter got its first reading (review S4). */
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
                if (abs(vertical) > threshold() && now - lastImpact > COOLDOWN_MS) {
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
        /** Minimum GPS speed, in m/s (about 7 km/h), for a jolt to count (F-022 Q2). */
        const val MIN_SPEED_MPS = 2f
        /** At most one suggestion every 30 s. */
        const val COOLDOWN_MS = 30_000L
    }
}
