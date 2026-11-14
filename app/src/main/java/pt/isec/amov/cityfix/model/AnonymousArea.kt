package pt.isec.amov.cityfix.model

import pt.isec.amov.cityfix.utils.haversineMeters

/**
 * Centre of the anonymous area: Praça da República, Coimbra (brief I.1). The coordinates are
 * approximate; a code constant because there is no municipality document yet (F-012 Q1).
 */
val PRACA_REPUBLICA = Coordinates(40.2100, -8.4190)

/** Anonymous citizens can use the app only within this distance of [PRACA_REPUBLICA] (brief I.1). */
const val ANONYMOUS_RADIUS_METERS = 10_000.0

/**
 * True when [location] is within [ANONYMOUS_RADIUS_METERS] of [PRACA_REPUBLICA].
 * (0, 0) is the "no location" default of [Coordinates], so it is never inside.
 */
fun isInsideAnonymousArea(location: Coordinates): Boolean {
    if (location == Coordinates()) return false
    return haversineMeters(
        location.latitude, location.longitude,
        PRACA_REPUBLICA.latitude, PRACA_REPUBLICA.longitude,
    ) <= ANONYMOUS_RADIUS_METERS
}
