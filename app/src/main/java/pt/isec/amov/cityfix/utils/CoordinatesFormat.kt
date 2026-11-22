package pt.isec.amov.cityfix.utils

import java.util.Locale

fun formatCoordinate(value: Double): String = String.format(Locale.ROOT, "%.5f", value)
