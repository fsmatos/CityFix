package pt.isec.amov.cityfix.model

data class Municipality(
    val id: String = "",
    val name: String = "",
    val emailDomain: String = "",
    val center: Coordinates = Coordinates(),
    val radiusKm: Double = 10.0,
)
