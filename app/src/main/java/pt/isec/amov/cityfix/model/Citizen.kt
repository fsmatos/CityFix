package pt.isec.amov.cityfix.model

/** Registered citizen ("Municipe" in the brief). */
data class Citizen(
    override val id: String = "",
    override val name: String = "",
    override val nif: String = "",
    override val email: String = "",
    override val phone: String = "",
    override val photo: String? = null,
    /** Home location given at registration; personal data, never shown to anonymous users; not editable. */
    val georeferencing: Coordinates = Coordinates(),
    val registrationStatus: RegistrationStatus = RegistrationStatus.PENDING,
    /** Whether the user enabled vertical-impact detection ("detecao de impacto"). */
    val impactDetection: Boolean = false,
) : User
