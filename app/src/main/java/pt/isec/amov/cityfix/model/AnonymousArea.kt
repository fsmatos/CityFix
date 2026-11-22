package pt.isec.amov.cityfix.model

import pt.isec.amov.cityfix.utils.haversineMeters

val PRACA_REPUBLICA = Coordinates(40.2100, -8.4190)

const val ANONYMOUS_RADIUS_METERS = 10_000.0

fun isInsideAnonymousArea(location: Coordinates): Boolean {
    if (location == Coordinates()) return false
    return haversineMeters(
        location.latitude, location.longitude,
        PRACA_REPUBLICA.latitude, PRACA_REPUBLICA.longitude,
    ) <= ANONYMOUS_RADIUS_METERS
}
