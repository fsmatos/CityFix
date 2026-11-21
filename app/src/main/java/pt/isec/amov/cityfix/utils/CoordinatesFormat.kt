package pt.isec.amov.cityfix.utils

import java.util.Locale

/** D-019: 5 decimals (about 1 m) with a dot in every language, so "lat, lng" stays unambiguous. */
fun formatCoordinate(value: Double): String = String.format(Locale.ROOT, "%.5f", value)
