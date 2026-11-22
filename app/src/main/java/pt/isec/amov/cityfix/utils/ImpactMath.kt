package pt.isec.amov.cityfix.utils

import kotlin.math.sqrt

/*
 * Impact maths (brief I.2.F, F-022). Pure functions over the [x, y, z] arrays of the sensors
 * (class slides 10/11: TYPE_ACCELEROMETER includes gravity, TYPE_GRAVITY is gravity alone).
 */

/**
 * Vertical part of the acceleration [a] without gravity, in m/s²: the projection of [a] onto the
 * gravity direction [g], (a·g)/|g|, minus |g|. About 0 at rest in any phone position; positive
 * when pushed up, negative when dropping. 0 when [g] is still zero (no gravity reading yet).
 */
fun verticalAcceleration(a: FloatArray, g: FloatArray): Float {
    val gNorm = sqrt(g[0] * g[0] + g[1] * g[1] + g[2] * g[2])
    if (gNorm == 0f) return 0f
    val dot = a[0] * g[0] + a[1] * g[1] + a[2] * g[2]
    return dot / gNorm - gNorm
}

/**
 * Low-pass filter that estimates gravity from the accelerometer, for phones without TYPE_GRAVITY:
 * g = alpha·g + (1 - alpha)·a, updating [g] in place (Android SensorEvent documentation).
 */
fun lowPassGravity(g: FloatArray, a: FloatArray, alpha: Float = 0.8f) {
    for (i in 0..2) g[i] = alpha * g[i] + (1 - alpha) * a[i]
}
