package pt.isec.amov.cityfix.utils

import kotlin.math.sqrt

/*
 * Impact maths (brief I.2.F, F-022). Pure functions over the [x, y, z] arrays of the sensors
 * (class slides 10/11: TYPE_ACCELEROMETER includes gravity, TYPE_GRAVITY is gravity alone).
 */

fun verticalAcceleration(a: FloatArray, g: FloatArray): Float {
    val gNorm = sqrt(g[0] * g[0] + g[1] * g[1] + g[2] * g[2])
    if (gNorm == 0f) return 0f
    val dot = a[0] * g[0] + a[1] * g[1] + a[2] * g[2]
    return dot / gNorm - gNorm
}

fun lowPassGravity(g: FloatArray, a: FloatArray, alpha: Float = 0.8f) {
    for (i in 0..2) g[i] = alpha * g[i] + (1 - alpha) * a[i]
}
