package pt.isec.amov.cityfix.model

/** Municipality ("Municipio" in the brief): operators use [emailDomain]; tickets lie within [radiusKm] of [center]. */
data class Municipality(
    /** Firestore document id. Empty until persisted; sample data must set distinct ids. */
    val id: String = "",
    val name: String = "",
    val emailDomain: String = "",
    val center: Coordinates = Coordinates(),
    val radiusKm: Double = 10.0,
)
