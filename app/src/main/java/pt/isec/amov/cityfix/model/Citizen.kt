package pt.isec.amov.cityfix.model

data class Citizen(
    override val id: String = "",
    override val name: String = "",
    override val nif: String = "",
    override val email: String = "",
    override val phone: String = "",
    override val photo: String? = null,
    val georeferencing: Coordinates = Coordinates(),
    val registrationStatus: RegistrationStatus = RegistrationStatus.PENDING,
    val impactDetection: Boolean = false,
) : User
