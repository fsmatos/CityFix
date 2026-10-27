package pt.isec.amov.cityfix.model

/** Municipality ("Municipio" in the brief). Operators register with an e-mail in [emailDomain]; anonymous users must be within [radiusKm] of [center] (Pr. Republica, 10 km). */
data class Municipality(
    /** Firestore document id. Empty until persisted; sample data must set distinct ids. */
    val id: String = "",
    val name: String = "",
    val emailDomain: String = "",
    val center: Coordinates = Coordinates(),
    val radiusKm: Double = 10.0,
)
