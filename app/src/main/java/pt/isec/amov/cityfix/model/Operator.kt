package pt.isec.amov.cityfix.model

/** Municipal operator ("Operador" in the brief): manages the ticket lifecycle. */
data class Operator(
    override val id: String = "",
    override val name: String = "",
    override val nif: String = "",
    override val email: String = "",
    override val phone: String = "",
    override val photo: String? = null,
) : User
