package pt.isec.amov.cityfix.model

/**
 * A point in WGS84 degrees, shared by Ticket, Citizen and Municipality.
 * The default (0, 0) is not a real location: sample data must set real values.
 */
data class Coordinates(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
)
